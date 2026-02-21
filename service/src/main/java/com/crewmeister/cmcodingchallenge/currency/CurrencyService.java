package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.PageMeta;
import com.crewmeister.cmcodingchallenge.dto.RateItem;
import com.crewmeister.cmcodingchallenge.dto.RatesByDateResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CurrencyService {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;

    private final SyncService syncService;
    private final ExchangeRateRepository repo;

    public CurrencyService(SyncService syncService, ExchangeRateRepository repo) {
        this.syncService = syncService;
        this.repo = repo;
    }

    public List<String> getCurrencies() {
        this.syncService.syncLastDaysIfStale();
        return this.repo.findDistinctCurrencies();
    }

    public RatesResponse getRates(LocalDate start, LocalDate end, String currency) {
        return getRates(start, end, currency, DEFAULT_LIMIT, 0);
    }

    public RatesResponse getRates(LocalDate start, LocalDate end, String currency, int limit, int offset) {
        validateRatesRequest(start, end, currency, limit, offset);
        if (start != null && end != null) {
            this.syncService.syncRangeIfNeeded(start, end);
        } else {
            this.syncService.syncLastDaysIfStale();
        }
        String normalizedCurrency = normalizeCurrency(currency);
        long total = this.repo.countRates(start, end, normalizedCurrency);
        if (offset >= total) {
            return new RatesResponse("EUR", start, end, List.of(), new PageMeta(limit, offset, total));
        }
        List<ExchangeRateEntity> rows =
                this.repo.findRates(start, end, normalizedCurrency, limit, offset);
        List<RateItem> items = rows.stream()
                .map(e -> new RateItem(e.getDate(), e.getCurrency(), e.getRate()))
                .toList();

        return new RatesResponse("EUR", start, end, items, new PageMeta(limit, offset, total));
    }

    private static void validateRatesRequest(LocalDate start, LocalDate end, String currency, int limit, int offset) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("end must be greater than or equal to start");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        if (currency != null && !currency.isBlank()) {
            String c = currency.trim();
            if (!c.matches("^[A-Za-z]{3}$")) {
                throw new IllegalArgumentException("currency must be a 3-letter ISO code");
            }
        }
    }
    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    public RatesByDateResponse getRatesByDate(LocalDate date, String currency) {
        this.syncService.syncDayIfNeeded(date);
        String normalizedCurrency = normalizeCurrency(currency);
        List<ExchangeRateEntity> rows =
                this.repo.findByDateAndOptionalCurrency(date, normalizedCurrency);
        if (rows.isEmpty()) {
            throw new RateNotFoundException("No rate exists for that date");
        }
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (ExchangeRateEntity e : rows) {
            rates.put(e.getCurrency(), e.getRate());
        }
        return new RatesByDateResponse("EUR", date, rates);
    }
}
