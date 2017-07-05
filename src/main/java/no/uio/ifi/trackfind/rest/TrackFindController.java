package no.uio.ifi.trackfind.rest;

import no.uio.ifi.trackfind.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackFindController {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindController(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @GetMapping(path = "/metamodel", produces = "application/json")
    public Object getMetamodel() {
        return trackFindService.getMetamodel();
    }

}
