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
 * Entity representing a break structure in the system.
 */
@Entity
@Table(name = "break_structures", indexes = {
        @Index(name = "ix_break_structures_id", columnList = "id"),
        @Index(name = "ix_break_structures_name", columnList = "name"),
        @Index(name = "ix_break_structures_hour", columnList = "hour"),
        @Index(name = "ix_break_structures_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "hour", nullable = false)
    private Integer hour;

    @Column(name = "break_positions", columnDefinition = "JSONB")
    private String breakPositions;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

