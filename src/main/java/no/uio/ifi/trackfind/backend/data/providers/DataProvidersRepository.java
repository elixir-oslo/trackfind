package no.uio.ifi.trackfind.backend.data.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class DataProvidersRepository {

    public static final String JSON_KEY = "java_data_provider";

    private final Collection<DataProvider> dataProviders;

    @Autowired
    public DataProvidersRepository(Collection<DataProvider> dataProviders) {
        this.dataProviders = dataProviders;
    }

    public Optional<DataProvider> getDataProvider(String simpleName) {
        return dataProviders.stream().filter(dp -> dp.getClass().getSimpleName().equals(simpleName)).findAny();
    }

}
