package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.rest.responses.SearchResponse;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

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

    private TrackFindService trackFindService;

    /**
     * Performs search over the Directory of specified DataProvider.
     *
     * @param provider DataProvider name.
     * @param query    Search query (Lucene syntax, see https://lucene.apache.org/solr/guide/6_6/the-standard-query-parser.html).
     * @param limit    Max number of entries to return.
     * @return Search results by revision.
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Performs search over the index using Apache Lucene query language.", response = SearchResponse.class, responseContainer = "Map")
    @GetMapping(path = "/{provider}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Collection<Map>>> search(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "Search query to execute.", required = true, example = "Advanced>analysis_attributes>alignment_software: Bowtie")
            @RequestParam String query,
            @ApiParam(value = "Max number of results to return. Unlimited by default.", required = false, defaultValue = "0", example = "10")
            @RequestParam(required = false, defaultValue = "0") int limit) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).search(query, limit).asMap());
    }

    /**
     * Fetches raw data by ID.
     *
     * @param provider   DataProvider name.
     * @param documentId Lucene Document ID.
     * @param revision   Revision of the repository.
     * @return Raw (JSON) data.
     */
    @ApiOperation(value = "Fetches raw (JSON) data by Lucene Document ID.")
    @GetMapping(path = "/{provider}/fetch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Object>> fetch(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "ID of the document to return.", required = true)
            @RequestParam String documentId,
            @ApiParam(value = "Revision of the document to return.", required = false)
            @RequestParam(required = false) String revision) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).fetch(documentId, revision));
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
