package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

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

    @OneToMany(mappedBy = "hub")
    private Collection<TfVersion> versions;

    public Optional<TfVersion> getMaxVersion() {
        Collection<TfVersion> versions = getVersions();
        if (CollectionUtils.isEmpty(versions)) {
            return Optional.empty();
        }
        return Optional.ofNullable(Collections.max(getVersions(), Comparator.comparing(TfVersion::getVersion)));
    }

}
