package no.uio.ifi.trackfind.backend.rest.controllers;

import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping(path = "providers", produces = "application/json")
    public Object getProviders() throws Exception {
        return trackFindService.getDataProviders().stream().map(dp -> dp.getClass().getSimpleName()).collect(Collectors.toSet());
    }

    @GetMapping(path = "/{provider}/reinit", produces = "application/json")
    public void reinit(@PathVariable String provider) throws Exception {
        trackFindService.getDataProvider(provider).updateIndex();
    }

    @GetMapping(path = "/{provider}/metamodel-tree", produces = "application/json")
    public Object getMetamodelTree(@PathVariable String provider) throws IOException, ParseException {
        return trackFindService.getDataProvider(provider).getMetamodelTree();
    }

    @GetMapping(path = "/{provider}/metamodel-flat", produces = "application/json")
    public Object getMetamodelFlat(@PathVariable String provider) throws IOException, ParseException {
        return trackFindService.getDataProvider(provider).getMetamodelFlat().asMap();
    }

    @GetMapping(path = "/{provider}/attributes", produces = "application/json")
    public Object getAttributes(@PathVariable String provider,
                                @RequestParam(required = false, defaultValue = "") String expression) throws IOException, ParseException {
        Set<String> attributes = trackFindService.getDataProvider(provider).getMetamodelFlat().asMap().keySet();
        return attributes.stream().filter(a -> a.contains(expression)).collect(Collectors.toSet());
    }

    @GetMapping(path = "/{provider}/values", produces = "application/json")
    public Object getValues(@PathVariable String provider,
                            @RequestParam String attribute) throws IOException, ParseException {
        return trackFindService.getDataProvider(provider).getMetamodelFlat().get(attribute);
    }

    @GetMapping(path = "/{provider}/search", produces = "application/json")
    public Object search(@PathVariable String provider,
                         @RequestParam String query) throws IOException, ParseException {
        return trackFindService.getDataProvider(provider).search(query);
    }

}
