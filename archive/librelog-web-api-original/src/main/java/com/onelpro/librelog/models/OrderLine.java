package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.RevenueType;
import com.onelpro.librelog.enums.SelloutClass;
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
 * Entity representing an order line in the system.
 */
@Entity
@Table(name = "order_lines", indexes = {
        @Index(name = "ix_order_lines_order_id", columnList = "order_id"),
        @Index(name = "ix_order_lines_start_date", columnList = "start_date"),
        @Index(name = "ix_order_lines_end_date", columnList = "end_date"),
        @Index(name = "ix_order_lines_station", columnList = "station"),
        @Index(name = "ix_order_lines_revenue_type", columnList = "revenue_type"),
        @Index(name = "ix_order_lines_daypart", columnList = "daypart"),
        @Index(name = "ix_order_lines_priority_code", columnList = "priority_code"),
        @Index(name = "ix_order_lines_makegood_eligible", columnList = "makegood_eligible"),
        @Index(name = "ix_order_lines_guaranteed_position", columnList = "guaranteed_position"),
        @Index(name = "ix_order_lines_preemptible", columnList = "preemptible"),
        @Index(name = "ix_order_lines_product_code", columnList = "product_code"),
        @Index(name = "ix_order_lines_station_id", columnList = "station_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "station", length = 100)
    private String station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station stationEntity;

    @Column(name = "product", length = 255)
    private String product;

    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_type", columnDefinition = "revenuetype")
    private RevenueType revenueType;

    @Column(name = "length")
    private Integer length;

    @Column(name = "daypart", length = 50)
    private String daypart;

    @Column(name = "days_of_week", length = 7)
    private String daysOfWeek;

    @Column(name = "rate", precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(name = "rate_type", length = 50)
    private String rateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sellout_class", columnDefinition = "selloutclass")
    private SelloutClass selloutClass;

    @Column(name = "priority_code", length = 50)
    private String priorityCode;

    @Column(name = "spot_frequency")
    private Integer spotFrequency;

    @Column(name = "estimated_impressions")
    private Integer estimatedImpressions;

    @Column(name = "cpm", precision = 10, scale = 2)
    private BigDecimal cpm;

    @Column(name = "cpp", precision = 10, scale = 2)
    private BigDecimal cpp;

    @Column(name = "makegood_eligible")
    @Builder.Default
    private Boolean makegoodEligible = true;

    @Column(name = "guaranteed_position")
    @Builder.Default
    private Boolean guaranteedPosition = false;

    @Column(name = "preemptible")
    @Builder.Default
    private Boolean preemptible = true;

    @Column(name = "inventory_class", length = 100)
    private String inventoryClass;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "deal_points", columnDefinition = "TEXT")
    private String dealPoints;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "impressions_booked")
    private Integer impressionsBooked;

    @Column(name = "delivery_window", length = 100)
    private String deliveryWindow;

    @Column(name = "targeting_parameters", columnDefinition = "JSONB")
    private String targetingParameters;

    @Column(name = "companion_banners", columnDefinition = "JSONB")
    private String companionBanners;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

