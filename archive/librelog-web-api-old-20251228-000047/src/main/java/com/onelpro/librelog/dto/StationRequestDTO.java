package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a station.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationRequestDTO {

    @NotBlank(message = "Call letters are required")
    @Size(max = 10, message = "Call letters must not exceed 10 characters")
    private String callLetters;

    @Size(max = 20, message = "Frequency must not exceed 20 characters")
    private String frequency;

    @Size(max = 255, message = "Market must not exceed 255 characters")
    private String market;

    @Size(max = 100, message = "Format must not exceed 100 characters")
    private String format;

    @Size(max = 255, message = "Ownership must not exceed 255 characters")
    private String ownership;

    private String contacts; // JSONB field

    private String rates; // JSONB field

    @Size(max = 50, message = "Inventory class must not exceed 50 characters")
    private String inventoryClass;

    @Builder.Default
    private Boolean active = true;
}

