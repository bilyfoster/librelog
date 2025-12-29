package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a daily log in the system.
 */
@Entity
@Table(name = "daily_logs", indexes = {
        @Index(name = "ix_daily_logs_id", columnList = "id"),
        @Index(name = "ix_daily_logs_date", columnList = "date"),
        @Index(name = "ix_daily_logs_published", columnList = "published"),
        @Index(name = "ix_daily_logs_locked", columnList = "locked")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "json_data", nullable = false, columnDefinition = "JSONB")
    private String jsonData;

    @Column(name = "published")
    private Boolean published;

    @Column(name = "locked")
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;

    @Column(name = "conflicts", columnDefinition = "JSONB")
    private String conflicts;

    @Column(name = "oversell_warnings", columnDefinition = "JSONB")
    private String oversellWarnings;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

