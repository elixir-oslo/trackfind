package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Hub;
import no.uio.ifi.trackfind.backend.dao.HubId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface HubRepository extends JpaRepository<Hub, HubId> {

    Collection<Hub> findByRepository(String repository);

}
