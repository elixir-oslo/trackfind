package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;

import java.io.Serializable;

@Data
public class SourceId implements Serializable {

    private Long id;

    private Long rawVersion;

    private Long curatedVersion;

}
