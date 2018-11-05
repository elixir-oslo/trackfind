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
            "ORDER BY raw_version DESC, curated_version DESC, standard_version DESC\n" +
            "LIMIT 1",
            nativeQuery = true)
    Dataset findByIdLatest(@Param("id") Long id);

}
