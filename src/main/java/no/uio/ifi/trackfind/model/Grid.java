package no.uio.ifi.trackfind.model;

import lombok.Data;

import java.util.Map;

@Data
public class Grid {

    private HubDescription hubDescription;
    private Map<String, Dataset> datasets;
    private Map<String, Map<String, String>> samples;

}
