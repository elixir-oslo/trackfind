package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectRepository extends JpaRepository<TfObject, Long> {

}
