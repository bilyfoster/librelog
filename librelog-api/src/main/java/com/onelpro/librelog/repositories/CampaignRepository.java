package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.CampaignStatus;
import com.onelpro.librelog.models.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Campaign entity.
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

	/**
	 * Find all campaigns for a station.
	 */
	List<Campaign> findByStationId(UUID stationId);

	/**
	 * Find all campaigns for a station with pagination.
	 */
	Page<Campaign> findByStationId(UUID stationId, Pageable pageable);

	/**
	 * Find campaigns by status.
	 */
	List<Campaign> findByStatus(CampaignStatus status);

	/**
	 * Find campaigns by station and status.
	 */
	List<Campaign> findByStationIdAndStatus(UUID stationId, CampaignStatus status);

	/**
	 * Find active campaigns within a date range.
	 */
	@Query("SELECT c FROM Campaign c WHERE c.station.id = :stationId " +
		   "AND c.status IN :statuses " +
		   "AND c.startDate <= :endDate AND c.endDate >= :startDate")
	List<Campaign> findActiveCampaignsInDateRange(
			@Param("stationId") UUID stationId,
			@Param("statuses") List<CampaignStatus> statuses,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * Find campaigns by advertiser name (case-insensitive search).
	 */
	List<Campaign> findByAdvertiserNameContainingIgnoreCase(String advertiserName);

	/**
	 * Count campaigns by status for a station.
	 */
	long countByStationIdAndStatus(UUID stationId, CampaignStatus status);

	/**
	 * Count campaigns by status.
	 */
	long countByStatus(CampaignStatus status);

	/**
	 * Find campaign by ID with station details.
	 */
	@Query("SELECT c FROM Campaign c JOIN FETCH c.station WHERE c.id = :id")
	Optional<Campaign> findByIdWithStation(@Param("id") UUID id);

	/**
	 * Find top 5 campaigns by creation date.
	 */
	List<Campaign> findTop5ByOrderByCreatedAtDesc();

}
