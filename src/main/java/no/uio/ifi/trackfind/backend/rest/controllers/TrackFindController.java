package no.uio.ifi.trackfind.backend.rest.controllers;

import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main REST controller exposing all main features of the system.
 *
 * @author Dmytro Titov
 */
@RequestMapping("/api/v1")
@RestController
public class TrackFindController {

    private TrackFindService trackFindService;

    /**
     * Gets all available DataProviders.
     *
     * @return List of DataProviders available.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/providers", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getProviders() throws Exception {
        return trackFindService.getDataProviders().stream().map(DataProvider::getName).collect(Collectors.toSet());
    }

    /**
     * Performs reinitialization of particular DataProvider.
     *
     * @param provider DataProvider name.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/reinit", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void reinit(@PathVariable String provider) throws Exception {
        trackFindService.getDataProvider(provider).updateIndex();
    }

    /**
     * Performs reinitialization of all providers.
     *
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/reinit", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void reinit() throws Exception {
        trackFindService.getDataProviders().forEach(DataProvider::updateIndex);
    }

    /**
     * Gets DataProvider's metamodel in tree form.
     *
     * @param provider DataProvider name.
     * @return Metamodel in tree form.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/metamodel-tree", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getMetamodelTree(@PathVariable String provider) throws Exception {
        return trackFindService.getDataProvider(provider).getMetamodelTree();
    }

    /**
     * Gets DataProvider's metamodel in flat form.
     *
     * @param provider DataProvider name.
     * @return Metamodel in flat form.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/metamodel-flat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getMetamodelFlat(@PathVariable String provider) throws Exception {
        return trackFindService.getDataProvider(provider).getMetamodelFlat().asMap();
    }

    /**
     * Gets the list of attributes available in DataProvider's metamodel.
     *
     * @param provider DataProvider name.
     * @param filter   Mask to filter attributes (by 'contains' rule).
     * @return List of attributes.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/attributes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getAttributes(@PathVariable String provider,
                                @RequestParam(required = false, defaultValue = "") String filter) throws Exception {
        Set<String> attributes = trackFindService.getDataProvider(provider).getMetamodelFlat().asMap().keySet();
        return attributes.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    /**
     * Gets the list of values available in for a particular attribute of DataProvider's metamodel.
     *
     * @param provider  DataProvider name.
     * @param attribute Attribute name.
     * @param filter    Mask to filter values (by 'contains' rule).
     * @return List of values.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/values", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getValues(@PathVariable String provider,
                            @RequestParam String attribute,
                            @RequestParam(required = false, defaultValue = "") String filter) throws Exception {
        Collection<String> values = trackFindService.getDataProvider(provider).getMetamodelFlat().get(attribute);
        return values.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    /**
     * Performs search over the Directory of specified DataProvider.
     *
     * @param provider DataProvider name.
     * @param query    Search query (Lucene syntax, see https://lucene.apache.org/solr/guide/6_6/the-standard-query-parser.html).
     * @param limit    Max number of entries to return.
     * @return Search results.
     * @throws Exception In case of some error.
     */
    @GetMapping(path = "/{provider}/search", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object search(@PathVariable String provider,
                         @RequestParam String query,
                         @RequestParam(required = false, defaultValue = "0") int limit) throws Exception {
        return trackFindService.getDataProvider(provider).search(query, limit);
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
