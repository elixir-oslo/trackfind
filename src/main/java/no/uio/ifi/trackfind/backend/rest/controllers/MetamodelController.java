package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Meta-model REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Meta-model", description = "Explore providers' meta-model")
@SwaggerDefinition(tags = @Tag(name = "Meta-model"))
@RequestMapping("/api/v1")
@RestController
public class MetamodelController {

    private TrackFindService trackFindService;

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
    @GetMapping(path = "/{provider}/{attribute}/values", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getValues(@PathVariable String provider,
                            @PathVariable String attribute,
                            @RequestParam(required = false, defaultValue = "") String filter) throws Exception {
        Collection<String> values = trackFindService.getDataProvider(provider).getMetamodelFlat().get(attribute);
        return values.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
