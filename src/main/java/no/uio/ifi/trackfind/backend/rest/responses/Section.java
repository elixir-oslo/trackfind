package no.uio.ifi.trackfind.backend.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public abstract class Section {

    @JsonProperty("Advanced")
    private AdvancedDocument advanced;

    @JsonProperty("Basic")
    private BasicDocument basic;

}
