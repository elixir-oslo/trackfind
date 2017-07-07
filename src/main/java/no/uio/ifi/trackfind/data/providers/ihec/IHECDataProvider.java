package no.uio.ifi.trackfind.data.providers.ihec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import no.uio.ifi.trackfind.data.providers.DataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Date;

@Component
public class IHECDataProvider implements DataProvider {

    private static final String RELEASES_URL = "http://epigenomesportal.ca//cgi-bin/api/getReleases.py";
    private static final String FETCH_URL = "http://epigenomesportal.ca/cgi-bin/api/getDataHub.py?data_release_id=";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public LinkedTreeMap fetchData() throws Exception {
        Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
        URL releasesUrl = new URL(RELEASES_URL);
        Release lastRelease;
        try (InputStreamReader reader = new InputStreamReader(releasesUrl.openStream())) {
            Collection<Release> releases = gson.fromJson(reader, new TypeToken<Collection<Release>>() {
            }.getType());
            lastRelease = releases.stream().sorted().findFirst().orElseThrow(RuntimeException::new);
        }
        Integer lastReleaseId = lastRelease.getId();
        URL fetchUrl = new URL(FETCH_URL + lastReleaseId);
        try (InputStreamReader reader = new InputStreamReader(fetchUrl.openStream())) {
            return gson.fromJson(reader, LinkedTreeMap.class);
        }
    }

    @Data
    private class Release implements Comparable<Release> {

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

        @Override
        public int compareTo(Release o) {
            return getIntegrationDate().compareTo(o.getIntegrationDate());
        }

    }


}
