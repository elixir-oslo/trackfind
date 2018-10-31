package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
@Table(name = "mappings")
@Data
public class Mapping {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "static", nullable = false)
    private boolean staticMapping;

    @Column(name = "map_from", nullable = false)
    private String from;

    @Column(name = "map_to", nullable = false)
    private String to;

}
