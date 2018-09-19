package no.uio.ifi.trackfind.backend.repositories;

import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Collection;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, BigInteger> {

    long countByRepository(String repository);

    Collection<Dataset> findByRepositoryAndVersion(String repository, long version);

    Collection<Dataset> findByIdIn(Collection<BigInteger> ids);

    Dataset findByIdAndVersion(BigInteger id, long version);

}
