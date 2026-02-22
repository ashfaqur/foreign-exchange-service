package com.crewmeister.cmcodingchallenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(
        @Schema(description = "Conversion date", example = "2026-02-18")
        LocalDate date,
        @Schema(description = "Target base currency", example = "EUR")
        String base,
        @Schema(description = "Input amount in foreign currency")
        Money from,
        @Schema(description = "Converted amount in EUR")
        Money to,
        @Schema(description = "Rate details used for conversion")
        Rate rate
) {
    public record Money(
            @Schema(description = "Currency code", example = "USD")
            String currency,
            @Schema(description = "Amount value", example = "100.00")
            BigDecimal amount
    ) {
    }

    public record Rate(
            @Schema(description = "Rate pair label", example = "EUR/USD")
            String pair,
            @Schema(description = "Rate value", example = "1.0923")
            BigDecimal value
    ) {
    }
}
