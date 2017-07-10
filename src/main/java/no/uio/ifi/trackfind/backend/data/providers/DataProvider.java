package no.uio.ifi.trackfind.backend.data.providers;

import java.util.Collection;
import java.util.Map;

public interface DataProvider {

    Collection<Map> fetchData();

}
