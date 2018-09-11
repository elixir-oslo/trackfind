package no.uio.ifi.trackfind.backend.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public abstract class BasicDocument {

    @JsonProperty("data_type")
    private Map<String, String> dataType;

    @JsonProperty("uri")
    private Map<String, String> uri;

}
