package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections4.MapUtils;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from Apache Lucene Document to TSV.
 *
 * @author Dmytro Titov
 */
@Component
public class DocumentToTSVConverter implements Function<Document, String> {

    private TrackFindProperties properties;
    private DocumentToMapConverter documentToMapConverter;

    /**
     * Convert dataset from Document to TSV.
     *
     * @param document Dataset as Document.
     * @return Tab Separated Values string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Document document) {
        StringBuilder result = new StringBuilder();
        Map map = documentToMapConverter.apply(document);
        Map<String, Object> basicMap = MapUtils.getMap(map, properties.getMetamodel().getBasicSectionName());
        basicMap = basicMap == null ? new HashMap<>() : basicMap;
        for (String basicAttribute : properties.getMetamodel().getBasicAttributes()) {
            result.append(String.valueOf(basicMap.get(basicAttribute))).append("\t");
        }
        result.append("\n");
        return result.toString();
    }

    @Autowired
    public void setDocumentToMapConverter(DocumentToMapConverter documentToMapConverter) {
        this.documentToMapConverter = documentToMapConverter;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }


}
