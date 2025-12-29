package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for station response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationResponseDTO {

    private UUID id;
    private String callLetters;
    private String frequency;
    private String market;
    private String format;
    private String ownership;
    private String contacts;
    private String rates;
    private String inventoryClass;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

