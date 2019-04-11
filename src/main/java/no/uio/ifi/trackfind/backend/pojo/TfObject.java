package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tf_objects")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString
@NoArgsConstructor
public class TfObject implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hub_id", referencedColumnName = "id")
    private TfHub hub;

    @ManyToOne
    @JoinColumn(name = "object_type_id", referencedColumnName = "id")
    private TfObjectType objectType;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

}
