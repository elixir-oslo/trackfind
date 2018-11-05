package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Standard;
import no.uio.ifi.trackfind.backend.dao.StandardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StandardRepository extends JpaRepository<Standard, StandardId> {

    @Query(value = "SELECT s1.*\n" +
            "FROM standard s1\n" +
            "LEFT JOIN standard s2 ON s1.id = s2.id\n" +
            "LEFT JOIN standard s3 ON s1.id = s3.id\n" +
            "LEFT JOIN standard s4 ON s1.id = s4.id\n" +
            "WHERE s1.id = :id\n" +
            "GROUP BY s1.id, s1.content, s1.raw_version, s1.curated_version, s1.standard_version\n" +
            "HAVING s1.raw_version = MAX(s2.raw_version)\n" +
            "   AND s1.curated_version = MAX(s3.curated_version)\n" +
            "   AND s1.standard_version = MAX(s4.standard_version)",
            nativeQuery = true)
    Optional<Standard> findByIdLatest(@Param("id") Long id);

}
