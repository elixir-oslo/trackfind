package no.uio.ifi.trackfind.rest;

import no.uio.ifi.trackfind.services.TrackFindService;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
    public Object search() throws IOException, ParseException {
        Collection<Document> documents = trackFindService.search("sample_id", "CEMT0055.gDNA");
        return documents.stream().map(Document::getFields).collect(Collectors.toSet());
    }

}
