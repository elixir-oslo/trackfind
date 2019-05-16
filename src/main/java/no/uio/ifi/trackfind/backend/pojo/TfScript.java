package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "tf_scripts")
@Data
@NoArgsConstructor
public class TfScript {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

    @Column(name = "index", nullable = false)
    private Long index;

    @Column(name = "script", nullable = false)
    private String script;

}
