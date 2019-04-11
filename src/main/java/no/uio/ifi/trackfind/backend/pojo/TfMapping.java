package no.uio.ifi.trackfind.backend.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "tf_mappings")
@Data
@NoArgsConstructor
public class TfMapping {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hub_id", referencedColumnName = "id")
    private TfHub hub;

    @Column(name = "map_from", nullable = false)
    private String from;

    @Column(name = "map_to", nullable = false)
    private String to;

    @Column(name = "static", nullable = false)
    private boolean staticMapping;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id")
    private TfVersion version;

}
