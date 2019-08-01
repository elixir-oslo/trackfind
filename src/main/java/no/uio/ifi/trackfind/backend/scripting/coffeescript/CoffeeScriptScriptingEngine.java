package no.uio.ifi.trackfind.backend.scripting.coffeescript;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netopyr.coffee4java.CoffeeScriptEngine;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.HashMap;

/**
 * CoffeeScript implementation of the Scripting Engine.
 *
 * @author Dmytro Titov
 */
@Component
public class CoffeeScriptScriptingEngine extends AbstractScriptingEngine {

    private CoffeeScriptEngine coffeeScriptEngine;

    private LoadingCache<String, CompiledScript> scripts = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                    new CacheLoader<String, CompiledScript>() {
                        public CompiledScript load(String script) throws ScriptException {
                            return coffeeScriptEngine.compile(script);
                        }
                    });

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguage() {
        return "CoffeeScript";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    /*
        Example:
            source = JSON.parse(dataset)
            source['test'] = 'test'
            return JSON.stringify(source)
     */
    public String execute(String script, String content) throws Exception {
        CompiledScript compiledScript = scripts.get(script);
        Object result = compiledScript.eval(new SimpleBindings(new HashMap<String, Object>() {{
            put(datasetVariable, content);
        }}));
        return String.valueOf(result);
    }

    @Autowired
    public void setCoffeeScriptEngine(CoffeeScriptEngine coffeeScriptEngine) {
        this.coffeeScriptEngine = coffeeScriptEngine;
    }

}
