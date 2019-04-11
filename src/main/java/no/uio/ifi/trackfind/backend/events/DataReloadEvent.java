package no.uio.ifi.trackfind.backend.events;

import no.uio.ifi.trackfind.backend.operations.Operation;
import org.springframework.context.ApplicationEvent;

/**
 * Event being sent upon data update (Crawling/Curation/TfMapping).
 */
public class DataReloadEvent extends ApplicationEvent {

    private String dataProviderName;

    /**
     * {@inheritDoc}
     */
    public DataReloadEvent(String dataProviderName, Operation source) {
        super(source);
        this.dataProviderName = dataProviderName;
    }

    public String getDataProviderName() {
        return dataProviderName;
    }

}
