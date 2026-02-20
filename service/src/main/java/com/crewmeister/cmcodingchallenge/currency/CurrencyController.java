package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
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
    public CurrencyController(BankService bankService){
        this.bankService = bankService;
    }

    @GetMapping("/rates/test")
    public ResponseEntity<List<ExchangeRateRow>> testRates() {
        LocalDate start = LocalDate.of(2026, 2, 18);
        LocalDate end   = LocalDate.of(2026, 2, 19);

        List<ExchangeRateRow> items = bankService.retrieveRates(start, end);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/currencies")
    public ResponseEntity<ArrayList<CurrencyConversionRates>> getCurrencies() {
        ArrayList<CurrencyConversionRates> currencyConversionRates = new ArrayList<CurrencyConversionRates>();
        currencyConversionRates.add(new CurrencyConversionRates(2.5));

        return new ResponseEntity<ArrayList<CurrencyConversionRates>>(currencyConversionRates, HttpStatus.OK);
    }
}
