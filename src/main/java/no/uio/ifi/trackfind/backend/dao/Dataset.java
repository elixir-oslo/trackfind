package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "datasets")
@Immutable
@Data
@EqualsAndHashCode(of = {"id", "version"})
public class Dataset {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "hub", nullable = false)
    private String hub;

    @JsonRawValue
    @Column(name = "curated_content", nullable = false, columnDefinition = "jsonb")
    private String curatedContent;

    @JsonRawValue
    @Column(name = "standard_content", columnDefinition = "jsonb")
    private String standardContent;

    @JsonValue
    @JsonRawValue
    @Column(name = "fair_content", nullable = false, columnDefinition = "jsonb")
    private String fairContent;

    @Column(name = "version", nullable = false)
    private String version;

}
