package no.uio.ifi.trackfind.backend.converters;

import com.google.gson.Gson;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Converter from Apache Lucene Document to JSON.
 *
 * @author Dmytro Titov
 */
// TODO: Add unit-test.
@Component
public class DocumentToJSONConverter implements Function<Document, String> {

    private DocumentToMapConverter documentToMapConverter;
    private Gson gson;

    /**
     * Convert dataset from Document to JSON.
     *
     * @param document Dataset as Document.
     * @return JSON string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Document document) {
        return gson.toJson(documentToMapConverter.apply(document));
    }

    @Autowired
    public void setDocumentToMapConverter(DocumentToMapConverter documentToMapConverter) {
        this.documentToMapConverter = documentToMapConverter;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
