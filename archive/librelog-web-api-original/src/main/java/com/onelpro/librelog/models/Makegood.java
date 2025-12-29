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
 * Entity representing a makegood (replacement spot) in the system.
 */
@Entity
@Table(name = "makegoods", indexes = {
        @Index(name = "ix_makegoods_id", columnList = "id"),
        @Index(name = "ix_makegoods_original_spot_id", columnList = "original_spot_id"),
        @Index(name = "ix_makegoods_makegood_spot_id", columnList = "makegood_spot_id"),
        @Index(name = "ix_makegoods_campaign_id", columnList = "campaign_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Makegood {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_spot_id", nullable = false)
    private Spot originalSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makegood_spot_id", nullable = false)
    private Spot makegoodSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}

