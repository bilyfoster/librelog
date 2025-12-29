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
import java.util.UUID;

/**
 * Entity representing a log revision in the system.
 */
@Entity
@Table(name = "log_revisions", indexes = {
        @Index(name = "ix_log_revisions_id", columnList = "id"),
        @Index(name = "ix_log_revisions_log_id", columnList = "log_id"),
        @Index(name = "ix_log_revisions_revision_number", columnList = "revision_number"),
        @Index(name = "ix_log_revisions_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private DailyLog log;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "json_data", nullable = false, columnDefinition = "JSONB")
    private String jsonData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_at")
    private Instant createdAt;
}

