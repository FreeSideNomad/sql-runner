package com.ivamare.repository;

import com.ivamare.domain.QueryVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for QueryVersion entities. */
@Repository
public interface QueryVersionRepository extends JpaRepository<QueryVersion, String> {

  List<QueryVersion> findByQueryIdOrderByVersionDesc(String queryId);

  List<QueryVersion> findByQueryIdOrderByVersionAsc(String queryId);

  Optional<QueryVersion> findByQueryIdAndVersion(String queryId, Integer version);

  Optional<QueryVersion> findTopByQueryIdOrderByVersionDesc(String queryId);

  int countByQueryId(String queryId);
}
