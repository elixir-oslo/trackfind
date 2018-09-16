package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.dao.Dataset;

import java.util.Collection;
import java.util.Map;

public class TestDataProvider implements DataProvider, Comparable<DataProvider> {

    public static final String TEST_DATA_PROVIDER = "TEST";

    @Override
    public String getName() {
        return TEST_DATA_PROVIDER;
    }

    @Override
    public void crawlRemoteRepository() {

    }

    @Override
    public void applyMappings() {

    }

    @Override
    public Map<String, Object> getMetamodelTree() {
        return null;
    }

    @Override
    public Multimap<String, String> getMetamodelFlat() {
        return null;
    }

    @Override
    public Collection<Dataset> search(String query, int limit) {
        return null;
    }

    @Override
    public Map<String, Object> fetch(String datasetId, String version) {
        return null;
    }

    @Override
    public int compareTo(DataProvider o) {
        return 0;
    }

}
