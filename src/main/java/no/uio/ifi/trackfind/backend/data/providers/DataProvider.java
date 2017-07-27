package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;

import java.io.IOException;
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
     * Fetches data from the repository.
     *
     * @return Data as map.
     * @throws IOException in case of reading problems.
     */
    Collection<Map> fetchData() throws IOException;

    /**
     * Gets BigData URL from dataset.
     *
     * @param dataset Particular dataset as ma.
     * @return BigData URL.
     */
    String getUrlFromDataset(Map dataset);

    /**
     * Re-fetches data, rebuilds index.
     */
    void updateIndex();

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
     * Performs search over the repository.
     *
     * @param query Search query.
     * @return Search result.
     */
    Collection<Map> search(String query);

}
