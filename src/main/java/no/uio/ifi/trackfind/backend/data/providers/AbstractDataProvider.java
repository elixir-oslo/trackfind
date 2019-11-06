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
import no.uio.ifi.trackfind.backend.services.impl.CacheService;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.backend.services.impl.SchemaService;
import no.uio.ifi.trackfind.backend.services.impl.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public abstract class AbstractDataProvider implements DataProvider {

    @Value("${trackfind.separator}")
    protected String separator;

    @Value("${trackfind.scripting.language}")
    protected String scriptingLanguage;

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
        return Collections.singleton(new TfHub(getName(), getName(), getFetchURI(null)));
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
            "metamodel-values",
            "metamodel-references",
            "metamodel-categories-by-name",
            "search",
            "gsuite"
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
        TfVersion version = createVersion(hubName, Operation.CRAWLING, false);

        Set<String> standardObjectTypeNames = new HashSet<>(schemaService.getAttributes().keySet());
        Collection<TfObject> objectsToSave = new ArrayList<>();
        for (String objectTypeName : objects.keySet()) {
            standardObjectTypeNames.remove(objectTypeName);
            TfObjectType objectType = createObjectType(version, objectTypeName);
            for (String obj : objects.get(objectTypeName)) {
                TfObject tfObject = new TfObject();
                tfObject.setObjectType(objectType);
                tfObject.setContent(obj);
                objectsToSave.add(tfObject);
            }
        }
        // create standard object-types, if not present yet
        standardObjectTypeNames.forEach(sotn -> objectTypeRepository.save(createObjectType(version, sotn)));
        objectRepository.saveAll(objectsToSave);
    }

    protected TfObjectType createObjectType(TfVersion version, String objectTypeName) {
        TfObjectType objectType = new TfObjectType();
        objectType.setName(objectTypeName);
        objectType.setVersion(version);
        objectType = objectTypeRepository.save(objectType);
        return objectType;
    }

    protected TfVersion createVersion(String hubName, Operation operation, boolean copyReferences) {
        log.info("Creating new version. Hub name: {}, operation: {}, copy references: {}", hubName, operation, copyReferences);
        TfHub hub = hubRepository.findByRepositoryAndName(getName(), hubName);
        long totalVersions = CollectionUtils.size(hub.getVersions());
        Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
        currentVersionOptional.ifPresent(cv -> {
            log.info("Current version: {}", cv);
            cv.setCurrent(false);
            versionRepository.saveAndFlush(cv);
        });

        TfVersion newVersion = new TfVersion();
        newVersion.setVersion(totalVersions + 1);
        newVersion.setCurrent(true);
        newVersion.setOperation(operation);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (!authentication.isAuthenticated()) {
                throw new AuthenticationServiceException("Unauthorized attempt to create new version!");
            }
            newVersion.setUser((TfUser) authentication.getPrincipal());
        }
        newVersion.setTime(new Date());
        newVersion.setHub(hub);
        if (Operation.CURATION.equals(operation) && currentVersionOptional.isPresent()) {
            newVersion.setBasedOn(currentVersionOptional.get());
        }

        newVersion = versionRepository.saveAndFlush(newVersion);
        log.info("New version: {}", newVersion);

        if (copyReferences && currentVersionOptional.isPresent()) {
            metamodelService.copyReferencesFromOneVersionToAnotherVersion(currentVersionOptional.get(), newVersion);
        }

        return newVersion;
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
            "metamodel-values",
            "metamodel-references",
            "metamodel-categories-by-name",
            "search",
            "gsuite"
    }, allEntries = true)
    @Override
    public synchronized void runCuration(String hubName) {
        log.info("Curating {} - {}...", getName(), hubName);
        try {
            Collection<TfMapping> mappings = metamodelService.getMappings(getName(), hubName);
            HashMultimap<TfObjectType, TfMapping> mappingsByCategories = HashMultimap.create();
            mappings.forEach(m -> mappingsByCategories.put(m.getToObjectType(), m));
            Collection<SearchResult> allEntries = searchService.search(getName(), hubName, Boolean.TRUE.toString(), Collections.emptySet(), 0).getValue();
            for (TfMapping mapping : mappings) {
                if (mapping.getFromObjectType() != null) {
                    runStaticMappings(allEntries, mapping);
                } else {
                    Optional<ScriptingEngine> scriptingEngineOptional = getScriptingEngine();
                    if (!scriptingEngineOptional.isPresent()) {
                        log.warn("Scripting engine for {} language is not registered. Skipping mapping: {}", scriptingLanguage, mapping);
                        continue;
                    }
                    ScriptingEngine scriptingEngine = scriptingEngineOptional.get();
                    runDynamicMappings(allEntries, mapping, scriptingEngine);
                }
            }
            TfVersion newVersion = createVersion(hubName, Operation.CURATION, false);
            storeMappedObjects(allEntries, newVersion);
            applicationEventPublisher.publishEvent(new DataReloadEvent(hubName, Operation.CURATION));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    protected void storeMappedObjects(Collection<SearchResult> allEntries, TfVersion newVersion) {
        List<TfObjectType> objectTypes = new ArrayList<>();
        List<TfObject> objectsToSave = allEntries.stream().map(entry -> {
            List<TfObject> result = new ArrayList<>();
            for (String objectTypeName : entry.getContent().keySet()) {
                TfObjectType objectType = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase(objectTypeName)).findAny().orElse(null);
                if (objectType == null) {
                    objectType = createObjectType(newVersion, objectTypeName);
                    objectTypes.add(objectType);
                }
                TfObject tfObject = new TfObject();
                tfObject.setObjectType(objectType);
                tfObject.setContent(gson.toJson(entry.getContent().get(objectTypeName)));
                result.add(tfObject);
            }
            return result;
        }).flatMap(List::stream).collect(Collectors.toList());
        objectRepository.saveAll(objectsToSave);
    }

    @SuppressWarnings("unchecked")
    protected void runStaticMappings(Collection<SearchResult> allEntries, TfMapping mapping) {
        for (SearchResult entry : allEntries) {
            String fromObjectTypeName = mapping.getFromObjectType().getName();
            String toObjectTypeName = mapping.getToObjectType().getName();
            Collection<String> values;
            Dynamic dynamicValues = Dynamic
                    .from(entry.getContent())
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
            putValueByPath(entry.getContent().computeIfAbsent(toObjectTypeName, k -> new HashMap<String, Object>()), path, values);
        }
    }

    @SuppressWarnings("unchecked")
    protected void runDynamicMappings(Collection<SearchResult> allEntries, TfMapping mapping, ScriptingEngine scriptingEngine) throws Exception {
        for (SearchResult entry : allEntries) {
            String jsonContent = gson.toJson(entry.getContent());
            String scriptResults = scriptingEngine.execute(mapping.getScript(), jsonContent);
            entry.setContent(gson.fromJson(scriptResults, Map.class));
        }
    }

    protected Optional<ScriptingEngine> getScriptingEngine() {
        return scriptingEngines.stream().filter(se -> StringUtils.equals(scriptingLanguage, se.getLanguage())).findAny();
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
