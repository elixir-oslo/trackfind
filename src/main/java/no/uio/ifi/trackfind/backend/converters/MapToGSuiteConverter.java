package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to GSuite.
 *
 * @author Dmytro Titov
 */
@Component
public class MapToGSuiteConverter implements Function<Map, String> {

    private TrackFindProperties properties;

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
        result.append("revision").append("\n");
        String revision = String.valueOf(map.keySet().iterator().next());
        Collection<Map<String, Object>> datasets = (Collection<Map<String, Object>>) map.get(revision);
        for (Map<String, Object> dataset : datasets) {
            Map<?, ?> basicMap = MapUtils.getMap(dataset, properties.getBasicSectionName());
            basicMap = basicMap == null ? new HashMap<>() : basicMap;
            for (String basicAttribute : properties.getBasicAttributes()) {
                Object value = basicMap.get(basicAttribute);
                result.append(value == null ? "." : String.valueOf(value)).append("\t");
            }
            result.append(revision).append("\n");
        }
        return result.toString();
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }


}
