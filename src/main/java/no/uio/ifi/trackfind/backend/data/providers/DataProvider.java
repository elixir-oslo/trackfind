package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import lombok.Data;
import org.apache.lucene.document.Document;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface describing access point for some external data repository.
 *
 * @author Dmytro Titov
 */
// TODO: Add unit-tests for new methods.
public interface DataProvider {

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
    Collection<Document> search(String query, int limit);

    /**
     * Fetches raw data by ID.
     *
     * @param documentId Lucene Document ID.
     * @return Raw (JSON) data.
     */
    Map<String, Object> fetch(String documentId);

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
    }

}
