package no.uio.ifi.trackfind.backend.scripting;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.converters.DocumentToMapConverter;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Common methods holder for all Scripting Engines.
 *
 * @author Dmytro Titov
 */
public abstract class AbstractScriptingEngine implements ScriptingEngine {

    protected TrackFindProperties properties;
    protected DocumentToMapConverter documentToMapConverter;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> execute(String script, Document document) throws Exception {
        Object result = executeInternally(script, document);
        Collection<String> values = new ArrayList<>();
        if (result instanceof Collection) { // multiple values
            for (Object value : (Collection) result) {
                values.add(String.valueOf(value));
            }
        } else { // single value
            values.add(String.valueOf(result));
        }
        values.remove("null");
        return values;
    }

    /**
     * Execute script.
     *
     * @param script   Mappings script.
     * @param document Document to process.
     * @return Mapped values.
     * @throws ScriptException When script can't be interpreted/executed.
     */
    protected abstract Object executeInternally(String script, Document document) throws Exception;

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setDocumentToMapConverter(DocumentToMapConverter documentToMapConverter) {
        this.documentToMapConverter = documentToMapConverter;
    }

}
