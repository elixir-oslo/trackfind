package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Source;
import no.uio.ifi.trackfind.backend.dao.SourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SourceRepository extends JpaRepository<Source, SourceId> {

    @Query(value = "SELECT s1.*\n" +
            "FROM source s1\n" +
            "       LEFT JOIN source s2 ON s1.id = s2.id\n" +
            "       LEFT JOIN source s3 ON s1.id = s3.id\n" +
            "WHERE s1.repository = :repository\n" +
            "GROUP BY s1.id, s1.raw_version, s1.curated_version\n" +
            "HAVING s1.raw_version = MAX(s2.raw_version)\n" +
            "   AND s1.curated_version = MAX(s3.curated_version)",
            nativeQuery = true)
    Collection<Source> findByRepositoryLatest(@Param("repository") String repository);

}
