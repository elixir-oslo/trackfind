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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Gets all available DataProviders.
     *
     * @return Collection of DataProviders available.
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Gets full set of data providers registered in the system.")
    @GetMapping(path = "/providers", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<String>> getProviders() throws Exception {
        return ResponseEntity.ok(trackFindService.getDataProviders().stream().map(DataProvider::getName).collect(Collectors.toSet()));
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
     * Performs reinitialization of all providers.
     *
     * @throws Exception In case of some error.
     */
    @ApiOperation(value = "Re-initializes all data providers.")
    @GetMapping(path = "/reinit", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void reinit() throws Exception {
        trackFindService.getDataProviders().forEach(DataProvider::updateIndex);
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
