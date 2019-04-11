package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface MappingRepository extends JpaRepository<TfMapping, Long> {

    Collection<TfMapping> findByHubId(Long hubId);

}
