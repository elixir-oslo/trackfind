package no.uio.ifi.trackfind.backend.data.providers.trackhub;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class TrackHubRegistryDataProvider extends AbstractDataProvider {

    //    private static final String HUBS_URL = "https://www.trackhubregistry.org/api/info/trackhubs";
//    private static final String FETCH_URL = "https://hyperbrowser.uio.no/hb/static/hyperbrowser/files/trackfind/blueprint_hub.json";
    private static final String FETCH_URL = "http://www-test.trackhubregistry.org/api/search/all";

//    @SuppressWarnings("unchecked")
//    @Override
//    public Collection<TfHub> getAllTrackHubs() {
//        try (InputStream inputStream = new URL(HUBS_URL).openStream();
//             InputStreamReader reader = new InputStreamReader(inputStream)) {
//            Collection<Map> hubs = gson.fromJson(reader, Collection.class);
//            return hubs.stream().map(h -> new TfHub(getName(), String.valueOf(h.get("name")))).collect(Collectors.toSet());
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Collections.emptyList();
//        }
//    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        try (InputStream inputStream = new URL(FETCH_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Collection entries = gson.fromJson(reader, Collection.class);
            for (Object entry : entries) {
                Map<String, Object> map = (Map<String, Object>) entry;
                mapToSave.put("TrackHubRegistry_entry", gson.toJson(map.get("_source")));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        save(hubName, mapToSave.asMap());
    }

}
