package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tf_hubs")
@Data
@EqualsAndHashCode(of = {"id", "repository", "name"})
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class TfHub implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(name = "repository", nullable = false)
    private String repository;

    @NonNull
    @Column(name = "name", nullable = false)
    private String name;

}
