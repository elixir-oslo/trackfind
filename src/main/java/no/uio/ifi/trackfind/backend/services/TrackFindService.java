package no.uio.ifi.trackfind.backend.services;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.TreeSet;
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

    /**
     * Get all registered DataProviders.
     *
     * @return Collection of DataProviders.
     */
    public Collection<DataProvider> getDataProviders() {
        return new TreeSet<>(dataProviders);
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
