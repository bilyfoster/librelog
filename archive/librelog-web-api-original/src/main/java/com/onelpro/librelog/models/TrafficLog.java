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
 * Entity representing a traffic log entry in the system.
 */
@Entity
@Table(name = "traffic_logs", indexes = {
        @Index(name = "ix_traffic_logs_id", columnList = "id"),
        @Index(name = "ix_traffic_logs_log_type", columnList = "log_type"),
        @Index(name = "ix_traffic_logs_log_id", columnList = "log_id"),
        @Index(name = "ix_traffic_logs_spot_id", columnList = "spot_id"),
        @Index(name = "ix_traffic_logs_order_id", columnList = "order_id"),
        @Index(name = "ix_traffic_logs_campaign_id", columnList = "campaign_id"),
        @Index(name = "ix_traffic_logs_copy_id", columnList = "copy_id"),
        @Index(name = "ix_traffic_logs_user_id", columnList = "user_id"),
        @Index(name = "ix_traffic_logs_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "log_type", nullable = false, length = 50)
    private String logType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id")
    private DailyLog log;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private Copy copy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "meta_data", columnDefinition = "JSONB")
    private String metaData;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

