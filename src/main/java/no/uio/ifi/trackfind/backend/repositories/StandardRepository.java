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
            "WHERE id = :id AND raw_version = :raw_version AND curated_version = :curated_version\n" +
            "ORDER BY raw_version DESC, curated_version DESC, standard_version DESC\n" +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Standard> findByIdAndRawVersionAndCuratedVersionLatest(@Param("id") Long id,
                                                                    @Param("raw_version") Long rawVersion,
                                                                    @Param("curated_version") Long curatedVersion);

}
