package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleDayRepository extends JpaRepository<ScheduleDay, UUID> {
    Optional<ScheduleDay> findByStationIdAndDate(UUID stationId, LocalDate date);
}
