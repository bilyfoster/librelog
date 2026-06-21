package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleDayClockSegmentRepository extends JpaRepository<ScheduleDayClockSegment, UUID> {

    List<ScheduleDayClockSegment> findByScheduleDayIdOrderByPositionAsc(UUID scheduleDayId);

    void deleteByScheduleDayId(UUID scheduleDayId);
}
