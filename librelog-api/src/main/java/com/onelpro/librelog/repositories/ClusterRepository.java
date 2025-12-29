package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Cluster entity operations.
 */
@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

	Optional<Cluster> findByName(String name);

	List<Cluster> findByOrganizationId(UUID organizationId);

}

