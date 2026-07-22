package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One row of the station's weekly clock assignment grid: on this ISO day of week
 * (1=Monday..7=Sunday), during this station-local minute window, run this clock.
 * New schedule days copy the matching weekday's rows into their per-day clock
 * schedule ({@link ScheduleDayClockSegment}); the per-day rows are the override.
 */
@Entity
@Table(name = "station_clock_grid")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClockGridRow {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    /** ISO day of week: 1 = Monday .. 7 = Sunday. */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private int position;

    @Column(name = "local_start_minutes", nullable = false)
    private int localStartMinutes;

    @Column(name = "local_end_minutes", nullable = false)
    private int localEndMinutes;

    @Column(name = "clock_template_id", nullable = false)
    private UUID clockTemplateId;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
