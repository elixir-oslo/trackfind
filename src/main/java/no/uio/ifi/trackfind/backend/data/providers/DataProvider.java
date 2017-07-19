package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

public interface DataProvider {

    Collection<Map> fetchData();

    String getUrlFromDataset(Map dataset);

    void updateIndex();

    Map<String, Object> getMetamodelTree();

    Multimap<String, String> getMetamodelFlat();

    Collection<Map> search(String query);

}
