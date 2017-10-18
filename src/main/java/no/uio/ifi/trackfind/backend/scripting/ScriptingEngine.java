package no.uio.ifi.trackfind.backend.scripting;

import org.apache.lucene.document.Document;

import javax.script.ScriptException;
import java.util.Collection;

public interface ScriptingEngine {

    String getLanguage();

    Collection<String> execute(String script, Document document) throws ScriptException;

}
