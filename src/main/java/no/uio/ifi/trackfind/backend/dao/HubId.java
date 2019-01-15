package no.uio.ifi.trackfind.backend.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class HubId implements Serializable {

    private String repository;

    private String hub;

}


