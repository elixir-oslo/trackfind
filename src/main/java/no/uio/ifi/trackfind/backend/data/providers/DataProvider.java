package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.dao.Dataset;

import java.util.Collection;
import java.util.Map;

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
     * Gets the names of track hubs by this data provider.
     *
     * @return Track hubs list.
     */
    Collection<String> getTrackHubs();

    /**
     * Re-fetches data, rebuilds index.
     */
    void crawlRemoteRepository();

    /**
     * Applies attributes mappings, rebuilds index.
     */
    void applyMappings();

    /**
     * Resets caches
     */
    void resetCaches();

    /**
     * Gets metamodel of the repository in "tree-from" (with nesting).
     *
     * @return Tree metamodel.
     */
    Map<String, Object> getMetamodelTree(boolean raw);

    /**
     * Gets metamodel of the repository in "flat-from" (non-nested).
     *
     * @return Flat metamodel.
     */
    Multimap<String, String> getMetamodelFlat(boolean raw);

    /**
     * Performs search over the repository limiting the number of results.
     *
     * @param query Search query.
     * @param limit Results quantity limit, 0 for unlimited.
     * @return Search result.
     */
    Collection<Dataset> search(String query, int limit);

    /**
     * Fetches Dataset by ID.
     *
     * @param datasetId Dataset ID.
     * @param version   Version.
     * @return Dataset.
     */
    Dataset fetch(Long datasetId, String version);

}
