package no.uio.ifi.trackfind.backend.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Basically the holder for all registered DataProviders.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
@Transactional
public class TrackFindService {

    private Collection<DataProvider> dataProviders;
    private HubRepository hubRepository;

    /**
     * Get all registered DataProviders.
     *
     * @return Collection of DataProviders.
     */
    @HystrixCommand
    public Collection<DataProvider> getDataProviders() {
        return dataProviders;
    }

    /**
     * Gets Track Hubs.
     *
     * @return Track Hubs.
     */
    @HystrixCommand
    public Collection<TfHub> getTrackHubs(boolean active) {
        return active ?
                dataProviders.stream().flatMap(dp -> dp.getActiveTrackHubs().stream()).collect(Collectors.toList())
                :
                dataProviders.stream().flatMap(dp -> dp.getAllTrackHubs().stream()).collect(Collectors.toList());
    }

    /**
     * Gets Track Hubs by repository.
     *
     * @return Track Hubs by repository.
     */
    @HystrixCommand
    public Collection<TfHub> getTrackHubs(String repositoryName, boolean active) {
        return active ? getDataProvider(repositoryName).getActiveTrackHubs()
                :
                getDataProvider(repositoryName).getAllTrackHubs();
    }

    /**
     * Activates hub.
     *
     * @param hubs Hubs to activate.
     */
    @HystrixCommand
    public void activateHubs(Collection<TfHub> hubs) {
        hubRepository.saveAll(hubs);
    }

    /**
     * Deactivates hub.
     *
     * @param hubs Hubs to deactivate.
     */
    @HystrixCommand
    public void deactivateHubs(Collection<TfHub> hubs) {
        hubRepository.deleteAll(hubs);
    }

    /**
     * Gets DataProvider by name.
     *
     * @param dataProviderName DataProvider's name.
     * @return DataProvider.
     */
    @HystrixCommand
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
