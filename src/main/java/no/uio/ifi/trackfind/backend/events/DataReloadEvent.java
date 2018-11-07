package no.uio.ifi.trackfind.backend.events;

import no.uio.ifi.trackfind.backend.operations.Operation;
import org.springframework.context.ApplicationEvent;

/**
 * Event being sent upon data update (Crawling/Curation/Mapping).
 */
public class DataReloadEvent extends ApplicationEvent {

    /**
     * {@inheritDoc}
     */
    public DataReloadEvent(Operation source) {
        super(source);
    }

}
