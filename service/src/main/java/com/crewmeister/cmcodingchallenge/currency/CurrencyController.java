package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesByDateResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController()
@RequestMapping("/api")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final CurrencyConversionService conversionService;

    public CurrencyController(CurrencyService currencyService, CurrencyConversionService conversionService) {
        this.currencyService = currencyService;
        this.conversionService = conversionService;
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        return ResponseEntity.ok(currencyService.getCurrencies());
    }


    @GetMapping("/rates")
    public ResponseEntity<RatesResponse> getRates(
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate start,

            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate end,

            @RequestParam(required = false)
            String currency,

            @RequestParam(defaultValue = "1000")
            int limit,

            @RequestParam(defaultValue = "0")
            int offset
    ) {
        RatesResponse response = this.currencyService.getRates(start, end, currency, limit, offset);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/rates/{date}")
    public ResponseEntity<RatesByDateResponse> getRatesByDate(
            @PathVariable
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String currency
    ) {
        return ResponseEntity.ok(this.currencyService.getRatesByDate(date, currency));
    }

    @GetMapping("/conversions/to-eur")
    public ResponseEntity<ConversionResponse> convertToEur(
            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate date,
            @RequestParam String currency,
            @RequestParam BigDecimal amount
    ) {
        return ResponseEntity.ok(this.conversionService.convertToEur(date, currency, amount));
    }

    @PostMapping("/update")
    public ResponseEntity<Void> update(
            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate start,

            @RequestParam
            @DateTimeFormat(iso = ISO.DATE)
            LocalDate end
    ) {
        this.currencyService.forceUpdateData(start, end);
        return ResponseEntity.noContent().build();
    }
}