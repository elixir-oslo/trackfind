package no.uio.ifi.trackfind.data.providers;

import com.google.gson.internal.LinkedTreeMap;

public interface DataProvider {

    LinkedTreeMap fetchData() throws Exception;

}
