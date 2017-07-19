package no.uio.ifi.trackfind.backend.services;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.TreeSet;

@Slf4j
@Service
public class TrackFindService {

    private final Collection<DataProvider> dataProviders;

    @Autowired
    public TrackFindService(Collection<DataProvider> dataProviders) {
        this.dataProviders = dataProviders;
    }

    public Collection<DataProvider> getDataProviders() {
        return new TreeSet<>(dataProviders);
    }

    public DataProvider getDataProvider(String dataProviderName) {
        return dataProviders.stream().filter(dp -> dp.getClass().getSimpleName().equals(dataProviderName)).findAny().orElseThrow(RuntimeException::new);
    }

}
