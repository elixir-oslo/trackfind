package no.uio.ifi.trackfind.backend.dao;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hubs")
@IdClass(HubId.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"repository", "hub"})
@ToString
public class Hub implements Serializable {

    @NonNull
    @Id
    @Column(name = "repository", nullable = false)
    private String repository;

    @NonNull
    @Id
    @Column(name = "hub", nullable = false)
    private String hub;

    @Column(name = "id_attribute", nullable = false)
    private String idAttribute;

}
