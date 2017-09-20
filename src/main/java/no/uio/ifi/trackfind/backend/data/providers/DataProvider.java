package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface describing access point for some external data repository.
 *
 * @author Dmytro Titov
 */
public interface DataProvider {

    String DATA_URL_ATTRIBUTE = "browser";
    String DATA_TYPE_ATTRIBUTE = "data_type";

    /**
     * Gets the name of the repository.
     *
     * @return Repository name.
     */
    String getName();

    /**
     * Gets the path of the repository.
     *
     * @return Repository name.
     */
    String getPath();

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
    void crawlRemoteRepository();

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
    Collection<Map<String, Object>> search(String query, int limit);

    /**
     * Loads DataProvider configuration from config-file.
     *
     * @return Configuration for this DataProvider.
     */
    Configuration loadConfiguration();

    /**
     * Saves DataProvider configuration to config-file.
     *
     * @param configuration Configuration to save.
     */
    void saveConfiguration(Configuration configuration);

    /**
     * Inner class for representing configuration of DataProvider.
     */
    @Data
    class Configuration {
        private Map<String, String> attributesMapping = new HashMap<>();
        private boolean published = false;
    }

}
