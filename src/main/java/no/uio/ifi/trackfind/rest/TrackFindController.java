package no.uio.ifi.trackfind.rest;

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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TrackFindController {

    private static final String QUERY_DELIMITER = ",";
    private static final String PART_DELIMITER = ":";

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
        Map<String, String> attributesToValues = parseQuery(query);
        Collection<Document> documents = trackFindService.search(attributesToValues);
        return documents.stream().map(d -> SerializationUtils.deserialize(d.getBinaryValue(TrackFindService.DATASET).bytes)).collect(Collectors.toSet());
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        String[] parts = query.split(QUERY_DELIMITER);
        for (String part : parts) {
            String[] attributeValue = part.split(PART_DELIMITER);
            String attribute = attributeValue[0];
            String value = attributeValue[1];
            result.put(attribute, value);
        }
        return result;
    }


}
