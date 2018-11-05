package no.uio.ifi.trackfind.backend.scripting;

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
     * @param content Dataset's content to process.
     * @return Content with mapped values.
     * @throws Exception When script can't be interpreted/executed.
     */
    String execute(String script, String content) throws Exception;

}
