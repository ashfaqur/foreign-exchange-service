package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.PageMeta;
import com.crewmeister.cmcodingchallenge.dto.RateItem;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
        this.syncService.syncLastDaysIfStale();
        currency = currency.trim().toUpperCase();
        Pageable pageable = PageRequest.of(
                offset / limit,
                limit,
                Sort.by("id.date").ascending().and(Sort.by("id.currency").ascending())
        );
        long total = this.repo.countRates(start, end, currency);
        List<ExchangeRateEntity> rows = this.repo.findRates(start, end, currency, pageable);
        List<RateItem> items = rows.stream()
                .map(entity -> new RateItem(entity.getDate(), entity.getCurrency(), entity.getRate()))
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
        if (currency == null || currency.isBlank()){
            throw new IllegalArgumentException("currency cannot be empty");
        }
        if (!currency.trim().matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
    }
}
