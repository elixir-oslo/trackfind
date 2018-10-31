package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Collection;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    long countByRepository(String repository);

    Dataset findByIdAndVersion(Long id, String version);

    @Query(value = "SELECT *\n" +
            "FROM datasets\n" +
            "WHERE id = :id\n" +
            "GROUP BY id, repository, curated_content, standard_content, raw_version, curated_version, standard_version, version\n" +
            "HAVING raw_version = MAX(raw_version)\n" +
            "   AND curated_version = MAX(curated_version)\n" +
            "   AND standard_version = MAX(standard_version)",
            nativeQuery = true)
    Dataset findByIdLatest(@Param("id") Long id);

}
