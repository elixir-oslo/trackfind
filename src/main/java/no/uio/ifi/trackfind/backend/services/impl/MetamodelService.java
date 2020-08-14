package no.uio.ifi.trackfind.backend.services.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.events.DataReloadEvent;
import no.uio.ifi.trackfind.backend.operations.Operation;
import no.uio.ifi.trackfind.backend.pojo.*;
import no.uio.ifi.trackfind.backend.repositories.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for getting metamodel information: categories, attributes, values, etc.
 */
@Slf4j
@Service
@Transactional
public class MetamodelService {

    @Value("${trackfind.separator}")
    protected String separator;

    protected MetamodelService metamodelService;
    protected JdbcTemplate jdbcTemplate;
    protected HubRepository hubRepository;
    protected VersionRepository versionRepository;
    protected ReferenceRepository referenceRepository;
    protected ObjectTypeRepository objectTypeRepository;
    protected MappingsRepository mappingsRepository;
    protected ApplicationEventPublisher applicationEventPublisher;

    @Cacheable(value = "metamodel-flat", sync = true)
    public Map<String, Multimap<String, String>> getMetamodelFlat(String repository, String hub, String valuesFilter, Set<Long> ids) {
        String fromClause = "tf_metamodel";
        if (CollectionUtils.isNotEmpty(ids)) {
            fromClause = "(" + buildFilteredMetamodelQuery(ids) + ") AS foo";
        }
        Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(repository, hub);
        Map<Long, String> objectTypesMap = objectTypes.stream().collect(Collectors.toMap(TfObjectType::getId, TfObjectType::getName));
        String objectTypeIds = objectTypes.stream().map(ot -> ot.getId().toString()).collect(Collectors.joining(","));
        return jdbcTemplate.query(String.format("SELECT object_type_id, attribute, value FROM %s WHERE object_type_id IN (%s)", fromClause, objectTypeIds),
                resultSet -> {
                    Map<String, Multimap<String, String>> result = new HashMap<>();
                    while (resultSet.next()) {
                        String value = resultSet.getString("value");
                        if (valuesFilter != null && !value.toLowerCase().contains(valuesFilter.toLowerCase())) {
                            continue;
                        }
                        long objectTypeId = resultSet.getLong("object_type_id");
                        String objectTypeName = objectTypesMap.get(objectTypeId);
                        String attribute = resultSet.getString("attribute");
                        result.computeIfAbsent(objectTypeName, k -> HashMultimap.create()).put(attribute, value);
                    }
                    return result;
                }
        );
    }

    protected String buildFilteredMetamodelQuery(Set<Long> ids) {
        String schemaSQL;
        try {
            schemaSQL = IOUtils.resourceToString("/schema.sql", Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String metamodelQuery = StringUtils.substringBetween(schemaSQL,
                "CREATE MATERIALIZED VIEW IF NOT EXISTS tf_metamodel AS",
                "WITH DATA;");
        return metamodelQuery.replace("jsonb_each(tf_current_objects.content) first_level",
                String.format("jsonb_each(tf_current_objects.content) first_level\nWHERE tf_current_objects.id IN (%s)\n", org.springframework.util.StringUtils.collectionToCommaDelimitedString(ids)));
    }

    @SuppressWarnings("unchecked")
    @Cacheable(value = "metamodel-tree", sync = true)
    public Map<String, Map<String, Object>> getMetamodelTree(String repository, String hub, String valuesFilter) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(repository, hub);
        Map<String, Multimap<String, String>> fullMetamodelFlat = metamodelService.getMetamodelFlat(repository, hub, valuesFilter, null);
        for (TfObjectType objectType : objectTypes) {
            if (!fullMetamodelFlat.containsKey(objectType.getName())) {
                continue;
            }
            Map<String, Object> fullMetamodel = new HashMap<>();
            Multimap<String, String> metamodelFlat = fullMetamodelFlat.get(objectType.getName());
            for (Map.Entry<String, Collection<String>> entry : metamodelFlat.asMap().entrySet()) {
                String attribute = entry.getKey();
                Map<String, Object> metamodel = fullMetamodel;
                String[] path = attribute.split(separator);
                for (int i = 0; i < path.length - 1; i++) {
                    String part = path[i];
                    metamodel = (Map<String, Object>) metamodel.computeIfAbsent(part, k -> new HashMap<String, Object>());
                }
                String valuesKey = path[path.length - 1];
                metamodel.put(valuesKey, entry.getValue());
            }
            result.put(objectType.getName(), fullMetamodel);
        }
        return result;
    }

    @Cacheable(value = "metamodel-categories", sync = true)
    public Collection<TfObjectType> getObjectTypes(String repository, String hub) {
        TfHub hubEntity = hubRepository.findByRepositoryAndName(repository, hub);
        Optional<TfVersion> currentVersionOptional = hubEntity.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return Collections.emptyList();
        }
        TfVersion currentVersion = currentVersionOptional.get();
        return currentVersion.getObjectTypes();
    }

    @Cacheable(value = "metamodel-categories-by-name", sync = true)
    public Optional<TfObjectType> findObjectTypeByName(String repository, String hub, String objectTypeName) {
        TfHub hubEntity = hubRepository.findByRepositoryAndName(repository, hub);
        Optional<TfVersion> currentVersionOptional = hubEntity.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return Optional.empty();
        }
        TfVersion currentVersion = currentVersionOptional.get();
        return objectTypeRepository.findByVersionAndName(currentVersion, objectTypeName);
    }

    @Cacheable(value = "metamodel-array-of-objects-attributes", sync = true)
    public Collection<String> getArrayOfObjectsAttributes(String repository, String hub, String category) {
        TfObjectType objectType = getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute FROM tf_array_of_objects WHERE object_type_id = ?",
                String.class,
                objectType.getId());
    }

    @SuppressWarnings("rawtypes")
    @Cacheable(value = "metamodel-attribute-types", sync = true)
    public Map<String, String> getAttributeTypes(String repository, String hub, String category) {
        TfObjectType objectType = metamodelService.getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
        Map<String, String> metamodel = new HashMap<>();
        List<Map<String, Object>> attributeTypePairs = jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute, type FROM tf_metamodel WHERE object_type_id = ?",
                objectType.getId());
        for (Map attributeTypePair : attributeTypePairs) {
            String attribute = String.valueOf(attributeTypePair.get("attribute"));
            String type = String.valueOf(attributeTypePair.get("type"));
            metamodel.put(attribute, type);
        }
        return metamodel;
    }

    @Cacheable(value = "metamodel-attributes-flat", sync = true)
    public Collection<String> getAttributesFlat(String repository, String hub, String category, String path) {
        TfObjectType objectType = metamodelService.getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
        if (StringUtils.isEmpty(path)) {
            return jdbcTemplate.queryForList(
                    "SELECT attribute FROM tf_attributes WHERE object_type_id = ?",
                    String.class,
                    objectType.getId());
        } else {
            return jdbcTemplate.queryForList(
                    "SELECT REPLACE(attribute, ?, '') FROM tf_attributes WHERE object_type_id = ? AND lower(attribute) LIKE ?",
                    String.class,
                    path.toLowerCase() + "->", objectType.getId(), path.toLowerCase() + "->%");
        }
    }

    @Cacheable(value = "metamodel-attributes", sync = true)
    public Collection<String> getAttributes(String repository, String hub, String category, String path) {
        return metamodelService.getAttributesFlat(repository, hub, category, path).stream()
                .map(a -> {
                    if (a.contains(separator)) {
                        return a.substring(0, a.indexOf(separator));
                    } else {
                        return a;
                    }
                })
                .collect(Collectors.toSet());
    }

    @Cacheable(value = "metamodel-values", sync = true)
    public Collection<String> getValues(String repository, String hub, String category, String path, String valuesFilter, Set<Long> ids) {
        Map<String, Multimap<String, String>> metamodelFlat = metamodelService.getMetamodelFlat(repository, hub, valuesFilter, ids);
        Multimap<String, String> metamodel = metamodelFlat.get(category);
        return metamodel.get(path).parallelStream().collect(Collectors.toSet());
    }

    @Cacheable(value = "metamodel-references", sync = true)
    public Collection<TfReference> getReferences(String repository, String hub) {
        TfHub currentHub = hubRepository.findByRepositoryAndName(repository, hub);
        Optional<TfVersion> currentVersionOptional = currentHub.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return Collections.emptyList();
        }
        TfVersion currentVersion = currentVersionOptional.get();
        Collection<TfObjectType> objectTypes = currentVersion.getObjectTypes();
        Collection<TfReference> references = new HashSet<>();
        for (TfObjectType objectType : objectTypes) {
            references.addAll(objectType.getReferences());
        }
        return references;
    }

    @CacheEvict(cacheNames = {
            "metamodel-references",
            "search",
            "gsuite"
    }, allEntries = true)
    public TfReference addReference(TfReference reference) {
        return referenceRepository.save(reference);
    }

    @CacheEvict(cacheNames = {
            "metamodel-references", "search",
            "gsuite"
    }, allEntries = true)
    public void deleteReference(TfReference reference) {
        referenceRepository.delete(reference);
    }

    @CacheEvict(cacheNames = {
            "metamodel-references", "search",
            "gsuite"
    }, allEntries = true)
    public void copyReferencesFromAnotherVersionToCurrentVersion(String repository, String hubName, TfVersion sourceVersion) {
        TfHub hub = hubRepository.findByRepositoryAndName(repository, hubName);
        Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return;
        }
        TfVersion currentVersion = currentVersionOptional.get();
        copyReferencesFromOneVersionToAnotherVersion(sourceVersion, currentVersion);
    }

    @CacheEvict(cacheNames = {
            "metamodel-references",
            "search",
            "gsuite"
    }, allEntries = true)
    public void copyReferencesFromOneVersionToAnotherVersion(TfVersion sourceVersion, TfVersion targetVersion) {
        log.info("Copying references from {} to {}", sourceVersion, targetVersion);
        Collection<TfObjectType> targetObjectTypes = targetVersion.getObjectTypes();
        Collection<String> targetObjectTypeNames = targetObjectTypes.stream().map(TfObjectType::getName).collect(Collectors.toSet());
        Collection<TfObjectType> sourceObjectTypes = sourceVersion.getObjectTypes();
        for (TfObjectType objectType : sourceObjectTypes) {
            for (TfReference reference : objectType.getReferences()) {
                if (targetObjectTypeNames.contains(reference.getFromObjectType().getName()) && targetObjectTypeNames.contains(reference.getToObjectType().getName())) {
                    TfObjectType newFromObjectType = targetObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(reference.getFromObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                    TfObjectType newToObjectType = targetObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(reference.getToObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                    TfReference newReference = new TfReference(null, newFromObjectType, reference.getFromAttribute(), newToObjectType, reference.getToAttribute());
                    addReference(newReference);
                }
            }
        }
    }

    public void copyMappingsFromAnotherVersionToCurrentVersion(String repository, String hubName, TfVersion sourceVersion) {
        TfHub hub = hubRepository.findByRepositoryAndName(repository, hubName);
        Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return;
        }
        TfVersion currentVersion = currentVersionOptional.get();
        copyMappingsFromOneVersionToAnotherVersion(sourceVersion, currentVersion);
    }

    public void copyMappingsFromOneVersionToAnotherVersion(TfVersion sourceVersion, TfVersion targetVersion) {
        log.info("Copying mappings from {} to {}", sourceVersion, targetVersion);
        Collection<TfObjectType> targetObjectTypes = targetVersion.getObjectTypes();
        Collection<String> targetObjectTypeNames = targetObjectTypes.stream().map(TfObjectType::getName).collect(Collectors.toSet());
        for (TfMapping mapping : sourceVersion.getMappings()) {
            if (mapping.getFromObjectType() == null) {
                addMapping(new TfMapping(null, null, targetVersion, null, null, null, null, mapping.getScript()));
                continue;
            }
            if (targetObjectTypeNames.contains(mapping.getFromObjectType().getName()) && targetObjectTypeNames.contains(mapping.getToObjectType().getName())) {
                TfObjectType newFromObjectType = targetObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(mapping.getFromObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                TfObjectType newToObjectType = targetObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(mapping.getToObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                TfMapping newMapping = new TfMapping(null, null, targetVersion, newFromObjectType, mapping.getFromAttribute(), newToObjectType, mapping.getToAttribute(), null);
                addMapping(newMapping);
            }
        }
    }

    public Collection<TfMapping> getMappings(String repository, String hubName) {
        TfHub hub = hubRepository.findByRepositoryAndName(repository, hubName);
        Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
        if (currentVersionOptional.isEmpty()) {
            return Collections.emptyList();
        }
        TfVersion currentVersion = currentVersionOptional.get();
        return currentVersion.getMappings();
    }

    public TfMapping addMapping(TfMapping mapping) {
        TfVersion version = mapping.getVersion();
        if (mapping.getOrderNumber() == null) {
            Collection<TfMapping> mappings = version.getMappings();
            TfMapping lastMapping = mappings.stream().max(Comparator.comparing(TfMapping::getOrderNumber)).orElse(new TfMapping(null, 0L, null, null, null, null, null, null));
            mapping.setOrderNumber(lastMapping.getOrderNumber() + 1L);
        }
        TfMapping savedMapping = mappingsRepository.save(mapping);
        version.getMappings().add(savedMapping);
        versionRepository.save(version);
        return savedMapping;
    }

    public void moveMapping(TfMapping mapping, boolean up) {
        TfVersion version = mapping.getVersion();
        TfMapping neighbour;
        if (up) {
            Optional<TfMapping> optionalNeighbour = mappingsRepository.findByVersionAndOrderNumber(version, mapping.getOrderNumber() - 1);
            if (optionalNeighbour.isEmpty()) {
                return;
            }
            neighbour = optionalNeighbour.get();
            neighbour.setOrderNumber(neighbour.getOrderNumber() + 1);
            mapping.setOrderNumber(mapping.getOrderNumber() - 1);
        } else {
            Optional<TfMapping> optionalNeighbour = mappingsRepository.findByVersionAndOrderNumber(version, mapping.getOrderNumber() + 1);
            if (optionalNeighbour.isEmpty()) {
                return;
            }
            neighbour = optionalNeighbour.get();
            neighbour.setOrderNumber(neighbour.getOrderNumber() - 1);
            mapping.setOrderNumber(mapping.getOrderNumber() + 1);
        }
        mappingsRepository.save(neighbour);
        mappingsRepository.save(mapping);
    }

    public void deleteMapping(TfMapping mapping) {
        TfHub hub = mapping.getVersion().getHub();
        Collection<TfMapping> mappings = getMappings(hub.getRepository(), hub.getName());
        List<TfMapping> mappingsAfter = mappings.stream().filter(m -> m.getOrderNumber() > mapping.getOrderNumber()).peek(tfMapping -> tfMapping.setOrderNumber(tfMapping.getOrderNumber() - 1)).collect(Collectors.toList());
        mappingsRepository.delete(mapping);
        mappingsRepository.saveAll(mappingsAfter);
    }

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
    public void activateVersion(TfVersion version) {
        TfHub hub = version.getHub();
        Optional<TfVersion> currentVersion = hub.getCurrentVersion();
        currentVersion.ifPresent(cv -> {
            cv.setCurrent(false);
            versionRepository.saveAndFlush(cv);
        });
        version.setCurrent(true);
        versionRepository.save(version);
        applicationEventPublisher.publishEvent(new DataReloadEvent(hub.getName(), Operation.VERSION_CHANGE));
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
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
    public void setVersionRepository(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Autowired
    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    @Autowired
    public void setObjectTypeRepository(ObjectTypeRepository objectTypeRepository) {
        this.objectTypeRepository = objectTypeRepository;
    }

    @Autowired
    public void setMappingsRepository(MappingsRepository mappingsRepository) {
        this.mappingsRepository = mappingsRepository;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

}
