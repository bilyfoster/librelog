package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps a station-local time window on a calendar day to a clock template.
 * Used when applying the day's clock schedule to LibreTime show instances.
 */
@Entity
@Table(name = "schedule_day_clock_segment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDayClockSegment {

    @Id
    private UUID id;

    @Column(name = "schedule_day_id", nullable = false)
    private UUID scheduleDayId;

    @Column(nullable = false)
    private int position;

    @Column(name = "local_start_minutes", nullable = false)
    private int localStartMinutes;

    @Column(name = "local_end_minutes", nullable = false)
    private int localEndMinutes;

    @Column(name = "clock_template_id", nullable = false)
    private UUID clockTemplateId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
