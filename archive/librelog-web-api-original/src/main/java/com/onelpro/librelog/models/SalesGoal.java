package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.PeriodType;
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
 * Entity representing a sales goal in the system.
 */
@Entity
@Table(name = "sales_goals", indexes = {
        @Index(name = "ix_sales_goals_id", columnList = "id"),
        @Index(name = "ix_sales_goals_sales_rep_id", columnList = "sales_rep_id"),
        @Index(name = "ix_sales_goals_period", columnList = "period"),
        @Index(name = "ix_sales_goals_target_date", columnList = "target_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id", nullable = false)
    private SalesRep salesRep;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, columnDefinition = "periodtype")
    private PeriodType period;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "goal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal goalAmount;

    @Column(name = "actual_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

