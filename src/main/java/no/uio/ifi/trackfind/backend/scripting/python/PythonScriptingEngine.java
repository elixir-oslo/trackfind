package no.uio.ifi.trackfind.backend.scripting.python;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.python.core.PyCode;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                        public PyCode load(String script) {
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
    /*
        Example:
            import json
            source = json.loads(dataset)
            source['test'] = 'test'
            result = json.dumps(source)
     */
    public String execute(String script, String content) throws Exception {
        PythonInterpreter localInterpreter = new PythonInterpreter();
        localInterpreter.set(properties.getScriptingDatasetVariableName(), content);
        localInterpreter.eval(scripts.get(script));
        return String.valueOf(localInterpreter.get(properties.getScriptingResultVariableName()));
    }

    @Autowired
    public void setCommonInterpreter(PythonInterpreter commonInterpreter) {
        this.commonInterpreter = commonInterpreter;
    }

}
