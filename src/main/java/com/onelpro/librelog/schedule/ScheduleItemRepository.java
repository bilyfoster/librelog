package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, UUID> {
    List<ScheduleItem> findByScheduleDayIdOrderByPositionAsc(UUID scheduleDayId);
    List<ScheduleItem> findBySpotIdIn(Collection<UUID> spotIds);
    void deleteByScheduleDayId(UUID scheduleDayId);
}
