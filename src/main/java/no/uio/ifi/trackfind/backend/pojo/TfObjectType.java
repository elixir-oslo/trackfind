package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

    @OneToMany(mappedBy = "toObjectType", fetch = FetchType.EAGER)
    private Set<TfReference> references;

    @OneToMany(mappedBy = "toObjectType", fetch = FetchType.EAGER)
    private Set<TfMapping> mappings;

}
