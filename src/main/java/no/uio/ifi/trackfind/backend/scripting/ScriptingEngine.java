package no.uio.ifi.trackfind.backend.scripting;

import org.apache.lucene.document.Document;

import java.util.Collection;

/**
 * Scripting Engine for performing dynamic attribute mappings.
 *
 * @author Dmytro Titov
 */
public interface ScriptingEngine {

    /**
     * Gets the scripting language.
     *
     * @return Scripting language.
     */
    String getLanguage();

    /**
     * Execute script.
     *
     * @param script   Mappings script.
     * @param document Document to process.
     * @return Mapped values.
     * @throws Exception When script can't be interpreted/executed.
     */
    Collection<String> execute(String script, Document document) throws Exception;

}
