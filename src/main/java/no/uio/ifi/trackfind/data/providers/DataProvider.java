package no.uio.ifi.trackfind.data.providers;

import java.util.Collection;
import java.util.Map;

public interface DataProvider {

    Collection<Map> fetchData();

}
