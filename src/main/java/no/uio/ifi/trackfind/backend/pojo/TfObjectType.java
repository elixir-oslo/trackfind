package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
@Table(name = "tf_object_types")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "name"})
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
