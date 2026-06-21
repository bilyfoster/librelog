package com.onelpro.librelog.orders;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public static final String KIND_STANDARD = "STANDARD";
    public static final String KIND_FOUNDING_MEMBER = "FOUNDING_MEMBER";

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Inclusive end of the contract. Null means open-ended (typical for founding member until cancelled).
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Used for {@link #KIND_STANDARD} orders; founding orders use {@link #monthlySpotAllowance} as the spot cap. */
    @Column(name = "total_spots", nullable = false)
    private int totalSpots;

    @Builder.Default
    @Column(name = "order_kind", nullable = false)
    private String orderKind = KIND_STANDARD;

    /** Max spot rows allowed on this order (founding / subscription-style cap). */
    @Column(name = "monthly_spot_allowance")
    private Integer monthlySpotAllowance;

    /** Optional billing amount in US cents (e.g. 10000 = $100/mo). */
    @Column(name = "monthly_price_cents")
    private Integer monthlyPriceCents;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    /** Maximum number of spot rows allowed for this order (standard: totalSpots; founding: monthlySpotAllowance). */
    public static int spotCap(Order o) {
        if (o == null) return 0;
        if (KIND_FOUNDING_MEMBER.equals(o.getOrderKind())) {
            return o.getMonthlySpotAllowance() != null ? o.getMonthlySpotAllowance() : 0;
        }
        return o.getTotalSpots();
    }

    public boolean isFoundingMember() {
        return KIND_FOUNDING_MEMBER.equals(orderKind);
    }
}
