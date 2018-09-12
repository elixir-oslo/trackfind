package no.uio.ifi.trackfind.backend.scripting;

import java.util.Collection;
import java.util.Map;

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
     * @param script  Mappings script.
     * @param dataset Dataset to process.
     * @return Mapped values.
     * @throws Exception When script can't be interpreted/executed.
     */
    Collection<String> execute(String script, Map dataset) throws Exception;

}
