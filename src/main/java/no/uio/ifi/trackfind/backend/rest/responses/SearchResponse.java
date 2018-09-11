package no.uio.ifi.trackfind.backend.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;

@Data
public abstract class SearchResponse {

    @JsonProperty("d4bf2ced769e3cfa08124b73d4be2607a4d44ae4")
    private Collection<Section> sections;

}
