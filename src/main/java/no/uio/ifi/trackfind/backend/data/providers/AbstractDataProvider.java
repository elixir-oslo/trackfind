package no.uio.ifi.trackfind.backend.data.providers;

import alexh.weak.Dynamic;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.*;
import no.uio.ifi.trackfind.backend.repositories.DatasetRepository;
import no.uio.ifi.trackfind.backend.repositories.MappingRepository;
import no.uio.ifi.trackfind.backend.repositories.SourceRepository;
import no.uio.ifi.trackfind.backend.repositories.StandardRepository;
import no.uio.ifi.trackfind.backend.scripting.ScriptingEngine;
import no.uio.ifi.trackfind.backend.services.QueryValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Abstract class for all data providers.
 * Implements some common logic like getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractDataProvider implements DataProvider, Comparable<DataProvider> {

    protected TrackFindProperties properties;
    protected JdbcTemplate jdbcTemplate;
    protected SourceRepository sourceRepository;
    protected StandardRepository standardRepository;
    protected DatasetRepository datasetRepository;
    protected MappingRepository mappingRepository;
    protected QueryValidator queryValidator;
    protected ExecutorService executorService;
    protected Gson gson;
    protected Collection<ScriptingEngine> scriptingEngines;

    @PostConstruct
    protected void init() {
        if (datasetRepository.countByRepository(getName()) == 0) {
            crawlRemoteRepository();
        }
        try {
            jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "source", "curated", properties.getLevelsSeparator(), properties.getLevelsSeparator()));
            jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "standard", "standard", properties.getLevelsSeparator(), properties.getLevelsSeparator()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName().replace("DataProvider", "");
    }

    /**
     * Fetches data from the repository.
     *
     * @throws Exception in case of some problems.
     */
    protected abstract void fetchData() throws Exception;

    /**
     * {@inheritDoc}
     */
    @CacheEvict(cacheNames = {"metamodel-tree", "metamodel-flat"}, allEntries = true)
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
        sourceRepository.saveAll(datasets.parallelStream().map(map -> {
            Source source = new Source();
            source.setRepository(getName());
            source.setContent(gson.toJson(map));
            source.setRawVersion(0L);           // TODO: set proper version
            source.setCuratedVersion(0L);       // TODO: set proper version
            return source;
        }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @CacheEvict(cacheNames = {"metamodel-tree", "metamodel-flat"}, allEntries = true)
    @Transactional
    @Override
    public synchronized void applyMappings() {
        log.info("Applying mappings for " + getName());
        Collection<Mapping> mappings = mappingRepository.findByRepository(getName());
        Collection<Mapping> staticMappings = mappings.stream().filter(Mapping::isStaticMapping).collect(Collectors.toSet());
        Optional<Mapping> dynamicMappingOptional = mappings.stream().filter(m -> !m.isStaticMapping()).findAny();
        Collection<Source> sources = sourceRepository.findByRepositoryLatest(getName());
        ScriptingEngine scriptingEngine = scriptingEngines.stream().filter(se -> properties.getScriptingLanguage().equals(se.getLanguage())).findAny().orElseThrow(RuntimeException::new);
        try {
            for (Source source : sources) {
                Map<String, Object> rawMap = gson.fromJson(source.getContent(), Map.class);
                Map<String, Object> standardMap = new HashMap<>();
                if (dynamicMappingOptional.isPresent()) {
                    Mapping mapping = dynamicMappingOptional.get();
                    standardMap = gson.fromJson(scriptingEngine.execute(mapping.getFrom(), source.getContent()), Map.class);
                }
                for (Mapping mapping : staticMappings) {
                    Collection<String> values;
                    Dynamic dynamicValues = Dynamic.from(rawMap).get(mapping.getFrom(), properties.getLevelsSeparator());
                    if (dynamicValues.isPresent()) {
                        if (dynamicValues.isList()) {
                            values = dynamicValues.asList();
                        } else {
                            values = Collections.singletonList(dynamicValues.asString());
                        }
                    } else {
                        values = Collections.emptyList();
                    }
                    String[] path = mapping.getTo().split(properties.getLevelsSeparator());
                    Map<String, Object> nestedMap = standardMap;
                    for (int i = 0; i < path.length - 1; i++) {
                        nestedMap = (Map<String, Object>) nestedMap.computeIfAbsent(path[i], k -> new HashMap<String, Object>());
                    }
                    if (CollectionUtils.size(values) == 1) {
                        nestedMap.put(path[path.length - 1], values.iterator().next());
                    } else {
                        nestedMap.put(path[path.length - 1], values);
                    }
                }
                Standard standard = new Standard();
                standard.setId(source.getId());
                standard.setContent(gson.toJson(standardMap));
                standard.setRawVersion(source.getRawVersion());
                standard.setCuratedVersion(source.getCuratedVersion());
                standard.setStandardVersion(0L);
                standardRepository.findByIdLatest(standard.getId()).ifPresent(s -> standard.setStandardVersion(s.getStandardVersion() + 1));
                standardRepository.save(standard);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * {@inheritDoc}
     */
    @Cacheable(value = "metamodel-tree", key = "#root.targetClass + #root.methodName + #root.args[0]")
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetamodelTree(boolean raw) {
        Map<String, Object> result = new HashMap<>();
        Multimap<String, String> metamodelFlat = getMetamodelFlat(raw);
        for (Map.Entry<String, Collection<String>> entry : metamodelFlat.asMap().entrySet()) {
            String attribute = entry.getKey();
            Map<String, Object> metamodel = result;
            String[] path = attribute.split(properties.getLevelsSeparator());
            for (int i = 0; i < path.length - 1; i++) {
                String part = path[i];
                metamodel = (Map<String, Object>) metamodel.computeIfAbsent(part, k -> new HashMap<String, Object>());
            }
            String valuesKey = path[path.length - 1];
            metamodel.put(valuesKey, entry.getValue());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Cacheable(value = "metamodel-flat", key = "#root.targetClass + #root.methodName + #root.args[0]")
    @Override
    public Multimap<String, String> getMetamodelFlat(boolean raw) {
        Multimap<String, String> metamodel = HashMultimap.create();
        List<Map<String, Object>> attributeValuePairs = jdbcTemplate.queryForList(
                "SELECT attribute, value FROM " + (raw ? "source" : "standard") + "_metamodel WHERE repository = ?",
                getName());
        for (Map attributeValuePair : attributeValuePairs) {
            String attribute = String.valueOf(attributeValuePair.get("attribute"));
            String value = String.valueOf(attributeValuePair.get("value"));
            metamodel.put(attribute, value);
        }
        return metamodel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Dataset> search(String query, int limit) {
        try {
            limit = limit == 0 ? Integer.MAX_VALUE : limit;
            String rawQuery = String.format("SELECT *\n" +
                    "FROM latest_datasets\n" +
                    "WHERE repository = '%s'\n" +
                    "  AND (%s)\n" +
                    "ORDER BY id ASC\n" +
                    "LIMIT %s", getName(), query, limit);
            return jdbcTemplate.query(queryValidator.validate(rawQuery), (resultSet, i) -> {
                Dataset dataset = new Dataset();
                dataset.setId(resultSet.getLong("id"));
                dataset.setRepository(resultSet.getString("repository"));
                dataset.setCuratedContent(resultSet.getString("curated_content"));
                dataset.setStandardContent(resultSet.getString("standard_content"));
                dataset.setFairContent(resultSet.getString("fair_content"));
                dataset.setVersion(resultSet.getString("version"));
                return dataset;
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Dataset fetch(Long datasetId, String version) {
        return version == null ? datasetRepository.findByIdLatest(datasetId) : datasetRepository.findByIdAndVersion(datasetId, version);
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
    public void setSourceRepository(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Autowired
    public void setStandardRepository(StandardRepository standardRepository) {
        this.standardRepository = standardRepository;
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
    public void setQueryValidator(QueryValidator queryValidator) {
        this.queryValidator = queryValidator;
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
