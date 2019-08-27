package no.uio.ifi.trackfind.backend.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.events.DataReloadEvent;
import no.uio.ifi.trackfind.backend.operations.Operation;
import no.uio.ifi.trackfind.backend.pojo.*;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import no.uio.ifi.trackfind.backend.repositories.MappingsRepository;
import no.uio.ifi.trackfind.backend.repositories.ReferenceRepository;
import no.uio.ifi.trackfind.backend.repositories.VersionRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    protected JdbcTemplate jdbcTemplate;
    protected HubRepository hubRepository;
    protected VersionRepository versionRepository;
    protected ReferenceRepository referenceRepository;
    protected MappingsRepository mappingsRepository;
    protected ApplicationEventPublisher applicationEventPublisher;

    @Cacheable(value = "metamodel-flat", sync = true)
    public Map<String, Multimap<String, String>> getMetamodelFlat(String repository, String hub) {
        Collection<TfObjectType> objectTypes = getObjectTypes(repository, hub);
        Map<Long, String> objectTypesMap = objectTypes.stream().collect(Collectors.toMap(TfObjectType::getId, TfObjectType::getName));
        String objectTypeIds = objectTypes.stream().map(ot -> ot.getId().toString()).collect(Collectors.joining(","));
        return jdbcTemplate.query(String.format("SELECT object_type_id, attribute, value FROM tf_metamodel WHERE object_type_id IN (%s)", objectTypeIds),
                resultSet -> {
                    Map<String, Multimap<String, String>> result = new HashMap<>();
                    while (resultSet.next()) {
                        long objectTypeId = resultSet.getLong("object_type_id");
                        String objectTypeName = objectTypesMap.get(objectTypeId);
                        String attribute = resultSet.getString("attribute");
                        String value = resultSet.getString("value");
                        result.computeIfAbsent(objectTypeName, k -> HashMultimap.create()).put(attribute, value);
                    }
                    return result;
                }
        );
    }

    @SuppressWarnings("unchecked")
    @Cacheable(value = "metamodel-tree", sync = true)
    public Map<String, Map<String, Object>> getMetamodelTree(String repository, String hub) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Collection<TfObjectType> objectTypes = getObjectTypes(repository, hub);
        Map<String, Multimap<String, String>> fullMetamodelFlat = getMetamodelFlat(repository, hub);
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
        TfVersion currentVersion = hubEntity.getCurrentVersion().orElseThrow(RuntimeException::new);
        return currentVersion.getObjectTypes();
    }

    @Cacheable(value = "metamodel-array-of-objects-attributes", sync = true)
    public Collection<String> getArrayOfObjectsAttributes(String repository, String hub, String category) {
        TfObjectType objectType = getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute FROM tf_array_of_objects WHERE object_type_id = ?",
                String.class,
                objectType.getId());
    }

    @Cacheable(value = "metamodel-attribute-types", sync = true)
    public Map<String, String> getAttributeTypes(String repository, String hub, String category) {
        TfObjectType objectType = getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
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
        TfObjectType objectType = getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
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
        return getAttributesFlat(repository, hub, category, path).stream()
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
    public Collection<String> getValues(String repository, String hub, String category, String path) {
        Map<String, Multimap<String, String>> metamodelFlat = getMetamodelFlat(repository, hub);
        Multimap<String, String> metamodel = metamodelFlat.get(category);
        return metamodel.get(path).parallelStream().collect(Collectors.toSet());
    }

    public Collection<TfScript> getScripts(String repository, String hub) {
        TfHub currentHub = hubRepository.findByRepositoryAndName(repository, hub);
        return currentHub.getCurrentVersion().orElseThrow(RuntimeException::new).getScripts();
    }

    public Collection<TfReference> getReferences(String repository, String hub) {
        TfHub currentHub = hubRepository.findByRepositoryAndName(repository, hub);
        TfVersion currentVersion = currentHub.getCurrentVersion().orElseThrow(RuntimeException::new);
        Collection<TfObjectType> objectTypes = currentVersion.getObjectTypes();
        Collection<TfReference> references = new HashSet<>();
        for (TfObjectType objectType : objectTypes) {
            references.addAll(objectType.getReferences());
        }
        return references;
    }

    public void addReference(TfReference reference) {
        referenceRepository.save(reference);
    }

    public void deleteReference(TfReference reference) {
        referenceRepository.delete(reference);
    }

    public void copyReferencesFromAnotherVersion(String repository, String hub, TfVersion sourceVersion) {
        Collection<TfObjectType> currentObjectTypes = getObjectTypes(repository, hub);
        Collection<String> currentObjectTypeNames = currentObjectTypes.stream().map(TfObjectType::getName).collect(Collectors.toSet());
        Collection<TfObjectType> sourceObjectTypes = sourceVersion.getObjectTypes();
        for (TfObjectType objectType : sourceObjectTypes) {
            for (TfReference reference : objectType.getReferences()) {
                if (currentObjectTypeNames.contains(reference.getFromObjectType().getName()) && currentObjectTypeNames.contains(reference.getToObjectType().getName())) {
                    TfObjectType newFromObjectType = currentObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(reference.getFromObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                    TfObjectType newToObjectType = currentObjectTypes.stream().filter(cot -> cot.getName().equalsIgnoreCase(reference.getToObjectType().getName())).findAny().orElseThrow(RuntimeException::new);
                    TfReference newReference = new TfReference(null, newFromObjectType, reference.getFromAttribute(), newToObjectType, reference.getToAttribute());
                    referenceRepository.save(newReference);
                }
            }
        }
    }

    public Collection<TfMapping> getMappings(String repository, String hub) {
        TfHub currentHub = hubRepository.findByRepositoryAndName(repository, hub);
        TfVersion currentVersion = currentHub.getCurrentVersion().orElseThrow(RuntimeException::new);
        Collection<TfObjectType> objectTypes = currentVersion.getObjectTypes();
        Collection<TfMapping> mappings = new HashSet<>();
        for (TfObjectType objectType : objectTypes) {
            mappings.addAll(objectType.getMappings());
        }
        return mappings;
    }

    public void addMapping(TfMapping mapping) {
        mappingsRepository.save(mapping);
    }

    public void deleteMapping(TfMapping mapping) {
        mappingsRepository.delete(mapping);
    }

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
    public void setMappingsRepository(MappingsRepository mappingsRepository) {
        this.mappingsRepository = mappingsRepository;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

}
