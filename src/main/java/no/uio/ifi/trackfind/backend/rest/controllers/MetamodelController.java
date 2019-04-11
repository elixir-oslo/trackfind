package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.*;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Meta-model REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Meta-model", description = "Explore Track Hubs' meta-model")
@SwaggerDefinition(tags = @Tag(name = "Meta-model"))
@RequestMapping("/api/v1")
@RestController
public class MetamodelController {

    private MetamodelService metamodelService;

    /**
     * Gets Track TfHub's metamodel in tree form.
     *
     * @param repository Repository name.
     * @param hub        Track hub name.
     * @param raw        Raw or Standardized metamodel.
     * @return Metamodel in tree form.
     */
    @ApiOperation(value = "Gets the metamodel of the specified Track TfHub in the hierarchical form.")
    @GetMapping(path = "/{repository}/{hub}/metamodel-tree", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Object>> getMetamodelTree(
            @ApiParam(value = "Repository name.", required = true, example = "TrackHubRegistry")
            @PathVariable String repository,
            @ApiParam(value = "Track TfHub name.", required = true, example = "IHEC")
            @PathVariable String hub,
            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
            @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(metamodelService.getMetamodelTree(new TfHub(repository, hub), raw));
    }

    /**
     * Gets Track TfHub's metamodel in flat form.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param raw        Raw or Standardized metamodel.
     * @return Metamodel in flat form.
     */
    @ApiOperation(value = "Gets the metamodel of the specified Track TfHub in the flat form.")
    @GetMapping(path = "/{repository}/{hub}/metamodel-flat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Collection<String>>> getMetamodelFlat(
            @ApiParam(value = "Repository name.", required = true, example = "TrackHubRegistry")
            @PathVariable String repository,
            @ApiParam(value = "Track TfHub name.", required = true, example = "IHEC")
            @PathVariable String hub,
            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
            @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(metamodelService.getMetamodelFlat(new TfHub(repository, hub), raw).asMap());
    }

    /**
     * Gets the list of attributes available in Track TfHub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param filter     Mask to filter attributes (by 'contains' rule).
     * @param raw        Raw or Standardized metamodel.
     * @param top        <code>true</code> for returning only top attributes.
     * @return List of attributes.
     */
    @ApiOperation(value = "Gets full set of attributes for specified Track TfHub.")
    @GetMapping(path = "/{repository}/{hub}/attributes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getAttributes(@ApiParam(value = "Repository name.", required = true, example = "TrackHubRegistry")
                                                            @PathVariable String repository,
                                                            @ApiParam(value = "Track TfHub name.", required = true, example = "IHEC")
                                                            @PathVariable String hub,
                                                            @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "data")
                                                            @RequestParam(required = false, defaultValue = "") String filter,
                                                            @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                            @RequestParam(required = false, defaultValue = "false") boolean raw,
                                                            @ApiParam(value = "Return only top-level attributes", required = false, defaultValue = "false")
                                                            @RequestParam(required = false, defaultValue = "false") boolean top) {
        return ResponseEntity.ok(metamodelService.getAttributes(new TfHub(repository, hub), filter, raw, top));
    }

    /**
     * Gets the list of sub-attributes under a specified attribute.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param attribute  Attribute name.
     * @param filter     Mask to filter attributes (by 'contains' rule).
     * @param raw        Raw or Standardized metamodel.
     * @return List of attributes.
     */
    @ApiOperation(value = "Gets set of sub-attributes for specified attribute and Track TfHub.")
    @GetMapping(path = "/{repository}/{hub}/{attribute}/subattributes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getSubAttributes(@ApiParam(value = "Repository name.", required = true, example = "TrackHubRegistry")
                                                               @PathVariable String repository,
                                                               @ApiParam(value = "Track TfHub name.", required = true, example = "IHEC")
                                                               @PathVariable String hub,
                                                               @ApiParam(value = "Attribute name.", required = true, example = "analysis_attributes")
                                                               @PathVariable String attribute,
                                                               @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "version")
                                                               @RequestParam(required = false, defaultValue = "") String filter,
                                                               @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                               @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(metamodelService.getSubAttributes(new TfHub(repository, hub), attribute, filter, raw));
    }

    /**
     * Gets the list of values available in for a particular attribute of Track TfHub's metamodel.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param attribute  Attribute name.
     * @param filter     Mask to filter values (by 'contains' rule).
     * @return List of values.
     */
    @ApiOperation(value = "Gets full set of values for specified Track TfHub and the attribute.")
    @GetMapping(path = "/{repository}/{hub}/{attribute}/values", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getValues(@ApiParam(value = "Repository name.", required = true, example = "TrackHubRegistry")
                                                        @PathVariable String repository,
                                                        @ApiParam(value = "Track TfHub name.", required = true, example = "IHEC")
                                                        @PathVariable String hub,
                                                        @ApiParam(value = "Attribute name.", required = true, example = "sample_data->cell_type_ontology_uri")
                                                        @PathVariable String attribute,
                                                        @ApiParam(value = "Text mask to use as a filter.", required = false, defaultValue = "", example = "http")
                                                        @RequestParam(required = false, defaultValue = "") String filter,
                                                        @ApiParam(value = "Raw or Standardized metamodel", required = false, defaultValue = "false")
                                                        @RequestParam(required = false, defaultValue = "false") boolean raw) {
        return ResponseEntity.ok(metamodelService.getValues(new TfHub(repository, hub), attribute, filter, raw));
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
