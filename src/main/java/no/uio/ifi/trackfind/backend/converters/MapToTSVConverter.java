package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to TSV.
 *
 * @author Dmytro Titov
 */
@Component
public class MapToTSVConverter implements Function<Map, String> {

    private TrackFindProperties properties;

    /**
     * Convert dataset from Map to TSV.
     *
     * @param document Dataset as Map.
     * @return Tab Separated Values string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Map document) {
        StringBuilder result = new StringBuilder();
        Map<String, Object> basicMap = MapUtils.getMap(document, properties.getBasicSectionName());
        basicMap = basicMap == null ? new HashMap<>() : basicMap;
        for (String basicAttribute : properties.getBasicAttributes()) {
            result.append(String.valueOf(basicMap.get(basicAttribute))).append("\t");
        }
        return result.toString();
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }


}
