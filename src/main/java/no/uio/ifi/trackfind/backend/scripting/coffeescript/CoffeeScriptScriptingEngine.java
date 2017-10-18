package no.uio.ifi.trackfind.backend.scripting.coffeescript;

import com.netopyr.coffee4java.CoffeeScriptEngine;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.HashMap;

// TODO: Implement scripts caching.
@Component
public class CoffeeScriptScriptingEngine extends AbstractScriptingEngine {

    private CoffeeScriptEngine coffeeScriptEngine;

    @Override
    public String getLanguage() {
        return "CoffeeScript";
    }

    @Override
    protected Object executeInternally(String script, Document document) throws ScriptException {
        CompiledScript compiledScript = coffeeScriptEngine.compile(script);
        return compiledScript.eval(new SimpleBindings(new HashMap<String, Object>() {{
            put(properties.getScriptingDatasetVariableName(), documentToJSONConverter.apply(document));
        }}));
    }

    @Autowired
    public void setCoffeeScriptEngine(CoffeeScriptEngine coffeeScriptEngine) {
        this.coffeeScriptEngine = coffeeScriptEngine;
    }

}
