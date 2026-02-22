package com.crewmeister.cmcodingchallenge.dto;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record RatesResponse(
        @Schema(description = "Base currency for all returned rates", example = "EUR")
        String base,
        @Schema(description = "Start date used for filtering (inclusive)", example = "2026-01-01")
        LocalDate start,
        @Schema(description = "End date used for filtering (inclusive)", example = "2026-01-31")
        LocalDate end,
        @Schema(description = "Returned rate items")
        List<RateItem> items,
        @Schema(description = "Pagination metadata")
        PageMeta page
) {
}
