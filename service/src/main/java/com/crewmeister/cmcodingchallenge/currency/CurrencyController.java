package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController()
@RequestMapping("/api")
public class CurrencyController {

    private final BankService bankService;
    private final CurrencyService currencyService;

    public CurrencyController(BankService bankService, CurrencyService currencyService) {
        this.bankService = bankService;
        this.currencyService = currencyService;
    }

    @GetMapping("/rates/test")
    public ResponseEntity<List<ExchangeRateRow>> testRates() {
        LocalDate start = LocalDate.of(2026, 2, 18);
        LocalDate end = LocalDate.of(2026, 2, 19);

        List<ExchangeRateRow> items = bankService.retrieveRates(start, end);
        return ResponseEntity.ok(items);
    }


//    @GetMapping("/rates")
//    public ResponseEntity<RatesResponse> getRates() {
//        return ResponseEntity.ok();
//    }

    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        return ResponseEntity.ok(currencyService.getCurrencies());
    }
}