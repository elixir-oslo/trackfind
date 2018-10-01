package no.uio.ifi.trackfind.backend.converters;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to GSuite.
 *
 * @author Dmytro Titov
 */
@Component
public class DatasetsToGSuiteConverter implements Function<Collection<Dataset>, String> {

    private TrackFindProperties properties;
    private Gson gson;

    /**
     * Convert datasets from to GSuite.
     *
     * @param datasets Datasets to convert.
     * @return GSuite string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Collection<Dataset> datasets) {
        StringBuilder result = new StringBuilder("###");
        properties.getStandardAttributes().forEach(a -> result.append(a).append("\t"));
        result.append(properties.getIdAttribute()).append("\t").append(properties.getVersionAttribute()).append("\n");
        String version = datasets.iterator().next().getVersion();
        for (Dataset dataset : datasets) {
            Collection<Map<String, Object>> standardMaps = getStandardMaps(dataset);
            for (Map<String, Object> standardMap : standardMaps) {
                for (String standardAttribute : properties.getStandardAttributes()) {
                    String value = MapUtils.getString(standardMap, standardAttribute, ".");
                    result.append(value).append("\t");
                }
                result.append(dataset.getId()).append("\t").append(version).append("\n");
            }
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Collection<Map<String, Object>> getStandardMaps(Dataset dataset) {
        Map standardMap = gson.fromJson(dataset.getStandardContent(), Map.class);
        if (standardMap == null) {
            return Collections.emptySet();
        }
        Object dataTypesObject = standardMap.get(properties.getDataTypeAttribute());
        Collection<String> dataTypes;
        if (dataTypesObject instanceof Collection) {
            dataTypes = (Collection<String>) dataTypesObject;
        } else {
            dataTypes = Collections.singleton(dataTypesObject == null ? "." : dataTypesObject.toString());
        }
        Object urisObject = standardMap.get(properties.getUriAttribute());
        Collection<String> uris;
        if (urisObject instanceof Collection) {
            uris = (Collection<String>) urisObject;
        } else {
            uris = Collections.singleton(urisObject == null ? "." : urisObject.toString());
        }
        Assert.isTrue(CollectionUtils.size(dataTypes) == CollectionUtils.size(uris), "DataTypes and URIs mismatch!");
        Collection<Map<String, Object>> result = new HashSet<>();
        Iterator<String> iterator = uris.iterator();
        for (String dataType : dataTypes) {
            String uri = iterator.next();
            Map<String, Object> clone = clone(standardMap);
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
