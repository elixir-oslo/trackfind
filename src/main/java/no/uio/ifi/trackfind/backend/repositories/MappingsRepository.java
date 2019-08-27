package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfMapping;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MappingsRepository extends JpaRepository<TfMapping, Long> {

    Optional<TfMapping> findByVersionAndOrderNumber(TfVersion version, Long orderNumber);

}
