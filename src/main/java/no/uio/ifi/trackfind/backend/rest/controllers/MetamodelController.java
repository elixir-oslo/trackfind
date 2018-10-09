package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
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
    private TrackFindProperties trackFindProperties;

    /**
     * Gets DataProvider's metamodel in tree form.
     *
     * @param provider DataProvider name.
     * @return Metamodel in tree form.
     */
    @ApiOperation(value = "Gets the metamodel of the specified provider in the hierarchical form.")
    @GetMapping(path = "/{provider}/metamodel-tree", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Object>> getMetamodelTree(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
            @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).getMetamodelTree(raw));
    }

    /**
     * Gets DataProvider's metamodel in flat form.
     *
     * @param provider DataProvider name.
     * @return Metamodel in flat form.
     */
    @ApiOperation(value = "Gets the metamodel of the specified provider in the flat form.")
    @GetMapping(path = "/{provider}/metamodel-flat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Collection<String>>> getMetamodelFlat(
            @ApiParam(value = "Data provider name.", required = true, example = "IHEC")
            @PathVariable String provider,
            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
            @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(trackFindService.getDataProvider(provider).getMetamodelFlat(raw).asMap());
    }

    /**
     * Gets the list of attributes available in DataProvider's metamodel.
     *
     * @param provider DataProvider name.
     * @param filter   Mask to filter attributes (by 'contains' rule).
     * @return List of attributes.
     */
    @ApiOperation(value = "Gets full set of attributes for specified data provider.")
    @GetMapping(path = "/{provider}/attributes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getAttributes(@ApiParam(value = "Data provider name.", required = true, example = "IHEC")
                                                            @PathVariable String provider,
                                                            @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "data")
                                                            @RequestParam(required = false, defaultValue = "") String filter,
                                                            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                            @RequestParam(required = false, defaultValue = "false") boolean raw,
                                                            @ApiParam(value = "Return only top-level attributes", required = false, defaultValue = "false")
                                                            @RequestParam(required = false, defaultValue = "false") boolean top) {
        Set<String> attributes = top ?
                trackFindService.getDataProvider(provider).getMetamodelTree(raw).keySet() :
                trackFindService.getDataProvider(provider).getMetamodelFlat(raw).asMap().keySet();
        return ResponseEntity.ok(attributes.parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet()));
    }

    /**
     * Gets the list of sub-attributes under a specified attribute.
     *
     * @param provider  DataProvider name.
     * @param attribute Attribute name.
     * @param filter    Mask to filter attributes (by 'contains' rule).
     * @return List of attributes.
     */
    @ApiOperation(value = "Gets set of sub-attributes for specified attribute and data provider.")
    @GetMapping(path = "/{provider}/{attribute}/subattributes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getSubAttributes(@ApiParam(value = "Data provider name.", required = true, example = "IHEC")
                                                               @PathVariable String provider,
                                                               @ApiParam(value = "Attribute name.", required = true, example = "analysis_attributes")
                                                               @PathVariable String attribute,
                                                               @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "version")
                                                               @RequestParam(required = false, defaultValue = "") String filter,
                                                               @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                               @RequestParam(required = false, defaultValue = "false") boolean raw) {
        Set<String> attributes = trackFindService.getDataProvider(provider).getMetamodelFlat(raw).asMap().keySet();
        String separator = trackFindProperties.getLevelsSeparator();
        return ResponseEntity.ok(attributes
                .parallelStream()
                .filter(a -> a.startsWith(attribute))
                .map(a -> a.replace(attribute + separator, ""))
                .map(a -> (a.contains(separator) ? a.substring(0, a.indexOf(separator)) : a))
                .filter(a -> a.contains(filter))
                .collect(Collectors.toSet()));
    }

    /**
     * Gets the list of values available in for a particular attribute of DataProvider's metamodel.
     *
     * @param provider  DataProvider name.
     * @param attribute Attribute name.
     * @param filter    Mask to filter values (by 'contains' rule).
     * @return List of values.
     */
    @ApiOperation(value = "Gets full set of values for specified data provider and the attribute.")
    @GetMapping(path = "/{provider}/{attribute}/values", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getValues(@ApiParam(value = "Data provider name.", required = true, example = "IHEC")
                                                        @PathVariable String provider,
                                                        @ApiParam(value = "Attribute name.", required = true, example = "sample_data->cell_type_ontology_uri")
                                                        @PathVariable String attribute,
                                                        @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "http")
                                                        @RequestParam(required = false, defaultValue = "") String filter,
                                                        @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                        @RequestParam(required = false, defaultValue = "false") boolean raw) {
        Collection<String> values = trackFindService.getDataProvider(provider).getMetamodelFlat(raw).get(attribute);
        return ResponseEntity.ok(values.parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet()));
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setTrackFindProperties(TrackFindProperties trackFindProperties) {
        this.trackFindProperties = trackFindProperties;
    }

}
