package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to <a href="http://gtrack.no/">GSuite</a>.
 *
 * @author Dmytro Titov
 */
@Component
public class MapToGSuiteConverter implements Function<Map, String> {

    private TrackFindProperties properties;

    /**
     * Convert dataset from Map to GSuite.
     *
     * @param document Dataset as Map.
     * @return GSuite string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Map document) {
        StringBuilder result = new StringBuilder();
        Map<String, Object> basicMap = MapUtils.getMap(document, properties.getBasicSectionName());
        basicMap = basicMap == null ? new HashMap<>() : basicMap;
        for (String basicAttribute : properties.getBasicAttributes()) {
            Object value = basicMap.get(basicAttribute);
            result.append(value == null ? "." : String.valueOf(value)).append("\t");
        }
        return result.toString();
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }


}
