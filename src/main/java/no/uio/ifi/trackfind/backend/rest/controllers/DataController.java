package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

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
     * @param query    Search query (Lucene syntax, see https://lucene.apache.org/solr/guide/6_6/the-standard-query-parser.html).
     * @param limit    Max number of entries to return.
     * @return Search results.
     * @throws Exception In case of some error.
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Performs search over the index using Apache Lucene query language.")
    @GetMapping(path = "/{provider}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<Map>> search(@PathVariable String provider,
                                                  @RequestParam String query,
                                                  @RequestParam(required = false, defaultValue = "0") int limit) throws Exception {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).search(query, limit));
    }

    /**
     * Fetches raw data by ID.
     *
     * @param provider   DataProvider name.
     * @param documentId Lucene Document ID.
     * @return Raw (JSON) data.
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Fetches raw (JSON) data by Lucene Document ID.")
    @GetMapping(path = "/{provider}/fetch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Object>> fetch(@PathVariable String provider,
                                                     @RequestParam String documentId) throws Exception {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).fetch(documentId));
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
