package no.uio.ifi.trackfind.backend.data.providers;

import alexh.weak.Dynamic;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.*;
import no.uio.ifi.trackfind.backend.events.DataReloadEvent;
import no.uio.ifi.trackfind.backend.operations.Operation;
import no.uio.ifi.trackfind.backend.repositories.*;
import no.uio.ifi.trackfind.backend.scripting.ScriptingEngine;
import no.uio.ifi.trackfind.backend.services.CacheService;
import no.uio.ifi.trackfind.backend.services.SearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

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
public abstract class AbstractDataProvider implements DataProvider {

    protected TrackFindProperties properties;
    protected ApplicationEventPublisher applicationEventPublisher;
    protected CacheService cacheService;
    protected SearchService searchService;
    protected JdbcTemplate jdbcTemplate;
    protected HubRepository hubRepository;
    protected SourceRepository sourceRepository;
    protected StandardRepository standardRepository;
    protected DatasetRepository datasetRepository;
    protected MappingRepository mappingRepository;
    protected ExecutorService executorService;
    protected Gson gson;
    protected Collection<ScriptingEngine> scriptingEngines;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName().replace("DataProvider", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Hub> getAllTrackHubs() {
        return Collections.singleton(new Hub(getName(), getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Hub> getActiveTrackHubs() {
        return hubRepository.findByRepository(getName());
    }

    /**
     * Fetches data from the repository.
     *
     * @throws Exception in case of some problems.
     */
    protected abstract void fetchData(String hubName) throws Exception;

    /**
     * {@inheritDoc}
     */
    @CacheEvict(cacheNames = {
            "metamodel-array-of-objects-attributes",
            "metamodel-flat",
            "metamodel-tree",
            "metamodel-attribute-types",
            "metamodel-attributes",
            "metamodel-subattributes",
            "metamodel-values"
    }, allEntries = true)
    @Transactional
    @Override
    public synchronized void crawlRemoteRepository(String hubName) {
        log.info("Fetching data using for {}: {}", getName(), hubName);
        try {
            fetchData(hubName);
            applicationEventPublisher.publishEvent(new DataReloadEvent(getName(), Operation.CRAWLING));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * Saves datasets to the database.
     *
     * @param hubName  Track Hub name.
     * @param datasets Datasets to save.
     */
    protected void save(String hubName, Collection<Map> datasets) {
        Hub hub = hubRepository.getOne(new HubId(getName(), hubName));
        Long maxRawVersion = jdbcTemplate.queryForObject("SELECT MAX(raw_version) FROM source WHERE repository = ? AND hub = ?", Long.class, hub.getRepository(), hub.getHub());
        String idAttribute = hub.getIdAttribute();
        Collection<Source> sourcesToSave = new ArrayList<>();
        for (Map dataset : datasets) {
            Source source = new Source();
            source.setRepository(getName());
            source.setHub(hubName);
            source.setContent(gson.toJson(dataset));
            source.setRawVersion(maxRawVersion == null ? 1L : maxRawVersion + 1);
            source.setCuratedVersion(0L);
            sourcesToSave.add(source);
            if (idAttribute != null) {
                Optional optionalId = Dynamic.from(dataset).get(idAttribute.replace("'", ""), properties.getLevelsSeparator()).asOptional();
                if (optionalId.isPresent()) {
                    String id = String.valueOf(optionalId.get());
                    Collection<Dataset> foundDatasets = searchService.search(hub,
                            String.format("fair_content%s%s ? '%s'", properties.getLevelsSeparator(), idAttribute, id), 1);
                    if (CollectionUtils.isNotEmpty(foundDatasets)) {
                        Dataset foundDataset = foundDatasets.iterator().next();
                        source.setId(foundDataset.getId());
                    }
                } else {
                    log.error("ID field not found for Hub {} in entry {}", hub, dataset);
                }
            }
        }
        sourceRepository.saveAll(sourcesToSave);
    }

    @Transactional
    protected synchronized void applyAutomaticMappings(Hub hub, Collection<Source> sources) {
        Collection<Standard> standardsToSave = new ArrayList<>();
        for (Source source : sources) {
            String idMappingAttribute = hub.getIdMappingAttribute();
            String versionMappingAttribute = hub.getVersionMappingAttribute();
            Standard standard = new Standard();
            standard.setId(source.getId());
            standard.setRawVersion(source.getRawVersion());
            standard.setCuratedVersion(source.getCuratedVersion());
            standard.setStandardVersion(1L);
            Map<String, Object> standardMap = new HashMap<>();
            standardMap.put(idMappingAttribute.replace("'", ""), standard.getId());
            standardMap.put(versionMappingAttribute.replace("'", ""), standard.getStandardVersion());
            standard.setContent(gson.toJson(standardMap));
            standardsToSave.add(standard);
        }
        standardRepository.saveAll(standardsToSave);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @CacheEvict(cacheNames = {
            "metamodel-array-of-objects-attributes",
            "metamodel-flat",
            "metamodel-tree",
            "metamodel-attribute-types",
            "metamodel-attributes",
            "metamodel-subattributes",
            "metamodel-values"
    }, allEntries = true)
    @Transactional
    @Override
    public synchronized void applyMappings(String hubName) {
        log.info("Applying mappings for {}: {}", getName(), hubName);
        Collection<Mapping> mappings = mappingRepository.findByRepositoryAndHub(getName(), hubName);
        Collection<Mapping> staticMappings = mappings.stream().filter(Mapping::isStaticMapping).collect(Collectors.toSet());
        Optional<Mapping> dynamicMappingOptional = mappings.stream().filter(m -> !m.isStaticMapping()).findAny();
        Collection<Source> sources = sourceRepository.findByRepositoryAndHubLatest(getName(), hubName);
        Collection<Standard> standards = new HashSet<>();
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
                Optional<Standard> standardLatest =
                        standardRepository.findByIdAndRawVersionAndCuratedVersionLatest(standard.getId(),
                                standard.getRawVersion(),
                                standard.getCuratedVersion());
                if (standardLatest.isPresent()) {
                    standard.setStandardVersion(standardLatest.get().getStandardVersion() + 1);
                } else {
                    standard.setStandardVersion(1L);
                }
                standards.add(standard);
            }
            standardRepository.saveAll(standards);
            applicationEventPublisher.publishEvent(new DataReloadEvent(hubName, Operation.MAPPING));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Autowired
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Autowired
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setHubRepository(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
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
