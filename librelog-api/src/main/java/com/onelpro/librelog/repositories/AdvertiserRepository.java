package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Advertiser entity operations.
 */
@Repository
public interface AdvertiserRepository extends JpaRepository<Advertiser, UUID> {

	Optional<Advertiser> findByName(String name);

	List<Advertiser> findByAgencyId(UUID agencyId);

	List<Advertiser> findBySalesRepId(UUID salesRepId);

	List<Advertiser> findByIsActive(Boolean isActive);

}

