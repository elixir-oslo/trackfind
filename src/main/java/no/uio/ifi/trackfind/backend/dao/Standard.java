package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(name = "standard")
@IdClass(StandardId.class)
@Data
@EqualsAndHashCode(of = {"id", "rawVersion", "curatedVersion", "standardVersion"})
public class Standard {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonRawValue
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Id
    @Column(name = "raw_version", nullable = false)
    private Long rawVersion;

    @Id
    @Column(name = "curated_version", nullable = false)
    private Long curatedVersion;

    @Id
    @Column(name = "standard_version", nullable = false)
    private Long standardVersion;

}
