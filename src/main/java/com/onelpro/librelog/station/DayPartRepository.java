package com.onelpro.librelog.station;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayPartRepository extends JpaRepository<DayPart, UUID> {

    List<DayPart> findByStationIdOrderBySortOrderAscNameAsc(UUID stationId);

    Optional<DayPart> findByStationIdAndNameIgnoreCase(UUID stationId, String name);
}
