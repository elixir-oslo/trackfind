package no.uio.ifi.trackfind.backend.data.providers.ihec;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.lucene.index.IndexWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Fetches data from <a href="http://epigenomesportal.ca/ihec/grid.html/">IHEC</a>.
 * Includes: CEEHRC, Blueprint, ENCODE, NIH Roadmap, DEEP, AMED-CREST, KNIH, GIS and some other institutions.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class IHECDataProvider extends AbstractDataProvider {

    private static final String RELEASES_URL = "http://epigenomesportal.ca/cgi-bin/api/getReleases.py";
    private static final String FETCH_URL = "http://epigenomesportal.ca/cgi-bin/api/getDataHub.py?data_release_id=";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(IndexWriter indexWriter) throws Exception {
        log.info("Collecting releases...");
        Collection<Release> releases;
        try (InputStream inputStream = new URL(RELEASES_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            releases = gson.fromJson(reader, new TypeToken<Collection<Release>>() {
            }.getType());
        }
        if (CollectionUtils.isEmpty(releases)) {
            return;
        }
        int size = releases.size();
        log.info(size + " releases collected.");
        Set<Integer> releaseIds = releases.stream().sorted().map(Release::getId).collect(Collectors.toSet());
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for (int releaseId : releaseIds) {
            executorService.submit(() -> {
                try (InputStream inputStream = new URL(FETCH_URL + releaseId).openStream();
                     InputStreamReader reader = new InputStreamReader(inputStream)) {
                    Map grid = gson.fromJson(reader, Map.class);
                    Map datasetsMap = (Map) grid.get("datasets");
                    Collection<Map> datasets = datasetsMap.values();
                    Map samplesMap = (Map) grid.get("samples");
                    Object hubDescription = grid.get("hub_description");
                    for (Map<String, Object> dataset : datasets) {
                        String sampleId = String.valueOf(dataset.get("sample_id"));
                        Object sample = samplesMap.get(sampleId);
                        dataset.put("sample_data", sample);
                        dataset.put("hub_description", hubDescription);
                        Map<String, Collection<Map<String, String>>> browser = (Map<String, Collection<Map<String, String>>>) dataset.get(DATA_URL_ATTRIBUTE);
                        Map<String, Collection<String>> browserToStore = new HashMap<>();
                        for (String dataType : browser.keySet()) {
                            Collection<Map<String, String>> bigDataUrls = browser.get(dataType);
                            for (Map<String, String> bigDataUrl : bigDataUrls) {
                                browserToStore.computeIfAbsent(dataType, k -> new HashSet<>()).add(bigDataUrl.get("big_data_url"));
                            }
                        }
                        dataset.put(DATA_URL_ATTRIBUTE, browserToStore);
                    }
                    postProcessDatasets(datasets);
                    indexWriter.addDocuments(datasets.stream().map(this::convertDatasetToDocument).collect(Collectors.toSet()));
                    log.info("Release " + releaseId + " processed.");
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }

}
