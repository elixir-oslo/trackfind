package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.dao.Hub;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Data providers REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Track Hubss", description = "List and manage available Track Hubs")
@SwaggerDefinition(tags = @Tag(name = "Track Hubs"))
@RequestMapping("/api/v1")
@RestController
public class TrackHubsController {

    private TrackFindService trackFindService;

    /**
     * Gets all available Track Hubs.
     *
     * @return Collection of Track Hubs available.
     */
    @ApiOperation(value = "Gets set of Track Hubs registered in the system.")
    @GetMapping(path = "/hubs", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<Hub>> getTrackHubs(@ApiParam(value = "Only Active Track Hubs", required = false, defaultValue = "true")
                                                        @RequestParam(required = false, defaultValue = "false") boolean active) {
        return ResponseEntity.ok(active ? trackFindService.getActiveTrackHubs() : trackFindService.getAllTrackHubs());
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
