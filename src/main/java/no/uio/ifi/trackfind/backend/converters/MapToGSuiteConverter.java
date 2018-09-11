package no.uio.ifi.trackfind.backend.converters;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to GSuite.
 *
 * @author Dmytro Titov
 */
@Component
public class MapToGSuiteConverter implements Function<Map, String> {

    private TrackFindProperties properties;
    private Gson gson;

    /**
     * Convert revisioned datasets from Map to GSuite.
     *
     * @param map Datasets as Map.
     * @return GSuite string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Map map) {
        StringBuilder result = new StringBuilder("###");
        properties.getBasicAttributes().forEach(a -> result.append(a).append("\t"));
        result.append(properties.getRevisionAttribute()).append("\n");
        String revision = String.valueOf(map.keySet().iterator().next());
        Collection<Map<String, Object>> datasets = (Collection<Map<String, Object>>) map.get(revision);
        for (Map<String, Object> dataset : datasets) {
            Collection<Map<String, Object>> basicMaps = getBasicMaps(dataset);
            for (Map<String, Object> basicMap : basicMaps) {
                for (String basicAttribute : properties.getBasicAttributes()) {
                    String value = MapUtils.getString(basicMap, basicAttribute, ".");
                    result.append(value).append("\t");
                }
                result.append(revision).append("\n");
            }
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Collection<Map<String, Object>> getBasicMaps(Map<String, Object> dataset) {
        Map<?, ?> basicMap = MapUtils.getMap(dataset, properties.getBasicSectionName(), new HashMap<>());
        Object dataTypesObject = basicMap.get(properties.getDataTypeAttribute());
        Collection<String> dataTypes;
        if (dataTypesObject instanceof Collection) {
            dataTypes = (Collection<String>) dataTypesObject;
        } else {
            dataTypes = Collections.singleton(String.valueOf(dataTypesObject));
        }
        Object urisObject = basicMap.get(properties.getUriAttribute());
        Collection<String> uris;
        if (urisObject instanceof Collection) {
            uris = (Collection<String>) urisObject;
        } else {
            uris = Collections.singleton(String.valueOf(urisObject));
        }

        Asserts.check(CollectionUtils.size(dataTypes) == CollectionUtils.size(uris), "DataTypes and URIs mismatch!");
        Collection<Map<String, Object>> result = new HashSet<>();
        Iterator<String> iterator = uris.iterator();
        for (String dataType : dataTypes) {
            String uri = iterator.next();
            Map<String, Object> clone = clone(basicMap);
            clone.put(properties.getDataTypeAttribute(), dataType);
            clone.put(properties.getUriAttribute(), uri);
            result.add(clone);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> clone(Map<?, ?> map) {
        return gson.fromJson(gson.toJson(map), Map.class);
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
