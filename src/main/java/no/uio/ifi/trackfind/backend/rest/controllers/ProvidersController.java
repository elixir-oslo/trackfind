package no.uio.ifi.trackfind.backend.rest.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Data providers REST controller.
 *
 * @author Dmytro Titov
 */
@Api(tags = "Data providers", description = "List and manage available data providers")
@SwaggerDefinition(tags = @Tag(name = "Data providers"))
@RequestMapping("/api/v1")
@RestController
public class ProvidersController {

    private TrackFindService trackFindService;

    /**
     * Gets all available DataProviders (only published ones by default).
     *
     * @param published <code>true</code> for returning published only providers, <code>false</code> for returning all providers.
     * @return Collection of DataProviders available.
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Gets full set of data providers registered in the system.")
    @GetMapping(path = "/providers", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getProviders(@RequestParam(required = false, defaultValue = "true") boolean published) throws Exception {
        return ResponseEntity.ok(trackFindService.getDataProviders(published).stream().map(DataProvider::getName).collect(Collectors.toSet()));
    }

    /**
     * Performs reinitialization of particular DataProvider.
     *
     * @param provider DataProvider name.
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Re-initializes specified data provider.")
    @GetMapping(path = "/{provider}/reinit", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void reinit(@PathVariable String provider) throws Exception {
        trackFindService.getDataProvider(provider).updateIndex();
    }

    /**
     * Performs reinitialization of all or published only providers.
     *
     * @param published <code>true</code> for reiniting published only providers, <code>false</code> for reiniting all providers.
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Re-initializes all or published only data providers.")
    @GetMapping(path = "/reinit", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void reinit(@RequestParam boolean published) throws Exception {
        trackFindService.getDataProviders(published).forEach(DataProvider::updateIndex);
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
