package no.uio.ifi.trackfind.backend.data.providers;

import no.uio.ifi.trackfind.backend.pojo.TfHub;

import java.util.Collection;

/**
 * Interface describing access point for some external data repositories.
 *
 * @author Dmytro Titov
 */
public interface DataProvider {

    /**
     * Gets the name of the data provider.
     *
     * @return Data provider name.
     */
    String getName();

    /**
     * Gets the names of all Track Hubs by this data provider.
     *
     * @return All Track Hubs list.
     */
    Collection<TfHub> getAllTrackHubs();

    /**
     * Gets the names of active Track hubs by this data provider.
     *
     * @return Active Track Hubs list.
     */
    Collection<TfHub> getActiveTrackHubs();

    /**
     * Re-fetches data, rebuilds index.
     *
     * @param hubName TfHub name.
     */
    void crawlRemoteRepository(String hubName);

    /**
     * Applies attributes mappings, rebuilds index.
     *
     * @param hubName TfHub name.
     */
    void applyMappings(String hubName);

}
