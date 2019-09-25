package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
@Table(name = "tf_hubs")
@Data
@EqualsAndHashCode(of = {"repository", "name"})
@ToString(of = {"id", "repository", "name"})
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

    @NonNull
    @Column(name = "uri", nullable = false)
    private String uri;

    @OneToMany(mappedBy = "hub", fetch = FetchType.EAGER)
    private Set<TfVersion> versions;

    public Optional<TfVersion> getCurrentVersion() {
        if (CollectionUtils.isEmpty(versions)) {
            return Optional.empty();
        }
        return versions.stream().filter(v -> Boolean.TRUE.equals(v.getCurrent())).findAny();
    }

}
