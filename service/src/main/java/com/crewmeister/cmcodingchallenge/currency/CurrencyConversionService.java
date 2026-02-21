package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class CurrencyConversionService {

    private final SyncService syncService;
    private final ExchangeRateRepository repo;

    public CurrencyConversionService(SyncService syncService, ExchangeRateRepository repo) {
        this.syncService = syncService;
        this.repo = repo;
    }


    public ConversionResponse convertToEur(LocalDate date, String currency, BigDecimal foreignCurrencyAmount) {
        validateInput(date, currency, foreignCurrencyAmount);
        this.syncService.syncLastDaysIfStale();
        String normalizedCurrency = normalizeCurrency(currency);

        // Handling the special case of EUR -> EUR, just return the same amount rounded
        if (normalizedCurrency.equals("EUR")) {
            BigDecimal rounded = foreignCurrencyAmount.setScale(2, RoundingMode.HALF_UP);
            return new ConversionResponse(
                    date,
                    "EUR",
                    new ConversionResponse.Money("EUR", rounded),
                    new ConversionResponse.Money("EUR", rounded),
                    new ConversionResponse.Rate("EUR/EUR", BigDecimal.ONE)
            );
        }

        ExchangeRateEntity rateEntity = this.repo.findByIdDateAndIdCurrency(date, normalizedCurrency)
                .orElseThrow(() -> new RateNotFoundException("no rate found"));
        BigDecimal rate = rateEntity.getRate();
        BigDecimal givenForeignCurrencyAmount = foreignCurrencyAmount.setScale(2, RoundingMode.HALF_UP);
        // conversion to euro from given foreign curreny amount
        BigDecimal eurPerForeign = foreignCurrencyAmount.divide(rate, 10, RoundingMode.HALF_UP);
        BigDecimal eurRounded = eurPerForeign.setScale(2, RoundingMode.HALF_UP);

        return new ConversionResponse(
                date,
                "EUR",
                new ConversionResponse.Money(normalizedCurrency, givenForeignCurrencyAmount),
                new ConversionResponse.Money("EUR", eurRounded),
                new ConversionResponse.Rate("EUR/" + normalizedCurrency, rateEntity.getRate())
        );
    }

    private static void validateInput(LocalDate date, String currency, BigDecimal amount) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        String c = currency.trim();
        if (!c.matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return null;
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
