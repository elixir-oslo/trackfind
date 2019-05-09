package no.uio.ifi.trackfind.backend.pojo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.HashMap;
import java.util.Map;

public class SearchResult {


    private Map<String, Map> content = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Map> getContent() {
        return content;
    }

}
