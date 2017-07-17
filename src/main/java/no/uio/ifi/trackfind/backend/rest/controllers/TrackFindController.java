package no.uio.ifi.trackfind.backend.rest.controllers;

import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class TrackFindController {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindController(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @GetMapping(path = "/reinit", produces = "application/json")
    public void reinit() throws Exception {
        trackFindService.updateIndex();
        trackFindService.resetCaches();
    }

    @GetMapping(path = "/metamodel-tree", produces = "application/json")
    public Object getMetamodelTree() throws IOException, ParseException {
        return trackFindService.getMetamodelTree();
    }

    @GetMapping(path = "/metamodel-flat", produces = "application/json")
    public Object getMetamodelFlat() throws IOException, ParseException {
        return trackFindService.getMetamodelFlat().asMap();
    }

    @GetMapping(path = "/attributes", produces = "application/json")
    public Object getAttributes(@RequestParam(required = false, defaultValue = "") String expression) throws IOException, ParseException {
        Set<String> attributes = trackFindService.getMetamodelFlat().asMap().keySet();
        return attributes.stream().filter(a -> a.contains(expression)).collect(Collectors.toSet());
    }

    @GetMapping(path = "/values", produces = "application/json")
    public Object getValues(@RequestParam String attribute) throws IOException, ParseException {
        return trackFindService.getMetamodelFlat().get(attribute);
    }

    @GetMapping(path = "/search", produces = "application/json")
    public Object search(@RequestParam String query) throws IOException, ParseException {
        return trackFindService.search(query);
    }

}
