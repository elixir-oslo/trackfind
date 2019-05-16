package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "tf_scripts")
@Data
@NoArgsConstructor
public class TfScript {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "index", nullable = false)
    private Long index;

    @ManyToMany
    @JoinTable(
            name = "tf_scripts_joining",
            joinColumns = {@JoinColumn(name = "script_id")},
            inverseJoinColumns = {@JoinColumn(name = "object_type_id")}
    )
    private Collection<TfObjectType> objectTypes;

    @Column(name = "script", nullable = false)
    private String script;

}
