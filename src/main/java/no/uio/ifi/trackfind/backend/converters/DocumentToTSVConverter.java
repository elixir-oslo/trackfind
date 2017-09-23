package no.uio.ifi.trackfind.backend.converters;

import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Converter from Apache Lucene Document to TSV.
 *
 * @author Dmytro Titov
 */
@Component
public class DocumentToTSVConverter implements Function<Document, String> {

    private DocumentToMapConverter documentToMapConverter;
    private MapToTSVConverter mapToTSVConverter;

    /**
     * Convert dataset from Document to TSV.
     *
     * @param document Dataset as Document.
     * @return Tab Separated Values string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Document document) {
        return mapToTSVConverter.apply(documentToMapConverter.apply(document));
    }

    @Autowired
    public void setDocumentToMapConverter(DocumentToMapConverter documentToMapConverter) {
        this.documentToMapConverter = documentToMapConverter;
    }

    @Autowired
    public void setMapToTSVConverter(MapToTSVConverter mapToTSVConverter) {
        this.mapToTSVConverter = mapToTSVConverter;
    }

}
