package no.uio.ifi.trackfind.backend.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfMapping;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import no.uio.ifi.trackfind.backend.repositories.MappingRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for getting metamodel information: categories, attributes, values, etc.
 */
// TODO: cover with tests
@Service
public class MetamodelService {

    private TrackFindProperties properties;
    private JdbcTemplate jdbcTemplate;
    private HubRepository hubRepository;
    private MappingRepository mappingRepository;

    @Cacheable("metamodel-flat")
    @Transactional
    public Map<String, Multimap<String, String>> getMetamodelFlat(String repository, String hub) {
        Collection<TfObjectType> objectTypes = getObjectTypes(repository, hub);
        Map<String, Multimap<String, String>> metamodel = new HashMap<>();
        for (TfObjectType objectType : objectTypes) {
            Multimap<String, String> attributeValues = HashMultimap.create();
            List<Map<String, Object>> attributeValuePairs = jdbcTemplate.queryForList(
                    "SELECT attribute, value FROM tf_metamodel WHERE object_type_id = ?",
                    objectType.getId());
            for (Map attributeValuePair : attributeValuePairs) {
                String attribute = String.valueOf(attributeValuePair.get("attribute"));
                String value = String.valueOf(attributeValuePair.get("value"));
                attributeValues.put(attribute, value);
            }
            metamodel.put(objectType.getName(), attributeValues);
        }
        return metamodel;
    }

    @SuppressWarnings("unchecked")
    @Cacheable("metamodel-tree")
    @Transactional
    public Map<String, Map<String, Object>> getMetamodelTree(String repository, String hub) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Collection<TfObjectType> objectTypes = getObjectTypes(repository, hub);
        for (TfObjectType objectType : objectTypes) {
            Map<String, Object> fullMetamodel = new HashMap<>();
            Multimap<String, String> metamodelFlat = getMetamodelFlat(repository, hub).get(objectType.getName());
            for (Map.Entry<String, Collection<String>> entry : metamodelFlat.asMap().entrySet()) {
                String attribute = entry.getKey();
                Map<String, Object> metamodel = fullMetamodel;
                String[] path = attribute.split(properties.getLevelsSeparator());
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

    @Cacheable("metamodel-categories")
    @Transactional
    public Collection<TfObjectType> getObjectTypes(String repository, String hub) {
        TfHub hubs = hubRepository.findByRepositoryAndName(repository, hub);
        TfVersion maxVersion = hubs.getMaxVersion().orElseThrow(RuntimeException::new);
        return maxVersion.getObjectTypes();
    }

    @Cacheable("metamodel-array-of-objects-attributes")
    @Transactional
    public Collection<String> getArrayOfObjectsAttributes(String repository, String hub, String category) {
        TfObjectType objectType = getObjectTypes(repository, hub).stream().filter(c -> c.getName().equals(category)).findAny().orElseThrow(RuntimeException::new);
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute FROM tf_array_of_objects WHERE object_type_id = ?",
                String.class,
                objectType.getId());
    }

    @Cacheable("metamodel-attribute-types")
    @Transactional
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

    @Cacheable("metamodel-attributes")
    @Transactional
    public Collection<String> getAttributes(String repository, String hub, String category, String path) {
        if (StringUtils.isEmpty(path)) {
            return getMetamodelTree(repository, hub).get(category).keySet();
        }
        Map<String, Multimap<String, String>> metamodelFlat = getMetamodelFlat(repository, hub);
        Set<String> attributes = metamodelFlat.get(category).asMap().keySet();
        String separator = properties.getLevelsSeparator();
        String clearPath = path.replace(category + separator, "");
        if (attributes.contains(clearPath)) {
            return Collections.emptySet();
        }
        return attributes
                .parallelStream()
                .filter(a -> a.startsWith(clearPath))
                .map(a -> a.replace(clearPath, ""))
                .map(a -> (a.contains(separator) ? a.substring(separator.length()) : a))
                .map(a -> (a.contains(separator) ? a.substring(0, a.indexOf(separator)) : a))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    @Cacheable("metamodel-values")
    @Transactional
    public Collection<String> getValues(String repository, String hub, String category, String path) {
        String separator = properties.getLevelsSeparator();
        path = path.replace(category + separator, "");
        Map<String, Multimap<String, String>> metamodelFlat = getMetamodelFlat(repository, hub);
        Multimap<String, String> metamodel = metamodelFlat.get(category);
        return metamodel.get(path).parallelStream().collect(Collectors.toSet());
    }

    @Cacheable("metamodel-mappings")
    @Transactional
    public Collection<TfMapping> getMappings(String repository, String hub) {
        TfHub currentHub = hubRepository.findByRepositoryAndName(repository, hub);
        return mappingRepository.findByVersionId(currentHub.getMaxVersion().orElseThrow(RuntimeException::new).getId());
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
    public void setHubRepository(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @Autowired
    public void setMappingRepository(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

}
