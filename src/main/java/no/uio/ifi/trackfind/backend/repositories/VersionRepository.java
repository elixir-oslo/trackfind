package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionRepository extends JpaRepository<TfVersion, Long> {

}
