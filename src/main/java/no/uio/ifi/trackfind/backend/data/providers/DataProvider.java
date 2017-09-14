package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Interface describing access point for some external data repository.
 *
 * @author Dmytro Titov
 */
public interface DataProvider {

    String DATA_URL_ATTRIBUTE = "browser";
    String DATA_TYPE_ATTRIBUTE = "data_type";

    Collection<String> BASIC_ATTRIBUTES = Collections.singleton("data_type");

    /**
     * Gets the name of the repository.
     *
     * @return Repository name.
     */
    String getName();

    /**
     * Gets BigData URLs from dataset.
     *
     * @param query   Search query used for finding this dataset.
     * @param dataset Particular dataset as map.
     * @return BigData URLs.
     */
    Collection<String> getUrlsFromDataset(String query, Map dataset);

    /**
     * Re-fetches data, rebuilds index.
     */
    void updateIndex();

    /**
     * Gets metamodel of the repository in "tree-from" (with nesting).
     *
     * @param advanced <code>true</code> for advanced metamodel, <code>false</code> for basic one.
     * @return Tree metamodel.
     */
    Map<String, Object> getMetamodelTree(boolean advanced);

    /**
     * Gets metamodel of the repository in "flat-from" (non-nested).
     *
     * @param advanced <code>true</code> for advanced metamodel, <code>false</code> for basic one.
     * @return Flat metamodel.
     */
    Multimap<String, String> getMetamodelFlat(boolean advanced);

    /**
     * Performs search over the repository limiting the number of results.
     *
     * @param query Search query.
     * @param limit Results quantity limit, 0 for unlimited.
     * @return Search result.
     */
    Collection<Map<String, Object>> search(String query, int limit);

}
