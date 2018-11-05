package no.uio.ifi.trackfind.backend.scripting;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common methods holder for all Scripting Engines.
 *
 * @author Dmytro Titov
 */
public abstract class AbstractScriptingEngine implements ScriptingEngine {

    protected TrackFindProperties properties;

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
