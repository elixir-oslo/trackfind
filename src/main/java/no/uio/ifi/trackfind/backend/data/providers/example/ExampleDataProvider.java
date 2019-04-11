package no.uio.ifi.trackfind.backend.data.providers.example;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class ExampleDataProvider extends AbstractDataProvider {

    private static final String FETCH_URL = "https://hyperbrowser.uio.no/hb/static/hyperbrowser/files/trackfind/basic_example.json";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
//        try (InputStream inputStream = new URL(FETCH_URL).openStream();
//             InputStreamReader reader = new InputStreamReader(inputStream)) {
//            save(hubName, gson.fromJson(reader, Collection.class));
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
    }

}
