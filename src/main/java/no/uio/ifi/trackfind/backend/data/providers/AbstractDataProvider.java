package no.uio.ifi.trackfind.backend.data.providers;

import alexh.weak.Dynamic;
import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.events.DataReloadEvent;
import no.uio.ifi.trackfind.backend.operations.Operation;
import no.uio.ifi.trackfind.backend.pojo.*;
import no.uio.ifi.trackfind.backend.repositories.*;
import no.uio.ifi.trackfind.backend.scripting.ScriptingEngine;
import no.uio.ifi.trackfind.backend.services.CacheService;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.backend.services.SchemaService;
import no.uio.ifi.trackfind.backend.services.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract class for all data providers.
 * Implements some common logic like getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Transactional
public abstract class AbstractDataProvider implements DataProvider {

    @Value("${trackfind.separator}")
    protected String separator;

    protected ApplicationEventPublisher applicationEventPublisher;
    protected MetamodelService metamodelService;
    protected SchemaService schemaService;
    protected CacheService cacheService;
    protected SearchService searchService;
    protected JdbcTemplate jdbcTemplate;
    protected HubRepository hubRepository;
    protected ObjectTypeRepository objectTypeRepository;
    protected VersionRepository versionRepository;
    protected ObjectRepository objectRepository;
    protected ScriptRepository scriptRepository;
    protected ReferenceRepository referenceRepository;
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
    public Collection<TfHub> getAllTrackHubs() {
        return Collections.singleton(new TfHub(getName(), getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TfHub> getActiveTrackHubs() {
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
            "metamodel-categories",
            "metamodel-attributes",
            "metamodel-attributes-flat",
            "metamodel-attribute-types",
            "metamodel-values"
    }, allEntries = true)
    @Override
    public synchronized void crawlRemoteRepository(String hubName) {
        log.info("Fetching data for {}: {}", getName(), hubName);
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
     * @param hubName Hub name.
     * @param objects Object-type to object map.
     */
    protected void save(String hubName, Map<String, Collection<String>> objects) {
        TfHub hub = hubRepository.findByRepositoryAndName(getName(), hubName);
        Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
        Optional<TfVersion> lastVersionOptional = hub.getLastVersion();
        AtomicLong versionNumber = new AtomicLong(0);
        lastVersionOptional.ifPresent(lv -> versionNumber.set(lv.getVersion()));
        currentVersionOptional.ifPresent(cv -> {
            cv.setCurrent(false);
            versionRepository.saveAndFlush(cv);
        });
        TfVersion version = new TfVersion();
        version.setVersion(versionNumber.incrementAndGet());
        version.setCurrent(true);
        version.setOperation(Operation.CRAWLING);
        version.setUsername("admin");
        version.setTime(new Date());
        version.setHub(hub);
        version = versionRepository.save(version);
        Set<String> standardObjectTypeNames = new HashSet<>(schemaService.getAttributes().keySet());
        Collection<TfObject> objectsToSave = new ArrayList<>();
        for (String objectTypeName : objects.keySet()) {
            standardObjectTypeNames.remove(objectTypeName);
            TfObjectType objectType = new TfObjectType();
            objectType.setName(objectTypeName);
            objectType.setVersion(version);
            objectType = objectTypeRepository.save(objectType);
            for (String obj : objects.get(objectTypeName)) {
                TfObject tfObject = new TfObject();
                tfObject.setObjectType(objectType);
                tfObject.setContent(obj);
                objectsToSave.add(tfObject);
            }
        }
        // create standard object-types, if not present yet
        for (String objectTypeName : standardObjectTypeNames) {
            TfObjectType objectType = new TfObjectType();
            objectType.setName(objectTypeName);
            objectType.setVersion(version);
            objectTypeRepository.save(objectType);
        }
        objectRepository.saveAll(objectsToSave);
    }

    /**
     * {@inheritDoc}
     */
    @CacheEvict(cacheNames = {
            "metamodel-array-of-objects-attributes",
            "metamodel-flat",
            "metamodel-tree",
            "metamodel-categories",
            "metamodel-attributes",
            "metamodel-attributes-flat",
            "metamodel-attribute-types",
            "metamodel-values"
    }, allEntries = true)
//    @HystrixCommand(commandProperties = {@HystrixProperty(name = "execution.timeout.enabled", value = "false")})
    @Override
    public synchronized void applyMappings(String hubName) {
        log.info("Applying mappings for {}: {}", getName(), hubName);
        try {
            Collection<TfMapping> mappings = metamodelService.getMappings(getName(), hubName);
            HashMultimap<TfObjectType, TfMapping> mappingsByCategories = HashMultimap.create();
            mappings.forEach(m -> mappingsByCategories.put(m.getToObjectType(), m));
            Collection<SearchResult> allEntries = searchService.search(getName(), hubName, Boolean.TRUE.toString(), Collections.emptySet(), 0);
            for (SearchResult entry : allEntries) {
                Collection<TfObject> objectsToSave = new ArrayList<>();
                Map<String, Map> rawMap = entry.getContent();
                for (Map.Entry<TfObjectType, Collection<TfMapping>> categoryToMappings : mappingsByCategories.asMap().entrySet()) {
                    Map<String, Object> standardMap = new HashMap<>();
                    TfObjectType toObjectType = categoryToMappings.getKey();
                    for (TfMapping mapping : categoryToMappings.getValue()) {
                        String fromObjectTypeName = mapping.getFromObjectType().getName();
                        Collection<String> values;
                        Dynamic dynamicValues = Dynamic
                                .from(rawMap)
                                .get(fromObjectTypeName + separator + mapping.getFromAttribute().replace("'", ""), separator);
                        if (dynamicValues.isPresent()) {
                            if (dynamicValues.isList()) {
                                values = dynamicValues.asList();
                            } else {
                                values = Collections.singletonList(dynamicValues.asString());
                            }
                        } else {
                            values = Collections.emptyList();
                        }
                        String[] path = mapping.getToAttribute().replace("'", "").split(separator);
                        putValueByPath(standardMap, path, values);
                    }
                    TfObject standardObject = new TfObject();
                    standardObject.setContent(gson.toJson(standardMap));
                    standardObject.setObjectType(toObjectType);
                    objectsToSave.add(standardObject);
                }
                objectRepository.saveAll(objectsToSave);
            }
//            for (TfMapping mapping : mappings) {
//                TfReference reference = new TfReference();
//                reference.setFromObjectType(mapping.getFromObjectType());
//                reference.setFromAttribute(mapping.getFromAttribute());
//                reference.setToObjectType(mapping.getToObjectType());
//                reference.setToAttribute(mapping.getToAttribute());
//                referenceRepository.save(reference);
//            }
            applicationEventPublisher.publishEvent(new DataReloadEvent(hubName, Operation.MAPPING));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    @SuppressWarnings("unchecked")
    private void putValueByPath(Map<String, Object> standardMap, String[] path, Collection<String> values) {
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

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
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
    public void setObjectTypeRepository(ObjectTypeRepository objectTypeRepository) {
        this.objectTypeRepository = objectTypeRepository;
    }

    @Autowired
    public void setVersionRepository(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Autowired
    public void setObjectRepository(ObjectRepository objectRepository) {
        this.objectRepository = objectRepository;
    }

    @Autowired
    public void setScriptRepository(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    @Autowired
    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
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
