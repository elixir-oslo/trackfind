package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tf_mappings")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TfMapping implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_object_type_id", referencedColumnName = "id")
    private TfObjectType fromObjectType;

    @Column(name = "from_attribute", nullable = false)
    private String fromAttribute;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_object_type_id", referencedColumnName = "id")
    private TfObjectType toObjectType;

    @Column(name = "to_attribute", nullable = false)
    private String toAttribute;

}
