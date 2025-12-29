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

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a radio station in the system.
 */
@Entity
@Table(name = "stations", indexes = {
        @Index(name = "ix_stations_id", columnList = "id"),
        @Index(name = "ix_stations_call_letters", columnList = "call_letters", unique = true),
        @Index(name = "ix_stations_frequency", columnList = "frequency"),
        @Index(name = "ix_stations_market", columnList = "market"),
        @Index(name = "ix_stations_inventory_class", columnList = "inventory_class")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_letters", nullable = false, length = 10, unique = true)
    private String callLetters;

    @Column(name = "frequency", length = 20)
    private String frequency;

    @Column(name = "market", length = 255)
    private String market;

    @Column(name = "format", length = 100)
    private String format;

    @Column(name = "ownership", length = 255)
    private String ownership;

    @Column(name = "contacts", columnDefinition = "JSONB")
    private String contacts;

    @Column(name = "rates", columnDefinition = "JSONB")
    private String rates;

    @Column(name = "inventory_class", length = 50)
    private String inventoryClass;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

