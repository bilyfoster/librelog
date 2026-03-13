package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.SpotStatus;
import com.onelpro.librelog.models.Spot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Spot entity.
 */
@Repository
public interface SpotRepository extends JpaRepository<Spot, UUID> {

	/**
	 * Find all spots for a campaign.
	 */
	List<Spot> findByCampaignId(UUID campaignId);

	/**
	 * Find all spots for a station.
	 */
	List<Spot> findByStationId(UUID stationId);

	/**
	 * Find spots by status.
	 */
	List<Spot> findByStatus(SpotStatus status);

	/**
	 * Find spots scheduled for a specific date.
	 */
	List<Spot> findByStationIdAndScheduledDate(UUID stationId, LocalDate scheduledDate);

	/**
	 * Find spots within a date range.
	 */
	List<Spot> findByStationIdAndScheduledDateBetween(UUID stationId, LocalDate startDate, LocalDate endDate);

	/**
	 * Find spots by campaign and date range.
	 */
	List<Spot> findByCampaignIdAndScheduledDateBetween(UUID campaignId, LocalDate startDate, LocalDate endDate);

	/**
	 * Count spots by campaign and status.
	 */
	long countByCampaignIdAndStatus(UUID campaignId, SpotStatus status);

	/**
	 * Find spots by status and date range (for scheduling).
	 */
	@Query("SELECT s FROM Spot s WHERE s.station.id = :stationId " +
		   "AND s.scheduledDate = :date AND s.status IN :statuses " +
		   "ORDER BY s.scheduledTime")
	List<Spot> findByStationAndDateAndStatuses(
			@Param("stationId") UUID stationId,
			@Param("date") LocalDate date,
			@Param("statuses") List<SpotStatus> statuses);

	/**
	 * Find spots by daypart.
	 */
	List<Spot> findByStationIdAndScheduledDateAndDaypart(UUID stationId, LocalDate scheduledDate, String daypart);

	/**
	 * Find spots with pagination.
	 */
	Page<Spot> findByStationId(UUID stationId, Pageable pageable);

	/**
	 * Find spots by break name and date.
	 */
	List<Spot> findByStationIdAndScheduledDateAndBreakNameOrderByBreakPositionAsc(
			UUID stationId, LocalDate scheduledDate, String breakName);

}
