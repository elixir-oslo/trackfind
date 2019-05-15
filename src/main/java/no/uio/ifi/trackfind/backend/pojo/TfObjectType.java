package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;

@Entity
@Table(name = "tf_object_types")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = "references")
@NoArgsConstructor
public class TfObjectType implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

    @OneToMany(mappedBy = "fromObjectType")
    private Collection<TfReference> references;

    @ManyToMany(mappedBy = "objectTypes")
    private Collection<TfScript> scripts;

}
