package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Collection;

@Repository
public interface SourceRepository extends JpaRepository<Source, BigInteger> {

    @Query(value = "SELECT *\n" +
            "FROM source\n" +
            "WHERE repository = :repository\n" +
            "GROUP BY id, repository, content\n" +
            "HAVING raw_version = MAX(raw_version)\n" +
            "   AND curated_version = MAX(curated_version)",
            nativeQuery = true)
    Collection<Source> findByRepositoryLatest(String repository);

}
