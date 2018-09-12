package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "mappings")
@IdClass(MappingId.class)
@Data
public class Mapping {

    @Id
    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "static", nullable = false)
    private Boolean staticMapping;

    @Column(name = "map_from", nullable = false)
    private String from;

    @Id
    @Column(name = "map_to", nullable = false)
    private String to;

}
