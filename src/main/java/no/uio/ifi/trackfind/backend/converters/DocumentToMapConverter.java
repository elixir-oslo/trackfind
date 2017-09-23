package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converter from Apache Lucene Document to Map.
 *
 * @author Dmytro Titov
 */
@Component
public class DocumentToMapConverter implements Function<Document, Map> {

    private TrackFindProperties properties;

    /**
     * Convert dataset from Document to Map.
     *
     * @param document Dataset as Document.
     * @return Dataset as a Map.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map apply(Document document) {
        Map<String, Object> result = new HashMap<>();
        Collection<String> fieldNames = document.getFields().parallelStream().map(IndexableField::name).collect(Collectors.toSet());
        for (String fieldName : fieldNames) {
            Map<String, Object> metamodel = result;
            String[] path = fieldName.split(properties.getLevelsSeparator());
            for (int i = 0; i < path.length - 1; i++) {
                String attribute = path[i];
                metamodel = (Map<String, Object>) metamodel.computeIfAbsent(attribute, k -> new HashMap<String, Object>());
            }
            String valuesKey = path[path.length - 1];
            Collection<String> values = (Collection<String>) metamodel.computeIfAbsent(valuesKey, k -> new HashSet<>());
            values.addAll(Arrays.asList(document.getValues(fieldName)));
            values.add(document.get(fieldName));
            values.remove(null);
            if (CollectionUtils.isEmpty(values)) {
                metamodel.remove(valuesKey);
            }
            if (CollectionUtils.size(values) == 1) {
                metamodel.put(valuesKey, values.iterator().next());
            }
        }
        return result;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
