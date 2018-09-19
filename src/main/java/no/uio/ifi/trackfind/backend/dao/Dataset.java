package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
@Table(name = "datasets")
@Data
@EqualsAndHashCode(of = "id")
public class Dataset {

    @Id
    @GeneratedValue
    private BigInteger id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "version", nullable = false)
    private Long version;

    @JsonRawValue
    @Column(name = "raw_dataset", nullable = false, columnDefinition = "jsonb")
    private String rawDataset;

    @JsonRawValue
    @Column(name = "basic_dataset", columnDefinition = "jsonb")
    private String basicDataset;

}
