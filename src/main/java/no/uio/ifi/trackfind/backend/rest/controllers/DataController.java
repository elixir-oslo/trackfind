package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

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
     * @param query    Search query.
     * @param limit    Max number of entries to return.
     * @return Search results by revision.
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Performs search in the database.", responseContainer = "Set")
    @GetMapping(path = "/{provider}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<Dataset>> search(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "Search query to execute.", required = true, example = "Advanced>analysis_attributes>alignment_software: Bowtie")
            @RequestParam String query,
            @ApiParam(value = "Max number of results to return. Unlimited by default.", required = false, defaultValue = "0", example = "10")
            @RequestParam(required = false, defaultValue = "0") int limit) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).search(query, limit));
    }

    /**
     * Fetches raw data by ID.
     *
     * @param provider DataProvider name.
     * @param id       Dataset ID.
     * @param revision Revision of the repository.
     * @return Raw (JSON) data.
     */
    @ApiOperation(value = "Fetches raw (JSON) data by DatasetID.")
    @GetMapping(path = "/{provider}/fetch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Dataset> fetch(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "ID of the dataset to return.", required = true)
            @RequestParam String id,
            @ApiParam(value = "Revision of the dataset to return.", required = false)
            @RequestParam(required = false) String revision) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).fetch(id, revision));
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
