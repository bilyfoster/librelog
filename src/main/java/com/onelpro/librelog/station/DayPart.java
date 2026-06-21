package com.onelpro.librelog.station;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "day_part")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayPart {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(nullable = false)
    private String name;

    /** Inclusive start, 0–1439 (station local minute-of-day). */
    @Column(name = "start_minutes", nullable = false)
    private int startMinutes;

    /** Exclusive end, 1–1440 (see {@link com.onelpro.librelog.time.TimeWindowUtil}). */
    @Column(name = "end_minutes", nullable = false)
    private int endMinutes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

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
