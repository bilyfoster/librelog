package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.TrackType;
import com.onelpro.librelog.models.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Track entity.
 */
@Repository
public interface TrackRepository extends JpaRepository<Track, UUID> {

	/**
	 * Find all tracks for a station.
	 */
	List<Track> findByStationId(UUID stationId);

	/**
	 * Find tracks by type.
	 */
	List<Track> findByType(TrackType type);

	/**
	 * Find tracks by station and type.
	 */
	List<Track> findByStationIdAndType(UUID stationId, TrackType type);

	/**
	 * Find tracks by title containing search term (case-insensitive).
	 */
	List<Track> findByTitleContainingIgnoreCase(String title);

	/**
	 * Find tracks by artist containing search term (case-insensitive).
	 */
	List<Track> findByArtistContainingIgnoreCase(String artist);

	/**
	 * Find tracks by genre.
	 */
	List<Track> findByGenreIgnoreCase(String genre);

	/**
	 * Search tracks by title or artist.
	 */
	@Query("SELECT t FROM Track t WHERE " +
		   "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
		   "LOWER(t.artist) LIKE LOWER(CONCAT('%', :search, '%'))")
	List<Track> searchByTitleOrArtist(@Param("search") String search);

	/**
	 * Find track by LibreTime ID.
	 */
	Optional<Track> findByLibretimeId(String libretimeId);

	/**
	 * Find tracks by station with pagination.
	 */
	Page<Track> findByStationId(UUID stationId, Pageable pageable);

	/**
	 * Count tracks by type for a station.
	 */
	long countByStationIdAndType(UUID stationId, TrackType type);

}
