package no.uio.ifi.trackfind.backend.data.providers.trackhub;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Draft of Data Provider for TrackHubRegistry.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
@Transactional
public class TrackHubRegistryDataProvider extends AbstractDataProvider {

    private static final String HUBS_URL = "https://www.trackhubregistry.org/api/info/trackhubs";

    @Cacheable(value = "thr-hubs", key = "#root.method.name", sync = true)
    @SuppressWarnings("unchecked")
    @Override
    public Collection<TfHub> getAllTrackHubs() {
        try (InputStream inputStream = new URL(HUBS_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Collection<Map> hubs = gson.fromJson(reader, Collection.class);
            return hubs.stream().map(h -> new TfHub(getName(), String.valueOf(h.get("name"))))
                    .collect(Collectors.toSet());
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
        Collection<String> fetchURLs = getFetchURLs(hubName);
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        for (String fetchURL : fetchURLs) {
            log.info("Fetch URL {}", fetchURL);
            try (InputStream inputStream = new URL(fetchURL).openStream();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                Map<String, Object> hub = (Map<String, Object>) gson.fromJson(reader, Map.class);
                Map<String, Object> source = (Map<String, Object>) hub.get("_source");
                Collection<Map<String, Object>> data = (Collection<Map<String, Object>>) source.get("data");
                for (Map<String, Object> entry : data) {
                    mapToSave.put("data", gson.toJson(entry));
                }
                Map<String, Object> dataMembersMap = findDataMembersMap((Map<String, Object>) source.get("configuration"));
                if (dataMembersMap != null) {
                    for (Object value : dataMembersMap.values()) {
                        mapToSave.put("configuration", gson.toJson(value));
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        save(hubName, mapToSave.asMap());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findDataMembersMap(Map<String, Object> inputMap) {
        if (inputMap.containsKey("members")) {
            Map<String, Object> members = (Map<String, Object>) inputMap.get("members");
            Collection<Object> memberValues = members.values();
            for (Object member : memberValues) {
                Map<String, Object> memberMap = (Map<String, Object>) member;
                if (memberMap.containsKey("track") && memberMap.containsKey("bigDataUrl")) {
                    return members;
                }
            }
        }
        for (Object value : inputMap.values()) {
            if (value instanceof Map) {
                Map<String, Object> dataMembersMap = findDataMembersMap((Map<String, Object>) value);
                if (dataMembersMap != null) {
                    return dataMembersMap;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getFetchURLs(String hubName) {
        Collection<String> fetchURLs = new HashSet<>();
        try (InputStream inputStream = new URL(HUBS_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Collection<Map> hubs = gson.fromJson(reader, Collection.class);
            Optional<Map> hubOptional = hubs.stream().filter(h -> String.valueOf(h.get("name")).equalsIgnoreCase(hubName)).findAny();
            if (!hubOptional.isPresent()) {
                log.warn("No hubs found!");
                return Collections.emptyList();
            }
            Map hub = hubOptional.get();
            Collection<Map> trackDBs = (Collection<Map>) hub.get("trackdbs");
            for (Map trackDB : trackDBs) {
                fetchURLs.add(String.valueOf(trackDB.get("uri")));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
        return fetchURLs;
    }

}
