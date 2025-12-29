package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.DaypartCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DaypartCategory entity operations.
 */
@Repository
public interface DaypartCategoryRepository extends JpaRepository<DaypartCategory, UUID> {

	Optional<DaypartCategory> findByName(String name);

}

