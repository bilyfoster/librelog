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
 * Entity representing a rotation rule in the system.
 */
@Entity
@Table(name = "rotation_rules", indexes = {
        @Index(name = "ix_rotation_rules_id", columnList = "id"),
        @Index(name = "ix_rotation_rules_name", columnList = "name"),
        @Index(name = "ix_rotation_rules_daypart_id", columnList = "daypart_id"),
        @Index(name = "ix_rotation_rules_campaign_id", columnList = "campaign_id"),
        @Index(name = "ix_rotation_rules_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rotation_type", nullable = false, length = 20)
    @Builder.Default
    private String rotationType = "SEQUENTIAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daypart_id")
    private Daypart daypart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "min_separation")
    @Builder.Default
    private Integer minSeparation = 0;

    @Column(name = "max_per_hour")
    private Integer maxPerHour;

    @Column(name = "max_per_day")
    private Integer maxPerDay;

    @Column(name = "weights", columnDefinition = "JSONB")
    private String weights;

    @Column(name = "exclude_days", columnDefinition = "JSONB")
    private String excludeDays;

    @Column(name = "exclude_times", columnDefinition = "JSONB")
    private String excludeTimes;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

