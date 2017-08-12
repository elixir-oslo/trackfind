package no.uio.ifi.trackfind.backend.data.providers.ihec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * Fetches data from IHEC (http://epigenomesportal.ca/ihec/grid.html/).
 * Includes: CEEHRC, Blueprint, ENCODE, NIH Roadmap, DEEP, AMED-CREST, KNIH, GIS and some other institutions.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class IHECDataProvider extends AbstractDataProvider {

    private static final String RELEASES_URL = "http://epigenomesportal.ca//cgi-bin/api/getReleases.py";
    private static final String FETCH_URL = "http://epigenomesportal.ca/cgi-bin/api/getDataHub.py?data_release_id=";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATASETS = "datasets";
    private static final String HUB_DESCRIPTION = "hub_description";
    private static final String SAMPLES = "samples";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> result = new HashSet<>();
        Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
        log.info("Collecting releases...");
        Collection<Release> releases;
        try (InputStreamReader reader = new InputStreamReader(new URL(RELEASES_URL).openStream())) {
            releases = gson.fromJson(reader, new TypeToken<Collection<Release>>() {
            }.getType());
        }
        if (CollectionUtils.isEmpty(releases)) {
            return result;
        }
        log.info(releases.size() + " releases collected.");
        releases.stream().map(Release::getId).forEach(releaseId -> {
            log.info("Processing release: " + releaseId);
            try (InputStreamReader reader = new InputStreamReader(new URL(FETCH_URL + releaseId).openStream())) {
                Map grid = gson.fromJson(reader, Map.class);
                Map datasetsMap = (Map) grid.get(DATASETS);
                Collection<Map> datasets = datasetsMap.values();
                Map samplesMap = (Map) grid.get(SAMPLES);
                Object hubDescription = grid.get(HUB_DESCRIPTION);
                for (Map<String, Object> dataset : datasets) {
                    String sampleId = String.valueOf(dataset.get("sample_id"));
                    Object sample = samplesMap.get(sampleId);
                    dataset.put("sample_data", sample);
                    dataset.put("hub_description", hubDescription);
                }
                result.addAll(datasets);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
        return result;
    }

    /**
     * Inner class for deserialization of Release data from IHEC (using Gson).
     */
    @Data
    private class Release {

        @SerializedName("assembly")
        private String assembly;

        @SerializedName("id")
        private Integer id;

        @SerializedName("integration_date")
        private Date integrationDate;

        @SerializedName("publishing_group")
        private String publishingGroup;

        @SerializedName("release_policies_url")
        private String releasePoliciesUrl;

        @SerializedName("releasing_group")
        private String releasingGroup;

        @SerializedName("species")
        private String species;

        @SerializedName("taxon_id")
        private Integer taxonId;

    }


}
