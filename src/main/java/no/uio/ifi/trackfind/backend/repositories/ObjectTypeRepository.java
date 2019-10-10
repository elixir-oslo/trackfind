package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObjectTypeRepository extends JpaRepository<TfObjectType, Long> {

    Optional<TfObjectType> findByVersionAndName(TfVersion tfVersion, String name);

}
