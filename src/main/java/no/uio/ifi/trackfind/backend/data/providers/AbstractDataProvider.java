package no.uio.ifi.trackfind.backend.data.providers;

import alexh.weak.Dynamic;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import no.uio.ifi.trackfind.backend.dao.Mapping;
import no.uio.ifi.trackfind.backend.repositories.DatasetRepository;
import no.uio.ifi.trackfind.backend.repositories.MappingRepository;
import no.uio.ifi.trackfind.backend.scripting.ScriptingEngine;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Abstract class for all data providers.
 * Implements some common logic like Lucene Directory initialization, getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractDataProvider implements DataProvider, Comparable<DataProvider> {

    protected TrackFindProperties properties;
    protected JdbcTemplate jdbcTemplate;
    protected DatasetRepository datasetRepository;
    protected MappingRepository mappingRepository;
    protected ExecutorService executorService;
    protected Gson gson;
    protected Collection<ScriptingEngine> scriptingEngines;

    @PostConstruct
    protected void init() {
        if (datasetRepository.countByRepository(getName()) == 0) {
            crawlRemoteRepository();
            jdbcTemplate.execute("\n" +
                    "CREATE OR REPLACE VIEW metamodel AS\n" +
                    "  WITH RECURSIVE collect_metadata AS (\n" +
                    "    SELECT datasets.repository,\n" +
                    "           datasets.version,\n" +
                    "           first_level.key,\n" +
                    "           first_level.value,\n" +
                    "           json_typeof(first_level.value) AS type\n" +
                    "    FROM datasets,\n" +
                    "         json_each(datasets.raw_dataset) first_level\n" +
                    "\n" +
                    "    UNION ALL\n" +
                    "\n" +
                    "    (WITH prev_level AS (\n" +
                    "        SELECT *\n" +
                    "        FROM collect_metadata\n" +
                    "    )\n" +
                    "    SELECT prev_level.repository,\n" +
                    "           prev_level.version,\n" +
                    "           concat(prev_level.key, '>', current_level.key),\n" +
                    "           current_level.value,\n" +
                    "           json_typeof(current_level.value) AS type\n" +
                    "    FROM prev_level,\n" +
                    "         json_each(prev_level.value) AS current_level\n" +
                    "    WHERE prev_level.type = 'object'\n" +
                    "\n" +
                    "    UNION ALL\n" +
                    "\n" +
                    "    SELECT prev_level.repository,\n" +
                    "           prev_level.version,\n" +
                    "           concat(prev_level.key, '>', current_level.key),\n" +
                    "           current_level.value,\n" +
                    "           json_typeof(current_level.value) AS type\n" +
                    "    FROM prev_level,\n" +
                    "         json_array_elements(prev_level.value) AS entries,\n" +
                    "         json_each(entries) AS current_level\n" +
                    "    WHERE prev_level.type = 'array')\n" +
                    "  )\n" +
                    "  SELECT DISTINCT repository, version, key AS attribute, string_agg(DISTINCT collect_metadata.value :: text, ',') AS value , type\n" +
                    "  FROM collect_metadata\n" +
                    "  WHERE collect_metadata.type NOT IN ('object', 'array')\n" +
                    "  GROUP BY repository, version, key, type");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName().replace("DataProvider", "").toLowerCase();
    }

    /**
     * Gets attributes to skip during indexing.
     *
     * @return Collection of unneeded attributes.
     */
    protected Collection<String> getAttributesToSkip() {
        return Collections.emptySet();
    }

    /**
     * Gets attributes to hide in the tree.
     *
     * @return Collection of hidden attributes.
     */
    protected Collection<String> getAttributesToHide() {
        return Collections.emptySet();
    }

    /**
     * Fetches data from the repository.
     *
     * @throws Exception in case of some problems.
     */
    protected abstract void fetchData() throws Exception;

    /**
     * Postprocess Dataset.
     *
     * @param dataset Dataset to split.
     * @return Postprocessed dataset.
     */
    @SuppressWarnings("unchecked")
    protected Map postprocessDataset(Map dataset) {
        clearAttributesToSkip(dataset);
        return dataset;
    }

    /**
     * Removes attributes which should be skipped.
     *
     * @param dataset Dataset to clear.
     */
    private void clearAttributesToSkip(Map dataset) {
        for (String attributeToSkip : getAttributesToSkip()) {
            Dynamic value = Dynamic.from(dataset).get(attributeToSkip, properties.getLevelsSeparator());
            if (value.isList()) {
                value.asList().clear();
            } else if (value.isMap()) {
                value.asMap().clear();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void crawlRemoteRepository() {
        log.info("Fetching data using " + getName());
        try {
            fetchData();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * Saves datasets to the database.
     *
     * @param datasets Datasets to save.
     */
    protected void save(Collection<Map> datasets) {
        datasetRepository.saveAll(datasets.parallelStream().map(this::postprocessDataset).map(map -> {
            Dataset dataset = new Dataset();
            dataset.setRepository(getName());
            dataset.setVersion(getCurrentVersion());
            dataset.setRawDataset(gson.toJson(map));
            return dataset;
        }).collect(Collectors.toSet()));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public synchronized void applyMappings() {
        log.info("Applying mappings for " + getName());
        Collection<Mapping> mappings = mappingRepository.findByRepository(getName());
        Collection<Dataset> datasets = datasetRepository.findAllByVersion(getCurrentVersion());
        ScriptingEngine scriptingEngine = scriptingEngines.stream().filter(se -> properties.getScriptingLanguage().equals(se.getLanguage())).findAny().orElseThrow(RuntimeException::new);
        try {
            for (Dataset dataset : datasets) {
                Map<String, Object> rawMap = gson.fromJson(dataset.getRawDataset(), Map.class);
                Map<String, Object> basicMap = new HashMap<>();
                for (Mapping mapping : mappings) {
                    String script = mapping.getFrom();
                    Collection<String> values;
                    if (mapping.getStaticMapping()) {
                        values = Dynamic.from(rawMap).get(mapping.getFrom(), properties.getLevelsSeparator()).asList();
                    } else {
                        values = scriptingEngine.execute(script, rawMap);
                    }
                    if (CollectionUtils.size(values) == 1) {
                        basicMap.put(mapping.getTo(), values.iterator().next());
                    } else {
                        basicMap.put(mapping.getTo(), values);
                    }
                }
                dataset.setBasicDataset(gson.toJson(basicMap));
            }
            datasetRepository.saveAll(datasets);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetamodelTree() {
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multimap<String, String> getMetamodelFlat() {
        Multimap<String, String> metamodel = HashMultimap.create();
        return metamodel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Dataset> search(String query, int limit) {
        limit = limit == 0 ? Integer.MAX_VALUE : limit;
        List<Long> ids = jdbcTemplate.queryForList("SELECT id " +
                "FROM datasets " +
                "WHERE version = ? " +
                "AND (raw_dataset::jsonb @> ? OR basic_dataset::jsonb @> ?)" +
                "ORDER BY id ASC LIMIT ?", Long.TYPE, getCurrentVersion(), query, query, limit);
        return datasetRepository.findAllByIdIn(ids);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> fetch(String datasetId, String version) {
        long longVersion = version == null ? getCurrentVersion() : Long.parseLong(version);
        Dataset dataset = datasetRepository.findByIdAndVersion(Long.parseLong(datasetId), longVersion);
        return gson.fromJson(dataset.getRawDataset(), Map.class);
    }

    protected long getCurrentVersion() {
        Long version = jdbcTemplate.queryForObject("SELECT MAX(version) FROM datasets", Long.TYPE);
        return version == null ? 0 : version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(DataProvider that) {
        return this.getName().compareTo(that.getName());
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setDatasetRepository(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Autowired
    public void setMappingRepository(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    @Autowired
    public void setExecutorService(ExecutorService workStealingPool) {
        this.executorService = workStealingPool;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setScriptingEngines(Collection<ScriptingEngine> scriptingEngines) {
        this.scriptingEngines = scriptingEngines;
    }

}
