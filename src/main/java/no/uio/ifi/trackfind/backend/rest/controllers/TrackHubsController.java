package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @ApiOperation(value = "Gets full set of Track Hubs registered in the system.")
    @GetMapping(path = "/hubs", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getTrackHubs() {
        return ResponseEntity.ok(trackFindService.getTrackHubs().values());
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
