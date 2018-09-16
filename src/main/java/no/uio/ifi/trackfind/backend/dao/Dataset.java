package no.uio.ifi.trackfind.backend.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(name = "datasets")
@Data
@EqualsAndHashCode(of = "id")
public class Dataset {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "raw_dataset", nullable = false, columnDefinition = "jsonb")
    private String rawDataset;

    @Column(name = "basic_dataset", columnDefinition = "jsonb")
    private String basicDataset;

}
