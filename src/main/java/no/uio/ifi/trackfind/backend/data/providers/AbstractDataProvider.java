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
import java.math.BigInteger;
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
        jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "source", "curated", properties.getLevelsSeparator(), properties.getLevelsSeparator()));
        jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "standard", "standard", properties.getLevelsSeparator(), properties.getLevelsSeparator()));
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
        Collection<Source> sources = sourceRepository.findByRepositoryLatest(getName());
        try {
            for (Source source : sources) {
                Map<String, Object> rawMap = gson.fromJson(source.getContent(), Map.class);
                Map<String, Object> standardMap = new HashMap<>();
                for (Mapping mapping : mappings) {
                    String script = mapping.getFrom();
                    Collection<String> values;
                    if (mapping.getStaticMapping()) {
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
                    } else {
                        ScriptingEngine scriptingEngine = scriptingEngines.stream().filter(se -> properties.getScriptingLanguage().equals(se.getLanguage())).findAny().orElseThrow(RuntimeException::new);
                        values = scriptingEngine.execute(script, rawMap);
                    }
                    if (CollectionUtils.size(values) == 1) {
                        standardMap.put(mapping.getTo(), values.iterator().next());
                    } else {
                        standardMap.put(mapping.getTo(), values);
                    }
                }
                Standard standard = new Standard();
                standard.setId(source.getId());
                standard.setContent(gson.toJson(standardMap));
                standard.setVersion(0L);                       // TODO: set proper version
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
    public Map<String, Object> getMetamodelTree(boolean advanced) {
        Map<String, Object> result = new HashMap<>();
        Multimap<String, String> metamodelFlat = getMetamodelFlat(advanced);
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
    public Multimap<String, String> getMetamodelFlat(boolean advanced) {
        Multimap<String, String> metamodel = HashMultimap.create();
        List<Map<String, Object>> attributeValuePairs = jdbcTemplate.queryForList(
                "SELECT attribute, value FROM " + (advanced ? "source" : "standard") + "_metamodel WHERE repository = ?",
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
            String rawQuery = String.format("SELECT id\n" +
                    "FROM datasets\n" +
                    "WHERE repository = '%s'\n" +
                    "AND (%s)\n" +
                    "GROUP BY id, raw_version, curated_version, standard_version\n" +
                    "HAVING raw_version = MAX(raw_version)\n" +
                    "   AND curated_version = MAX(curated_version)\n" +
                    "   AND standard_version = MAX(standard_version)\n" +
                    "ORDER BY id ASC LIMIT %s", getName(), query, limit);
            List<BigInteger> ids = jdbcTemplate.queryForList(queryValidator.validate(rawQuery), BigInteger.class);
            return datasetRepository.findByIdIn(ids);
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
    public Dataset fetch(String datasetId, String version) {
        return version == null ?
                datasetRepository.findByIdLatest(new BigInteger(datasetId)) :
                datasetRepository.findByIdAndVersion(new BigInteger(datasetId), version);
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
