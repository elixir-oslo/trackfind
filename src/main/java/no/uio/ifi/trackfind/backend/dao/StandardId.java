package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;

import java.io.Serializable;

@Data
public class StandardId implements Serializable {

    private Long id;

    private Long standardVersion;

}
