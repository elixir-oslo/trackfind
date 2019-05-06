package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.uio.ifi.trackfind.backend.operations.Operation;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

@Entity
@Table(name = "tf_versions")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = "objectTypes")
@NoArgsConstructor
public class TfVersion implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Operation operation;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "time", nullable = false)
    private Date time;

    @ManyToOne
    @JoinColumn(name = "hub_id", referencedColumnName = "id")
    private TfHub hub;

    @OneToMany(mappedBy = "version", fetch = FetchType.EAGER)
    private Collection<TfObjectType> objectTypes;

}
