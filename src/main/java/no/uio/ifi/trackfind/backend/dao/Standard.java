package no.uio.ifi.trackfind.backend.dao;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

@Entity
@Table(name = "standard")
@Data
@EqualsAndHashCode(of = {"id", "version"})
public class Standard {

    @Id
    @Column(name = "id", nullable = false)
    private BigInteger id;

    @JsonRawValue
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "version", nullable = false)
    private Long version;

}
