package no.uio.ifi.trackfind.backend.scripting.groovy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;
import java.util.HashMap;

/**
 * Groovy implementation of the Scripting Engine.
 *
 * @author Dmytro Titov
 */
@Component
public class GroovyScriptingEngine extends AbstractScriptingEngine {

    private GroovyShell groovyShell;

    private LoadingCache<String, Script> scripts = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                    new CacheLoader<String, Script>() {
                        public Script load(String script) throws ScriptException {
                            return groovyShell.parse(script);
                        }
                    });

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguage() {
        return "Groovy";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object executeInternally(String script, Document document) throws Exception {
        Script parsedScript = scripts.get(script);
        parsedScript.setBinding(new Binding(new HashMap<String, Object>() {{
            put(properties.getScriptingDatasetVariableName(), documentToJSONConverter.apply(document));
        }}));
        return parsedScript.evaluate(script);
    }

    @Autowired
    public void setGroovyShell(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

}
