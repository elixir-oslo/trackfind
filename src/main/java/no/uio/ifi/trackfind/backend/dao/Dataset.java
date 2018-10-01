package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

@Entity
@Table(name = "datasets")
@Immutable
@Data
@EqualsAndHashCode(of = {"id", "version"})
public class Dataset {

    @Id
    @Column(name = "id", nullable = false)
    private BigInteger id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @JsonRawValue
    @Column(name = "curated_content", nullable = false, columnDefinition = "jsonb")
    private String curatedContent;

    @JsonRawValue
    @Column(name = "standard_content", columnDefinition = "jsonb")
    private String standardContent;

    @Column(name = "version", nullable = false)
    private String version;

}
