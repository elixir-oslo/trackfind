package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.dao.Dataset;

import java.util.Collection;
import java.util.Map;

/**
 * Interface describing access point for some external data repository.
 *
 * @author Dmytro Titov
 */
public interface DataProvider {

    /**
     * Gets the name of the repository.
     *
     * @return Repository name.
     */
    String getName();

    /**
     * Re-fetches data, rebuilds index.
     */
    void crawlRemoteRepository();

    /**
     * Applies attributes mappings, rebuilds index.
     */
    void applyMappings();

    /**
     * Gets metamodel of the repository in "tree-from" (with nesting).
     *
     * @return Tree metamodel.
     */
    Map<String, Object> getMetamodelTree();

    /**
     * Gets metamodel of the repository in "flat-from" (non-nested).
     *
     * @return Flat metamodel.
     */
    Multimap<String, String> getMetamodelFlat();

    /**
     * Performs search over the repository limiting the number of results.
     *
     * @param query Search query.
     * @param limit Results quantity limit, 0 for unlimited.
     * @return Search result.
     */
    Collection<Dataset> search(String query, int limit);

    /**
     * Fetches raw data by ID.
     *
     * @param datasetId Dataset ID.
     * @param version   Version.
     * @return Raw (JSON) data.
     */
    Map<String, Object> fetch(String datasetId, String version);

}
