package com.crewmeister.cmcodingchallenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RatesByDateResponse (
        @Schema(description = "Base currency for all returned rates", example = "EUR")
        String base,
        @Schema(description = "Date of the returned rates", example = "2026-02-18")
        LocalDate date,
        @Schema(description = "Map of currency code to exchange rate", example = "{\"USD\":1.0923,\"GBP\":0.8541,\"JPY\":161.22}")
        Map<String, BigDecimal> rates
) {}
