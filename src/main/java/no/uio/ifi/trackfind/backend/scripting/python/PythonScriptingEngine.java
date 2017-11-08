package no.uio.ifi.trackfind.backend.scripting.python;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.apache.lucene.document.Document;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;

/**
 * Python implementation of the Scripting Engine.
 *
 * @author Dmytro Titov
 */
@Component
public class PythonScriptingEngine extends AbstractScriptingEngine {

    private PythonInterpreter commonInterpreter;

    private LoadingCache<String, PyCode> scripts = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                    new CacheLoader<String, PyCode>() {
                        public PyCode load(String script) throws ScriptException {
                            return commonInterpreter.compile(script);
                        }
                    });

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguage() {
        return "Python";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object executeInternally(String script, Document document) throws Exception {
        PythonInterpreter localInterpreter = new PythonInterpreter();
        localInterpreter.set(properties.getScriptingDatasetVariableName(), documentToJSONConverter.apply(document));
        return localInterpreter.eval(scripts.get(script));
    }

    @Autowired
    public void setCommonInterpreter(PythonInterpreter commonInterpreter) {
        this.commonInterpreter = commonInterpreter;
    }

}
