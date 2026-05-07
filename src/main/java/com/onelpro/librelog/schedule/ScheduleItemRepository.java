package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, UUID> {
    List<ScheduleItem> findByScheduleDayIdOrderByPositionAsc(UUID scheduleDayId);
    void deleteByScheduleDayId(UUID scheduleDayId);
}
