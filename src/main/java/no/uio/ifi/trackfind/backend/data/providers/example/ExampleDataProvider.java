package no.uio.ifi.trackfind.backend.data.providers.example;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
@Transactional
public class ExampleDataProvider extends AbstractDataProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFetchURI(String hubName) {
        return "https://raw.githubusercontent.com/fairtracks/fairtracks_standard/v1/1.0/json/examples/fairtracks.example.json";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData(String hubName) {
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        try (InputStream inputStream = new URL(getFetchURI(hubName)).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map topMap = gson.fromJson(reader, Map.class);
            for (String category : Arrays.asList("studies", "experiments", "samples", "tracks")) {
                List objects = (List) topMap.get(category);
                for (Object object : objects) {
                    mapToSave.put(category, gson.toJson(object));
                }
            }
            mapToSave.put("doc_info", gson.toJson(topMap.get("doc_info")));
            mapToSave.put("collection_info", gson.toJson(topMap.get("collection_info")));
            // fake category (for test)
            List objects = (List) topMap.get("samples");
            for (Object object : objects) {
                mapToSave.put("non_standard_samples", gson.toJson(object));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        save(hubName, mapToSave.asMap());
    }

}
