package no.uio.ifi.trackfind.backend.data.providers.trackhub;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.fairfiller.FairFiller;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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

    //    private static final String HUBS_URL = "https://www.trackhubregistry.org/api/info/trackhubs";
    private static final String HUBS_URL = "http://www-test.trackhubregistry.org/api/info/trackhubs";

    @Cacheable(value = "thr-hubs", key = "#root.method.name", sync = true)
    @SuppressWarnings("unchecked")
    @Override
    public Collection<TfHub> getAllTrackHubs() {
        try (InputStream inputStream = new URL(HUBS_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Collection<Map> hubs = gson.fromJson(reader, Collection.class);
            return hubs.stream().map(h -> {
                Collection<TfHub> internalHubs = new ArrayList<>();
                String name = String.valueOf(h.get("name"));
                Collection<Map> trackDBs = (Collection<Map>) h.get("trackdbs");
                for (Map trackDB : trackDBs) {
                    String uri = String.valueOf(trackDB.get("uri"));
                    String id = StringUtils.substringAfterLast(uri, "/");
                    internalHubs.add(new TfHub(getName(), String.format("%s (%s)", name, id), uri));
                }
                return internalHubs;
            }).flatMap(Collection::stream).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFetchURI(String hubName) {
        return hubRepository.findByRepositoryAndName(getName(), hubName).getUri();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
        String hubURI = getFetchURI(hubName);
        log.info("Fetch URL {}", hubURI);
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        try (InputStream inputStream = new URL(hubURI).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map<String, Object> hub = (Map<String, Object>) gson.fromJson(reader, Map.class);
            Map<String, Object> source = (Map<String, Object>) hub.get("_source");
            Collection<Map<String, Object>> data = (Collection<Map<String, Object>>) source.get("data");
            for (Map<String, Object> entry : data) {
                mapToSave.put("data", gson.toJson(entry));
            }
            Collection<Map<String, Object>> trackMaps = findTrackMaps((Map<String, Object>) source.get("configuration"));
            if (CollectionUtils.isNotEmpty(trackMaps)) {
                for (Map<String, Object> trackMap : trackMaps) {
                    mapToSave.put("configuration", gson.toJson(trackMap));
                }
            }
            hub = (Map<String, Object>) source.get("hub");
            Map<String, Object> fairData = (Map<String, Object>) hub.get("metaFairData");
            FairFiller fairFiller = new FairFiller();
            for (String key : fairData.keySet()) {
                Object value = fairData.get(key);
                if (value instanceof Collection) {
                    Collection collection = (Collection) value;
                    for (Object object : collection) {
                        fairFiller.fill((Map<String, Object>) object);
                        mapToSave.put(key, gson.toJson(object));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        save(hubName, mapToSave.asMap());
    }

    private Collection<Map<String, Object>> findTrackMaps(Map<String, Object> inputMap) {
        Collection<Map<String, Object>> trackMaps = new ArrayList<>();
        findTrackMapsRecursively(inputMap, trackMaps);
        return trackMaps;
    }

    @SuppressWarnings("unchecked")
    private void findTrackMapsRecursively(Map<String, Object> mapToTest, Collection<Map<String, Object>> trackMaps) {
        if (mapToTest.containsKey("track") && mapToTest.containsKey("bigDataUrl")) {
            trackMaps.add(mapToTest);
        }
        for (Object value : mapToTest.values()) {
            if (value instanceof Map) {
                findTrackMapsRecursively((Map<String, Object>) value, trackMaps);
            }
        }
    }

}
