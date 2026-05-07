package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "schedule_day")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDay {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String status;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "pushed_by")
    private UUID pushedBy;

    @Version
    @Column(nullable = false)
    private Long version;

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
        if (status == null) status = "DRAFT";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
