package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.ApprovalStatus;
import com.onelpro.librelog.enums.RateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity representing a campaign in the system.
 */
@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "ix_campaigns_id", columnList = "id"),
        @Index(name = "ix_campaigns_advertiser", columnList = "advertiser"),
        @Index(name = "ix_campaigns_start_date", columnList = "start_date"),
        @Index(name = "ix_campaigns_end_date", columnList = "end_date"),
        @Index(name = "ix_campaigns_priority", columnList = "priority"),
        @Index(name = "ix_campaigns_active", columnList = "active"),
        @Index(name = "ix_campaigns_order_number", columnList = "order_number", unique = true),
        @Index(name = "ix_campaigns_contract_number", columnList = "contract_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "advertiser", nullable = false, length = 255)
    private String advertiser;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "order_number", length = 50, unique = true)
    private String orderNumber;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @Column(name = "insertion_order_url", columnDefinition = "TEXT")
    private String insertionOrderUrl;

    @Column(name = "spot_lengths", columnDefinition = "JSONB")
    private String spotLengths;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", columnDefinition = "ratetype")
    private RateType rateType;

    @Column(name = "rates", columnDefinition = "JSONB")
    private String rates;

    @Column(name = "scripts", columnDefinition = "JSONB")
    private String scripts;

    @Column(name = "copy_instructions", columnDefinition = "TEXT")
    private String copyInstructions;

    @Column(name = "traffic_restrictions", columnDefinition = "JSONB")
    private String trafficRestrictions;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", columnDefinition = "approvalstatus")
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.NOT_REQUIRED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

