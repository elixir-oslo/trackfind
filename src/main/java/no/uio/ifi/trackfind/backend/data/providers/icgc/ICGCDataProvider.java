package no.uio.ifi.trackfind.backend.data.providers.icgc;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches data from ICGC (http://docs.icgc.org/portal/api/).
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class ICGCDataProvider extends AbstractDataProvider { // TODO: fetch more data

    private static final String DONORS = "https://dcc.icgc.org/api/v1/donors?";
    private static final String SUBMIT = "https://dcc.icgc.org/api/v1/download/submit?filters={%22donor%22:{%22id%22:{%22is%22:[%22__DONOR_ID__%22]}}}&info=[{%22key%22:%22__DATA_TYPE__%22,%22value%22:%22TSV%22}]";
    private static final String DOWNLOAD = "https://dcc.icgc.org/api/v1/download/";
    private static final String AVAILABLE_DATA_TYPES = "availableDataTypes";
    private static final int DONORS_PER_PAGE = 100;

    private final Gson gson;

    @Autowired
    public ICGCDataProvider(Gson gson) {
        this.gson = gson;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> result = new HashSet<>();
        log.info("Fetching donors...");
        long pages;
        try (InputStreamReader reader = new InputStreamReader(new URL(DONORS + "size=" + DONORS_PER_PAGE).openStream())) {
            Donors donors = gson.fromJson(reader, Donors.class);
            pages = donors.getPagination().getPages();
        }
        if (pages == 0) {
            return result;
        }
        log.info(pages * DONORS_PER_PAGE + " donors available.");
        log.info("Pages: " + pages);
        for (int i = 0; i < pages; i++) {
            log.info("Processing page: " + i);
            try (InputStreamReader reader = new InputStreamReader(new URL(DONORS +
                    "from=" + i * DONORS_PER_PAGE +
                    "&size=" + DONORS_PER_PAGE).openStream())) {
                Donors donors = gson.fromJson(reader, Donors.class);
                result.addAll(donors.getHits());
            }
        }
        for (Map dataset : result) {
            Map<String, Collection<String>> browser = new HashMap<>();
            String donorId = (String) dataset.get("id");
            Collection<String> availableDataTypes = (Collection<String>) dataset.get(AVAILABLE_DATA_TYPES);
            for (String dataType : availableDataTypes) {
                browser.computeIfAbsent(dataType, k -> new HashSet<>()).add(SUBMIT.replace("__DONOR_ID__", donorId).replace("__DATA_TYPE__", dataType));
            }
            dataset.put(BROWSER, browser);
            dataset.remove(AVAILABLE_DATA_TYPES);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getUrlsFromDataset(String query, Map dataset) {
        Collection<String> submitURLs = super.getUrlsFromDataset(query, dataset);
        return submitURLs.stream().map(submitURL -> {
            try (InputStreamReader reader = new InputStreamReader(new URL(submitURL).openStream())) {
                Download download = gson.fromJson(reader, Download.class);
                return DOWNLOAD + download.getDownloadId();
            } catch (IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

}
