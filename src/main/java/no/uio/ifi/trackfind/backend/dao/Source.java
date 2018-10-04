package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
@Table(name = "source")
@Data
@EqualsAndHashCode(of = {"id", "rawVersion", "curatedVersion"})
public class Source {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_ids_generator")
    @SequenceGenerator(name = "source_ids_generator", sequenceName = "source_ids_sequence")
    private BigInteger id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @JsonRawValue
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "raw_version", nullable = false)
    private Long rawVersion;

    @Column(name = "curated_version", nullable = false)
    private Long curatedVersion;

}
