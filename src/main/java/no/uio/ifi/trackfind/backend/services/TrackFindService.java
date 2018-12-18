package no.uio.ifi.trackfind.backend.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.TreeSet;

/**
 * Basically the holder for all registered DataProviders.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
public class TrackFindService {

    private Collection<DataProvider> dataProviders;

    /**
     * Get all registered DataProviders.
     *
     * @return Collection of DataProviders.
     */
    public Collection<DataProvider> getDataProviders() {
        return new TreeSet<>(dataProviders);
    }

    /**
     * Gets all Track Hubs by DataProviders.
     *
     * @return Track Hubs by DataProviders.
     */
    public Multimap<String, String> getTrackHubs() {
        Multimap<String, String> hubs = HashMultimap.create();
        for (DataProvider dataProvider : getDataProviders()) {
            Collection<String> trackHubs = dataProvider.getTrackHubs();
            for (String trackHub : trackHubs) {
                hubs.put(dataProvider.getName(), trackHub);
            }
        }
        return hubs;
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

}
