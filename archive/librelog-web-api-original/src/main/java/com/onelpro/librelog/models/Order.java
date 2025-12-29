package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.ApprovalStatus;
import com.onelpro.librelog.enums.OrderStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing an order in the system.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "ix_orders_id", columnList = "id"),
        @Index(name = "ix_orders_order_number", columnList = "order_number", unique = true),
        @Index(name = "ix_orders_campaign_id", columnList = "campaign_id"),
        @Index(name = "ix_orders_advertiser_id", columnList = "advertiser_id"),
        @Index(name = "ix_orders_agency_id", columnList = "agency_id"),
        @Index(name = "ix_orders_sales_rep_id", columnList = "sales_rep_id"),
        @Index(name = "ix_orders_start_date", columnList = "start_date"),
        @Index(name = "ix_orders_end_date", columnList = "end_date"),
        @Index(name = "ix_orders_status", columnList = "status"),
        @Index(name = "ix_orders_sales_team_id", columnList = "sales_team_id"),
        @Index(name = "ix_orders_sales_office_id", columnList = "sales_office_id"),
        @Index(name = "ix_orders_sales_region_id", columnList = "sales_region_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 50, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", nullable = false)
    private Advertiser advertiser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id")
    private SalesRep salesRep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_team_id")
    private SalesTeam salesTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_office_id")
    private SalesOffice salesOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_region_id")
    private SalesRegion salesRegion;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "spot_lengths", columnDefinition = "JSONB")
    private String spotLengths;

    @Column(name = "total_spots")
    @Builder.Default
    private Integer totalSpots = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, columnDefinition = "ratetype")
    @Builder.Default
    private RateType rateType = RateType.ROS;

    @Column(name = "rates", columnDefinition = "JSONB")
    private String rates;

    @Column(name = "total_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "orderstatus")
    @Builder.Default
    private OrderStatus status = OrderStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, columnDefinition = "approvalstatus")
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

