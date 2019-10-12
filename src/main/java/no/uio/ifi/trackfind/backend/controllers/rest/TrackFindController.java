package no.uio.ifi.trackfind.backend.controllers.rest;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.services.impl.GSuiteService;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.backend.services.impl.SearchService;
import no.uio.ifi.trackfind.backend.services.impl.TrackFindService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data REST controller.
 *
 * @author Dmytro Titov
 */
@RequestMapping("/api/v1")
@RestController
public class TrackFindController {

    private TrackFindService trackFindService;
    private MetamodelService metamodelService;
    private SearchService searchService;
    private GSuiteService gSuiteService;

    /**
     * Gets all repositories.
     *
     * @return Collection of repositories.
     */
    @GetMapping(path = "/repositories", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getRepositories() {
        return ResponseEntity.ok(trackFindService.getTrackHubs(true).stream().map(TfHub::getRepository).collect(Collectors.toSet()));
    }

    /**
     * Gets Track Hubs by repository.
     *
     * @return Collection of Track Hub names.
     */
    @GetMapping(path = "/hubs/{repository}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getHubs(
            @PathVariable String repository
    ) {
        return ResponseEntity.ok(trackFindService.getTrackHubs(repository, true).stream().map(TfHub::getName).collect(Collectors.toSet()));
    }

    /**
     * Gets Track Hub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track hub name.
     * @return Metamodel in tree form.
     */
    @GetMapping(path = "/metamodel/{repository}/{hub}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map> getMetamodel(
            @PathVariable String repository,
            @PathVariable String hub,
            @RequestParam(required = false, defaultValue = "false") boolean flat) {
        if (flat) {
            Map<String, Multimap<String, String>> metamodelFlat = metamodelService.getMetamodelFlat(repository, hub, null);
            Map<String, Map<String, Collection<String>>> result = new HashMap<>();
            for (String key : metamodelFlat.keySet()) {
                result.put(key, metamodelFlat.get(key).asMap());
            }
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.ok(metamodelService.getMetamodelTree(repository, hub));
        }
    }

    /**
     * Gets the list of attributes available in Track TfHub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track Hub name.
     * @return List of attributes.
     */
    @GetMapping(path = "/categories/{repository}/{hub}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getCategories(
            @PathVariable String repository,
            @PathVariable String hub) {
        return ResponseEntity.ok(metamodelService.getObjectTypes(repository, hub).stream().map(TfObjectType::getName).collect(Collectors.toSet()));
    }

    /**
     * Gets the list of attributes available in Track TfHub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track Hub name.
     * @param category   Category name.
     * @param path       Path to the attribute to get sub-attributes for (optional).
     * @return List of attributes.
     */
    @GetMapping(path = "/attributes/{repository}/{hub}/{category}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getAttributes(
            @PathVariable String repository,
            @PathVariable String hub,
            @PathVariable String category,
            @RequestParam(required = false) String path) {
        return ResponseEntity.ok(metamodelService.getAttributes(repository, hub, category, path));
    }

    /**
     * Gets the list of values available in for a particular attribute of Track TfHub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track Hub name.
     * @param category   Category name.
     * @param path       Path to the attribute to get values for.
     * @param filter     Optional filter for values (case-insensitive).
     * @param query      Optional search query to filter out objects before collecting available values.
     * @return List of values.
     */
    @GetMapping(path = "/values/{repository}/{hub}/{category}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getValues(
            @PathVariable String repository,
            @PathVariable String hub,
            @PathVariable String category,
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(required = false) String query) throws SQLException {
        Set<Long> ids = null;
        if (query != null) {
            ids = searchService.search(repository, hub, query, null, 0).getKey();
        }
        return ResponseEntity.ok(metamodelService.getValues(repository, hub, category, path, ids).stream().filter(v -> v.toLowerCase().contains(filter.toLowerCase())).collect(Collectors.toSet()));
    }

    /**
     * Performs search over the Directory of specified Track TfHub.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param query      Search query.
     * @param categories Comma-separated categories.
     * @param limit      Max number of entries to return.
     * @return Search results by version.
     */
    @GetMapping(path = "/search/{repository}/{hub}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<SearchResult>> searchJSON(
            @PathVariable String repository,
            @PathVariable String hub,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "") String categories,
            @RequestParam(required = false, defaultValue = "0") long limit) {
        try {
            return ResponseEntity.ok(searchService.search(repository, hub, query, Arrays.stream(StringUtils.split(categories, ",")).map(String::trim).collect(Collectors.toSet()), limit).getValue());
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Performs search over the Directory of specified Track TfHub.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param query      Search query.
     * @param categories Comma-separated categories.
     * @param limit      Max number of entries to return. 0 for unlimited.
     * @return Search results by version.
     */
    @GetMapping(path = "/search/{repository}/{hub}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> searchGSuite(
            @PathVariable String repository,
            @PathVariable String hub,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "") String categories,
            @RequestParam(required = false, defaultValue = "0") long limit) {
        try {
            Collection<SearchResult> datasets = searchService.search(repository, hub, query, Arrays.stream(StringUtils.split(categories, ",")).map(String::trim).collect(Collectors.toSet()), limit).getValue();
            return ResponseEntity.ok(gSuiteService.apply(datasets));
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Autowired
    public void setgSuiteService(GSuiteService gSuiteService) {
        this.gSuiteService = gSuiteService;
    }

}
