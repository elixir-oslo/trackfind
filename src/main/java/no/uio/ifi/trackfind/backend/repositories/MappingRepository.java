package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, Long> {

    Collection<Mapping> findByRepositoryAndHub(String repository, String hub);

}
