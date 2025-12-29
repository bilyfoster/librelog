package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.AvailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for AvailType entity operations.
 */
@Repository
public interface AvailTypeRepository extends JpaRepository<AvailType, UUID> {

	Optional<AvailType> findByNameIgnoreCase(String name);

	List<AvailType> findByIsActiveTrue();

}

