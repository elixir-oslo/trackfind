package no.uio.ifi.trackfind.rest.controllers;

import no.uio.ifi.trackfind.services.TrackFindService;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
public class TrackFindController {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindController(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @GetMapping(path = "/metamodel", produces = "application/json")
    public Object getMetamodel() throws IOException, ParseException {
        return trackFindService.getMetamodel();
    }

    @GetMapping(path = "/search", produces = "application/json")
    public Object search(@RequestParam String query) throws IOException, ParseException {
        Collection<Document> documents = trackFindService.search(query);
        return documents.stream().map(d -> SerializationUtils.deserialize(d.getBinaryValue(TrackFindService.DATASET).bytes)).collect(Collectors.toSet());
    }

}
