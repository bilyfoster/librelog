package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.VoiceTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VoiceTrack entity.
 */
@Repository
public interface VoiceTrackRepository extends JpaRepository<VoiceTrack, UUID> {

	/**
	 * Find all voice tracks for a station.
	 */
	List<VoiceTrack> findByStationId(UUID stationId);

	/**
	 * Find voice tracks by status.
	 */
	List<VoiceTrack> findByStatus(String status);

	/**
	 * Find voice tracks scheduled for a specific date.
	 */
	List<VoiceTrack> findByStationIdAndScheduledDate(UUID stationId, LocalDate scheduledDate);

	/**
	 * Find voice tracks by show name.
	 */
	List<VoiceTrack> findByShowNameContainingIgnoreCase(String showName);

	/**
	 * Find voice tracks by segment type.
	 */
	List<VoiceTrack> findBySegmentType(String segmentType);

	/**
	 * Find upcoming scheduled voice tracks.
	 */
	List<VoiceTrack> findByStationIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(UUID stationId, LocalDate date);

}
