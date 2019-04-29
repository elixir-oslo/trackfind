package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.pojo.TfHub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface HubRepository extends JpaRepository<TfHub, Long> {

    Collection<TfHub> findByRepository(String repository);

    TfHub findByRepositoryAndName(String repository, String name);

}
