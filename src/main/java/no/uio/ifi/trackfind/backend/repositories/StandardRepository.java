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

    @Query(value = "SELECT *\n" +
            "FROM standard\n" +
            "WHERE id = :id\n" +
            "GROUP BY id, raw_version, curated_version, standard_version\n" +
            "HAVING raw_version = MAX(raw_version)\n" +
            "   AND curated_version = MAX(curated_version)\n" +
            "   AND standard_version = MAX(standard_version)",
            nativeQuery = true)
    Optional<Standard> findByIdLatest(@Param("id") Long id);

}
