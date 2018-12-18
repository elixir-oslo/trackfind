package no.uio.ifi.trackfind.backend.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "hubs")
@IdClass(HubId.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"repository", "hub"})
public class Hub {

    @Id
    @Column(name = "repository", nullable = false)
    private String repository;

    @Id
    @Column(name = "hub", nullable = false)
    private String hub;

}
