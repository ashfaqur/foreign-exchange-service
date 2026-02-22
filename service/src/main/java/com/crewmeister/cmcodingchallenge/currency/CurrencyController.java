package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesByDateResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController()
@RequestMapping("/api")
@Tag(name = "Currency API", description = "Endpoints for EUR-based rates, conversion to EUR, and sync updates.")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final CurrencyConversionService conversionService;

    /**
     * Creates the currency API controller.
     *
     * @param currencyService   service for currency and rate endpoints
     * @param conversionService service for conversion endpoint
     */
    public CurrencyController(CurrencyService currencyService, CurrencyConversionService conversionService) {
        this.currencyService = currencyService;
        this.conversionService = conversionService;
    }

    /**
     * Returns all available currencies for EUR-based rates.
     *
     * @return list of currency codes
     */
    @GetMapping("/currencies")
    @Operation(summary = "List available currencies")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Currencies returned",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(type = "string"),
                            arraySchema = @Schema(example = "[\"AUD\",\"USD\"]")
                    ))
            ),
            @ApiResponse(responseCode = "503", description = "Sync in progress, retry")
    })
    public ResponseEntity<List<String>> getCurrencies() {
        return ResponseEntity.ok(currencyService.getCurrencies());
    }


    /**
     * Returns EUR-based rates with optional filtering and pagination.
     *
     * @return paginated rates response
     */
    @GetMapping("/rates")
    @Operation(summary = "Get EUR-based rates collection",
            description = "If start and end are omitted, the API returns the last 30 calendar days. " +
                    "If a custom range is needed, both start and end must be provided together.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates returned",
                    content = @Content(schema = @Schema(implementation = RatesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request input, one-sided range, or range greater than 90 days"),
            @ApiResponse(responseCode = "503", description = "Sync in progress, retry")
    })
    public ResponseEntity<RatesResponse> getRates(
            @Parameter(description = "Start date (inclusive). Must be provided together with end. Omit both to use default last 30 days.", example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate start,

            @Parameter(description = "End date (inclusive). Must be provided together with start. Omit both to use default last 30 days.", example = "2026-01-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate end,

            @Parameter(description = "Optional 3-letter currency code", example = "USD")
            @RequestParam(required = false)
            String currency,

            @Parameter(description = "Page size (default 1000, max 5000)", example = "1000")
            @RequestParam(defaultValue = "1000")
            int limit,

            @Parameter(description = "Row offset (default 0)", example = "0")
            @RequestParam(defaultValue = "0")
            int offset
    ) {
        RatesResponse response = this.currencyService.getRates(start, end, currency, limit, offset);
        return ResponseEntity.ok(response);
    }


    /**
     * Returns EUR-based rates for a single date.
     *
     * @return rates for the requested date
     */
    @GetMapping("/rates/{date}")
    @Operation(summary = "Get rates for a specific date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates returned",
                    content = @Content(schema = @Schema(implementation = RatesByDateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request input"),
            @ApiResponse(responseCode = "404", description = "No rate exists for that date"),
            @ApiResponse(responseCode = "503", description = "Sync in progress, retry")
    })
    public ResponseEntity<RatesByDateResponse> getRatesByDate(
            @Parameter(description = "Rate date", example = "2026-02-18")
            @PathVariable
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate date,
            @Parameter(description = "Optional 3-letter currency code", example = "USD")
            @RequestParam(required = false) String currency
    ) {
        return ResponseEntity.ok(this.currencyService.getRatesByDate(date, currency));
    }

    /**
     * Converts a foreign-currency amount to EUR for a given date.
     *
     * @return conversion response
     */
    @GetMapping("/conversions/to-eur")
    @Operation(summary = "Convert foreign-currency amount to EUR")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion returned",
                    content = @Content(schema = @Schema(implementation = ConversionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request input"),
            @ApiResponse(responseCode = "404", description = "No rate found"),
            @ApiResponse(responseCode = "503", description = "Sync in progress, retry")
    })
    public ResponseEntity<ConversionResponse> convertToEur(
            @Parameter(description = "Conversion date", example = "2026-02-18")
            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate date,
            @Parameter(description = "Foreign 3-letter currency code", example = "USD")
            @RequestParam String currency,
            @Parameter(description = "Amount in foreign currency", example = "100.00")
            @RequestParam BigDecimal amount
    ) {
        return ResponseEntity.ok(this.conversionService.convertToEur(date, currency, amount));
    }

    /**
     * Forces a data sync for the requested inclusive date range.
     *
     * @return no-content response when sync is accepted
     */
    @PostMapping("/update")
    @Operation(summary = "Force sync for a date range")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sync triggered"),
            @ApiResponse(responseCode = "400", description = "Invalid request input"),
            @ApiResponse(responseCode = "500", description = "Sync failed"),
            @ApiResponse(responseCode = "503", description = "Sync in progress, retry")
    })
    public ResponseEntity<Void> update(
            @Parameter(description = "Start date (inclusive)", example = "2026-01-01")
            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate start,

            @Parameter(description = "End date (inclusive)", example = "2026-01-31")
            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate end
    ) {
        this.currencyService.forceUpdateData(start, end);
        return ResponseEntity.noContent().build();
    }
}
