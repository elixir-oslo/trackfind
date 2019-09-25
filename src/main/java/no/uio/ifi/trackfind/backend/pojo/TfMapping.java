package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
@Table(name = "tf_mappings")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "orderNumber"})
@NoArgsConstructor
@AllArgsConstructor
public class TfMapping implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private Long orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_object_type_id", referencedColumnName = "id")
    private TfObjectType fromObjectType;

    @Column(name = "from_attribute")
    private String fromAttribute;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_object_type_id", referencedColumnName = "id")
    private TfObjectType toObjectType;

    @Column(name = "to_attribute")
    private String toAttribute;

    @Column(name = "script")
    private String script;

}
