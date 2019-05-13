package no.uio.ifi.trackfind.backend.data.providers.example;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class ExampleDataProvider extends AbstractDataProvider {

    private static final String FETCH_URL = "https://raw.githubusercontent.com/fairtracks/fairtracks_standard/master/json/examples/fairtracks.example.json";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        try (InputStream inputStream = new URL(FETCH_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map topMap = gson.fromJson(reader, Map.class);
            mapToSave.put("studies_Example", gson.toJson(((List)topMap.get("studies")).get(0)));
            mapToSave.put("experiments_Example", gson.toJson(((List)topMap.get("experiments")).get(0)));
            mapToSave.put("samples_Example", gson.toJson(((List)topMap.get("samples")).get(0)));
            mapToSave.put("tracks_Example", gson.toJson(((List)topMap.get("tracks")).get(0)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        save(hubName, mapToSave.asMap());
    }

}
