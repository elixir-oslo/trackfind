package no.uio.ifi.trackfind.backend.data.providers.trackhub;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class TrackHubRegistryDataProvider extends AbstractDataProvider {

    private static final String HUBS_URL = "https://www.trackhubregistry.org/api/info/trackhubs";
    private static final String FETCH_URL = "https://hyperbrowser.uio.no/hb/static/hyperbrowser/files/trackfind/blueprint_hub.json";

    @SuppressWarnings("unchecked")
    @Override
    public Collection<TfHub> getAllTrackHubs() {
        try (InputStream inputStream = new URL(HUBS_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Collection<Map> hubs = gson.fromJson(reader, Collection.class);
            return hubs.stream().map(h -> new TfHub(getName(), String.valueOf(h.get("name")))).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
//        try (InputStream inputStream = new URL(FETCH_URL).openStream();
//             InputStreamReader reader = new InputStreamReader(inputStream)) {
//            Map document = gson.fromJson(reader, Map.class);
//            Map hitsMap = MapUtils.getMap(document, "hits");
//            Collection<Map> hits = (Collection<Map>) hitsMap.get("hits");
//            Collection<Map> datasets = new HashSet<>();
//            for (Map hit : hits) {
//                Map source = MapUtils.getMap(hit, "_source");
//                Collection<Map> data = (Collection<Map>) source.get("data");
//                Map allMembers = MapUtils.getMap(MapUtils.getMap(MapUtils.getMap(source, "configuration"), "bp"), "members");
//                Map signalMembers = MapUtils.getMap(MapUtils.getMap(allMembers, "signal"), "members");
//                Map regionMembers = MapUtils.getMap(MapUtils.getMap(allMembers, "region"), "members");
//                for (Map dataset : data) {
//                    String id = MapUtils.getString(dataset, "id");
//                    if (signalMembers.containsKey(id)) {
//                        dataset.put("signal", signalMembers.get(id));
//                    }
//                    if (regionMembers.containsKey(id)) {
//                        dataset.put("region", regionMembers.get(id));
//                    }
//                    datasets.add(dataset);
//                }
//            }
//            save(hubName, datasets);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
    }

}
