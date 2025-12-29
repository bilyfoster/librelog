package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Entity representing an inventory slot in the system.
 */
@Entity
@Table(name = "inventory_slots", indexes = {
        @Index(name = "ix_inventory_slots_id", columnList = "id"),
        @Index(name = "ix_inventory_slots_date", columnList = "date"),
        @Index(name = "ix_inventory_slots_hour", columnList = "hour"),
        @Index(name = "ix_inventory_slots_sold_out", columnList = "sold_out")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "hour", nullable = false)
    private Integer hour;

    @Column(name = "break_position", length = 10)
    private String breakPosition;

    @Column(name = "daypart", length = 50)
    private String daypart;

    @Column(name = "available")
    @Builder.Default
    private Integer available = 3600;

    @Column(name = "booked")
    @Builder.Default
    private Integer booked = 0;

    @Column(name = "sold_out")
    @Builder.Default
    private Boolean soldOut = false;

    @Column(name = "revenue", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

