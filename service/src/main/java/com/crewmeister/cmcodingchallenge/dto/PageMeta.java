package com.crewmeister.cmcodingchallenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PageMeta(
        @Schema(description = "Requested page size", example = "1000")
        int limit,
        @Schema(description = "Requested row offset", example = "0")
        int offset,
        @Schema(description = "Total rows matching the filter", example = "123456")
        long total
) {
}
