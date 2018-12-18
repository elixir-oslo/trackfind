package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import no.uio.ifi.trackfind.backend.services.GSuiteService;
import no.uio.ifi.trackfind.backend.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;

/**
 * Data REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Data", description = "Perform search over the index and fetch data")
@SwaggerDefinition(tags = @Tag(name = "Data"))
@RequestMapping("/api/v1")
@RestController
public class DataController {

    private SearchService searchService;
    private GSuiteService gSuiteService;

    /**
     * Performs search over the Directory of specified Track Hub.
     *
     * @param hub   Track Hub name.
     * @param query Search query.
     * @param limit Max number of entries to return.
     * @return Search results by version.
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Performs search in the database.", responseContainer = "Set")
    @GetMapping(path = "/{hub}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<Dataset>> searchJSON(
            @ApiParam(value = "Track Hub name.", required = true, example = "IHEC")
            @PathVariable String hub,
            @ApiParam(value = "Search query to execute.", required = true,
                    example = "curated_content->'analysis_attributes'->>'alignment_software' = 'BISMARK' AND curated_content->'analysis_attributes'->>'analysis_software_version' IN ('v0.14.4', 'v0.14.5')")
            @RequestParam String query,
            @ApiParam(value = "Max number of results to return. Unlimited by default.", required = false, defaultValue = "0", example = "10")
            @RequestParam(required = false, defaultValue = "0") int limit) {
        return ResponseEntity.ok(searchService.search(hub, query, limit));
    }

    /**
     * Performs search over the Directory of specified Track Hub.
     *
     * @param hub   Track Hub name.
     * @param query Search query.
     * @param limit Max number of entries to return.
     * @return Search results by version.
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Performs search in the database.", responseContainer = "Set")
    @GetMapping(path = "/{hub}/search", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> searchGSuite(
            @ApiParam(value = "Track Hub name.", required = true, example = "IHEC")
            @PathVariable String hub,
            @ApiParam(value = "Search query to execute.", required = true,
                    example = "curated_content->'analysis_attributes'->>'alignment_software' = 'BISMARK' AND curated_content->'analysis_attributes'->>'analysis_software_version' IN ('v0.14.4', 'v0.14.5')")
            @RequestParam String query,
            @ApiParam(value = "Max number of results to return. Unlimited by default.", required = false, defaultValue = "0", example = "10")
            @RequestParam(required = false, defaultValue = "0") int limit) {
        Collection<Dataset> datasets = searchService.search(hub, query, limit);
        return ResponseEntity.ok(gSuiteService.apply(datasets));
    }

    /**
     * Fetches raw data by ID.
     *
     * @param id      Dataset ID.
     * @param version Version of the dataset.
     * @return Raw (JSON) data.
     */
    @ApiOperation(value = "Fetches raw (JSON) data by DatasetID.")
    @GetMapping(path = "/fetch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Dataset> fetchJSON(
            @ApiParam(value = "ID of the dataset to return.", required = true)
            @RequestParam Long id,
            @ApiParam(value = "Version of the dataset to return.", required = false)
            @RequestParam(required = false) String version) {
        return ResponseEntity.ok(searchService.fetch(id, version));
    }

    /**
     * Fetches raw data by ID.
     *
     * @param id      Dataset ID.
     * @param version Version of the dataset.
     * @return Raw (JSON) data.
     */
    @ApiOperation(value = "Fetches raw (JSON) data by DatasetID.")
    @GetMapping(path = "/fetch", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> fetchGSuite(
            @ApiParam(value = "ID of the dataset to return.", required = true)
            @RequestParam Long id,
            @ApiParam(value = "Version of the dataset to return.", required = false)
            @RequestParam(required = false) String version) {
        Dataset dataset = searchService.fetch(id, version);
        return ResponseEntity.ok(gSuiteService.apply(Collections.singleton(dataset)));
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
