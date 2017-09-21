package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Converter from Map (most likely parsed from JSON) to Apache Lucene Document.
 *
 * @author Dmytro Titov
 */
@Component
public class MapToDocumentConverter implements Function<Map, Document> {

    private TrackFindProperties properties;

    /**
     * Convert dataset from Map to Document.
     *
     * @param map Dataset as a Map.
     * @return Dataset as a Document.
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public Document apply(Map map) {
        Document document = new Document();
        convertMapToDocument(document, map, properties.getMetamodel().getLevelsSeparator() + properties.getMetamodel().getAdvancedSectionName());
        document.add(new StringField(properties.getMetamodel().getIdAttribute(), UUID.randomUUID().toString(), Field.Store.YES));
        document.add(new StoredField(properties.getMetamodel().getRawDataAttribute(), new BytesRef(SerializationUtils.serialize(map))));
        return document;
    }

    /**
     * Recursive implementation of Map to Document conversion: field by field, taking care of nesting.
     *
     * @param document Result Document.
     * @param object   Either inner Map or value.
     * @param path     Path to the current entry (sequence of attributes).
     */
    private void convertMapToDocument(Document document, Object object, String path) {
        if (object instanceof Map) {
            Set keySet = ((Map) object).keySet();
            for (Object key : keySet) {
                Object value = ((Map) object).get(key);
                convertMapToDocument(document, value, path + properties.getMetamodel().getLevelsSeparator() + key);
            }
        } else if (object instanceof Collection) {
            Collection values = (Collection) object;
            for (Object value : values) {
                convertMapToDocument(document, value, path);
            }
        } else if (object != null) {
            String attribute = path.substring(1);
            String value = String.valueOf(object);
            if (StringUtils.isEmpty(value)) {
                return;
            }
            document.add(new StringField(attribute, value, Field.Store.YES));
        }
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
