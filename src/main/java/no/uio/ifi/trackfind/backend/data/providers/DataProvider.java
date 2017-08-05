package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;

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
     * Gets attributes to skip during indexing.
     *
     * @return Collection of unneeded attributes.
     */
    Collection<String> getAttributesToSkip();

    /**
     * Gets values to skip during indexing.
     *
     * @return Collection of unneeded values.
     */
    Collection<String> getValuesToSkip();

    /**
     * Gets BigData URLs from dataset.
     *
     * @param dataset Particular dataset as map.
     * @return BigData URLs.
     */
    Collection<String> getUrlsFromDataset(Map dataset);

    /**
     * Gets BigData URLs from dataset by type.
     *
     * @param dataset  Particular dataset as map.
     * @param dataType Type of the data.
     * @return BigData URLs.
     */
    Collection<String> getUrlsFromDataset(Map dataset, String dataType);

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
