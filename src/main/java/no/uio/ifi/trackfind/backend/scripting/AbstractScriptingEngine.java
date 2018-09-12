package no.uio.ifi.trackfind.backend.scripting;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.springframework.beans.factory.annotation.Autowired;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Common methods holder for all Scripting Engines.
 *
 * @author Dmytro Titov
 */
public abstract class AbstractScriptingEngine implements ScriptingEngine {

    protected TrackFindProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> execute(String script, Map dataset) throws Exception {
        Object result = executeInternally(script, dataset);
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
     * @param script  Mappings script.
     * @param dataset Dataset to process.
     * @return Mapped values.
     * @throws ScriptException When script can't be interpreted/executed.
     */
    protected abstract Object executeInternally(String script, Map dataset) throws Exception;

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
