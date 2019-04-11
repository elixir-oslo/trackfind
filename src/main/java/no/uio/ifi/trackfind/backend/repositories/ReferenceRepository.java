package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceRepository extends JpaRepository<TfReference, Long> {

}
