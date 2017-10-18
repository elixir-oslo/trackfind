package no.uio.ifi.trackfind.backend.scripting.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;
import java.util.HashMap;

// TODO: Implement scripts caching.
@Component
public class GroovyScriptingEngine extends AbstractScriptingEngine {

    private GroovyShell groovyShell;

    @Override
    public String getLanguage() {
        return "Groovy";
    }

    @Override
    protected Object executeInternally(String script, Document document) throws ScriptException {
        Script parsedScript = groovyShell.parse(script);
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
