package no.uio.ifi.trackfind.backend.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public abstract class AdvancedDocument {

    @JsonProperty("ihec_data_portal")
    private Map<String, String> ihecDataPortal;

    @JsonProperty("experiment_attributes")
    private Map<String, String> experimentAttributes;

}
