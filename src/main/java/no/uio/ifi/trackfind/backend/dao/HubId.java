package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;

import java.io.Serializable;

@Data
public class HubId implements Serializable {

    private String repository;

    private String hub;

}
