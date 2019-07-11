package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import no.uio.ifi.trackfind.backend.operations.Operation;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "tf_versions")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = "objectTypes")
@NoArgsConstructor
@AllArgsConstructor
public class TfVersion implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "based_on")
    private TfVersion basedOn;

    @Column(name = "current", nullable = false)
    private Boolean current;

    @Column(name = "previous", nullable = false)
    private Boolean previous;

    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Operation operation;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "time", nullable = false)
    private Date time;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hub_id", referencedColumnName = "id")
    private TfHub hub;

    @OneToMany(mappedBy = "version", fetch = FetchType.EAGER)
    private Set<TfObjectType> objectTypes;

    @OneToMany(mappedBy = "version", fetch = FetchType.EAGER)
    private Set<TfScript> scripts;

}
