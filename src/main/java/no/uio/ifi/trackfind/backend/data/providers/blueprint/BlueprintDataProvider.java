package no.uio.ifi.trackfind.backend.data.providers.blueprint;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Draft of Data Provider for Blueprint (TrackHubRegistry).
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class BlueprintDataProvider extends AbstractDataProvider {

    private static final String FETCH_URL = "https://raw.githubusercontent.com/elixir-no-nels/trackfind/master/blueprint.json";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData(String hubName) {
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        try (InputStream inputStream = new URL(FETCH_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map topMap = gson.fromJson(reader, Map.class);
            for (String category : Arrays.asList("studies", "experiments", "samples", "tracks")) {
                List objects = (List) topMap.get(category);
                for (Object object : objects) {
                    mapToSave.put(category, gson.toJson(object));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        save(hubName, mapToSave.asMap());
    }

}
