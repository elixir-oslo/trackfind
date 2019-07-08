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
@ToString(exclude = "versions")
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

    @OneToMany(mappedBy = "hub", fetch = FetchType.EAGER)
    private Collection<TfVersion> versions;

    public Optional<TfVersion> getCurrentVersion() {
        if (CollectionUtils.isEmpty(versions)) {
            return Optional.empty();
        }
        return Optional.ofNullable(Collections.max(versions, Comparator.comparing(TfVersion::getVersion)));
    }

    public Optional<TfVersion> getPreviousVersion() {
        Optional<TfVersion> currentVersion = getCurrentVersion();
        if (!currentVersion.isPresent()) {
            return Optional.empty();
        }
        Long previousVersion = currentVersion.get().getVersion() - 1;
        return versions.stream().filter(v -> v.getVersion().equals(previousVersion)).findAny();
    }

}
