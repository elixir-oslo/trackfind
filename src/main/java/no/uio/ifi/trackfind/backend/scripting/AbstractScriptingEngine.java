package no.uio.ifi.trackfind.backend.scripting;

import org.springframework.beans.factory.annotation.Value;

/**
 * Common methods holder for all Scripting Engines.
 *
 * @author Dmytro Titov
 */
public abstract class AbstractScriptingEngine implements ScriptingEngine {

    @Value("${trackfind.scripting.variables.input}")
    protected String input;

    @Value("${trackfind.scripting.variables.output}")
    protected String output;

}
