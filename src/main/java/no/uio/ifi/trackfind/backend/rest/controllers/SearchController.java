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
 * Search REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Search", description = "Perform search over the index")
@SwaggerDefinition(tags = @Tag(name = "Search"))
@RequestMapping("/api/v1")
@RestController
// TODO: Add endpoint to fetch additional data by document ID and revision (maybe to separate controller).
public class SearchController {

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
    @ApiOperation(value = "Performs search over the index using Apache Lucene query language.")
    @GetMapping(path = "/{provider}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<Map<String, Object>>> search(@PathVariable String provider,
                                                                  @RequestParam String query,
                                                                  @RequestParam(required = false, defaultValue = "0") int limit) throws Exception {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).search(query, limit));
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
