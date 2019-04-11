package no.uio.ifi.trackfind.backend.pojo;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

@Data
public class SearchResult {

    @JsonRawValue
    private String content;

}
