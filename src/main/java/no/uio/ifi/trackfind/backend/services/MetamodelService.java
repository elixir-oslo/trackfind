package no.uio.ifi.trackfind.backend.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Hub;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for getting metamodel information: attibutes, subattributes, values, etc.
 */
// TODO: cover with tests
@Service
public class MetamodelService {

    private JdbcTemplate jdbcTemplate;
    private TrackFindProperties properties;

    @Cacheable("metamodel-array-of-objects-attributes")
    public Collection<String> getArrayOfObjectsAttributes(Hub hub, boolean raw) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute FROM " + (raw ? "source" : "standard") + "_array_of_objects WHERE repository = ? AND hub = ?",
                String.class,
                hub.getRepository(),
                hub.getHub());
    }

    @Cacheable("metamodel-flat")
    public Multimap<String, String> getMetamodelFlat(Hub hub, boolean raw) {
        Multimap<String, String> metamodel = HashMultimap.create();
        List<Map<String, Object>> attributeValuePairs = jdbcTemplate.queryForList(
                "SELECT attribute, value FROM " + (raw ? "source" : "standard") + "_metamodel WHERE repository = ? AND hub = ?",
                hub.getRepository(),
                hub.getHub());
        for (Map attributeValuePair : attributeValuePairs) {
            String attribute = String.valueOf(attributeValuePair.get("attribute"));
            String value = String.valueOf(attributeValuePair.get("value"));
            metamodel.put(attribute, value);
        }
        return metamodel;
    }

    @SuppressWarnings("unchecked")
    @Cacheable("metamodel-tree")
    public Map<String, Object> getMetamodelTree(Hub hub, boolean raw) {
        Map<String, Object> result = new HashMap<>();
        Multimap<String, String> metamodelFlat = getMetamodelFlat(hub, raw);
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

    @Cacheable("metamodel-attribute-types")
    public Map<String, String> getAttributeTypes(Hub hub, boolean raw) {
        Map<String, String> metamodel = new HashMap<>();
        List<Map<String, Object>> attributeTypePairs = jdbcTemplate.queryForList(
                "SELECT DISTINCT attribute, type FROM " + (raw ? "source" : "standard") + "_metamodel WHERE repository = ? AND hub = ?",
                hub.getRepository(),
                hub.getHub());
        for (Map attributeTypePair : attributeTypePairs) {
            String attribute = String.valueOf(attributeTypePair.get("attribute"));
            String type = String.valueOf(attributeTypePair.get("type"));
            metamodel.put(attribute, type);
        }
        return metamodel;
    }

    @Cacheable("metamodel-attributes")
    public Collection<String> getAttributes(Hub hub, String filter, boolean raw, boolean top) {
        Set<String> attributes = top ? getMetamodelTree(hub, raw).keySet() : getMetamodelFlat(hub, raw).asMap().keySet();
        return attributes.parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Cacheable("metamodel-subattributes")
    public Collection<String> getSubAttributes(Hub hub, String attribute, String filter, boolean raw) {
        Set<String> attributes = getMetamodelFlat(hub, raw).asMap().keySet();
        Set<String> filteredAttributes = attributes.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
        String separator = properties.getLevelsSeparator();
        if (filteredAttributes.contains(attribute)) {
            return Collections.emptySet();
        }
        return filteredAttributes
                .parallelStream()
                .filter(a -> a.startsWith(attribute))
                .map(a -> a.replace(attribute, ""))
                .map(a -> (a.contains(separator) ? a.substring(separator.length()) : a))
                .map(a -> (a.contains(separator) ? a.substring(0, a.indexOf(separator)) : a))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    @Cacheable("metamodel-values")
    public Collection<String> getValues(Hub hub, String attribute, String filter, boolean raw) {
        return getMetamodelFlat(hub, raw).get(attribute).parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
