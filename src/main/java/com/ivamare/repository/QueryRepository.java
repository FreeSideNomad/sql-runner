package com.ivamare.repository;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for Query entities. */
@Repository
public interface QueryRepository extends JpaRepository<Query, String> {

  List<Query> findByIsActiveTrue();

  List<Query> findByIsActiveTrueOrderByNameAsc();

  List<Query> findByCategory(String category);

  List<Query> findByCategoryAndIsActiveTrue(String category);

  List<Query> findByQueryTypeAndIsActiveTrue(QueryType queryType);

  List<Query> findByConnectionNameAndIsActiveTrue(String connectionName);

  @org.springframework.data.jpa.repository.Query(
      "SELECT DISTINCT q.category FROM Query q WHERE q.isActive = true ORDER BY q.category")
  List<String> findDistinctCategories();

  boolean existsByNameAndIsActiveTrue(String name);

  long countByIsActiveTrue();
}
