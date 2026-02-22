package com.crewmeister.cmcodingchallenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateItem(
        @Schema(description = "Rate date", example = "2026-01-02")
        LocalDate date,
        @Schema(description = "Foreign currency code", example = "USD")
        String currency,
        @Schema(description = "Exchange rate where 1 EUR equals rate units of currency", example = "1.0923")
        BigDecimal rate
) {
}
