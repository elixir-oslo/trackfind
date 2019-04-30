package no.uio.ifi.trackfind.backend.data.providers.ihec;

import com.google.common.collect.HashMultimap;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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

    private static final String RELEASES_URL = "https://epigenomesportal.ca/cgi-bin/api/getReleases.py";
    private static final String FETCH_URL = "https://epigenomesportal.ca/cgi-bin/api/getDataHub.py?data_release_id=";

    // Temp hack for IHEC
    private void disableSSL() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) throws Exception {
        disableSSL();
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
        log.info(size + " releases to process.");
        Set<Integer> releaseIds = releases.parallelStream().sorted().map(Release::getId).collect(Collectors.toSet());
        CountDownLatch countDownLatch = new CountDownLatch(size);
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        for (int releaseId : releaseIds) {
            executorService.submit(() -> {
                try (InputStream inputStream = new URL(FETCH_URL + releaseId).openStream();
                     InputStreamReader reader = new InputStreamReader(inputStream)) {
                    Map grid = gson.fromJson(reader, Map.class);
                    Map<String, Map> datasetsMap = MapUtils.getMap(grid, "datasets");
                    for (Map.Entry<String, Map> entry : datasetsMap.entrySet()) {
                        entry.getValue().put("tf_dataset_id", entry.getKey());
                    }
                    Collection<Map> datasets = datasetsMap.values();
                    for (Map dataset : datasets) {
                        mapToSave.put(hubName + "_dataset", gson.toJson(dataset));
                    }
                    Map<String, Map> samplesMap = MapUtils.getMap(grid, "samples");
                    for (Map.Entry<String, Map> entry : samplesMap.entrySet()) {
                        entry.getValue().put("tf_sample_id", entry.getKey());
                    }
                    Collection<Map> samples = samplesMap.values();
                    for (Map sample : samples) {
                        mapToSave.put(hubName + "_sample", gson.toJson(sample));
                    }
                    log.info("Release " + releaseId + " fetched.");
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        save(hubName, mapToSave.asMap());
        log.info(size + " releases stored.");
    }

}
