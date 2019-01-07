package no.uio.ifi.trackfind.backend.services;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.dao.Hub;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Basically the holder for all registered DataProviders.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
public class TrackFindService {

    private Collection<DataProvider> dataProviders;
    private HubRepository hubRepository;

    /**
     * Get all registered DataProviders.
     *
     * @return Collection of DataProviders.
     */
    public Collection<DataProvider> getDataProviders() {
        return dataProviders;
    }

    /**
     * Gets all Track Hubs by DataProviders.
     *
     * @return All Track Hubs by DataProviders.
     */
    public Collection<Hub> getAllTrackHubs() {
        return dataProviders.stream().flatMap(dp -> dp.getAllTrackHubs().stream()).collect(Collectors.toList());
    }

    /**
     * Gets active Track Hubs by DataProviders.
     *
     * @return Active Track Hubs by DataProviders.
     */
    public Collection<Hub> getActiveTrackHubs() {
        return dataProviders.stream().flatMap(dp -> dp.getActiveTrackHubs().stream()).collect(Collectors.toList());
    }

    /**
     * Activates hub.
     *
     * @param hubs Hubs to activate.
     */
    public void activateHubs(Collection<Hub> hubs) {
        hubRepository.saveAll(hubs);
    }

    /**
     * Deactivates hub.
     *
     * @param hubs Hubs to deactivate.
     */
    public void deactivateHubs(Collection<Hub> hubs) {
        hubRepository.deleteAll(hubs);
    }

    /**
     * Gets DataProvider by name.
     *
     * @param dataProviderName DataProvider's name.
     * @return DataProvider.
     */
    public DataProvider getDataProvider(String dataProviderName) {
        return getDataProviders().parallelStream().filter(dp -> dp.getName().equals(dataProviderName)).findAny().orElseThrow(RuntimeException::new);
    }

    @Autowired
    public void setDataProviders(Collection<DataProvider> dataProviders) {
        this.dataProviders = dataProviders;
    }

    @Autowired
    public void setHubRepository(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

}
