package no.uio.ifi.trackfind.rest.controllers;

import no.uio.ifi.trackfind.services.TrackFindService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TrackFindController {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindController(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @GetMapping(path = "/reinit", produces = "application/json")
    public void reinit() throws Exception {
        trackFindService.reinit();
    }

    @GetMapping(path = "/metamodel", produces = "application/json")
    public Object getMetamodel() throws IOException, ParseException {
        return trackFindService.getMetamodel();
    }

    @GetMapping(path = "/search", produces = "application/json")
    public Object search(@RequestParam String query) throws IOException, ParseException {
        return trackFindService.search(query);
    }

}
