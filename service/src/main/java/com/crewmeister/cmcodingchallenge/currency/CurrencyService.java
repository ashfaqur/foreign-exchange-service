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
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CurrencyService {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 90;

    private final SyncService syncService;
    private final ExchangeRateRepository repo;

    /**
     * Creates the core service for EUR-based rate retrieval.
     *
     * @param syncService sync service for keeping local data fresh
     * @param repo        exchange rate repository
     */
    public CurrencyService(SyncService syncService, ExchangeRateRepository repo) {
        this.syncService = syncService;
        this.repo = repo;
    }

    /**
     * Returns all available currency codes.
     *
     * @return sorted currency codes
     */
    public List<String> getCurrencies() {
        this.syncService.syncLastDays();
        return this.repo.findDistinctCurrencies();
    }

    /**
     * Returns rates with default pagination.
     *
     * @param start    optional start date (inclusive); must be paired with end
     * @param end      optional end date (inclusive); must be paired with start
     * @param currency optional 3-letter currency code
     * @return rates response
     * @throws IllegalArgumentException if input is invalid
     */
    public RatesResponse getRates(LocalDate start, LocalDate end, String currency) {
        return getRates(start, end, currency, DEFAULT_LIMIT, 0);
    }

    /**
     * Returns rates with optional filters and explicit pagination.
     *
     * @param start    optional start date (inclusive); must be paired with end
     * @param end      optional end date (inclusive); must be paired with start
     * @param currency optional 3-letter currency code
     * @param limit    page size
     * @param offset   row offset
     * @return paginated rates response
     * @throws IllegalArgumentException if input is invalid
     */
    public RatesResponse getRates(LocalDate start, LocalDate end, String currency, int limit, int offset) {
        validateRatesRequest(start, end, currency, limit, offset);
        DateRange range = resolveDateRange(start, end);
        this.syncService.syncRange(range.start(), range.end(), false);

        String normalizedCurrency = normalizeCurrency(currency);
        long total = this.repo.countRates(range.start(), range.end(), normalizedCurrency);
        if (offset >= total) {
            return new RatesResponse("EUR", range.start(), range.end(), List.of(), new PageMeta(limit, offset, total));
        }
        List<ExchangeRateEntity> rows =
                this.repo.findRates(range.start(), range.end(), normalizedCurrency, limit, offset);
        List<RateItem> items = rows.stream()
                .map(e -> new RateItem(e.getDate(), e.getCurrency(), e.getRate()))
                .toList();

        return new RatesResponse("EUR", range.start(), range.end(), items, new PageMeta(limit, offset, total));
    }

    private static void validateRatesRequest(LocalDate start, LocalDate end, String currency, int limit, int offset) {
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

    private static DateRange resolveDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            LocalDate resolvedEnd = LocalDate.now();
            LocalDate resolvedStart = resolvedEnd.minusDays(DEFAULT_RANGE_DAYS - 1L);
            return new DateRange(resolvedStart, resolvedEnd);
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end must be provided together");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must be greater than or equal to start");
        }

        long daysInclusive = ChronoUnit.DAYS.between(start, end) + 1L;
        if (daysInclusive > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("date range cannot be greater than " + MAX_RANGE_DAYS + " days");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns EUR-based rates for a specific date.
     *
     * @param date     date to query
     * @param currency optional currency filter
     * @return rates grouped by currency code
     * @throws RateNotFoundException if no rate exists for the date
     */
    public RatesByDateResponse getRatesByDate(LocalDate date, String currency) {
        this.syncService.syncDay(date);
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

    /**
     * Forces a sync for the provided inclusive date range.
     *
     * @param start start date (inclusive)
     * @param end   end date (inclusive)
     */
    public void forceUpdateData(LocalDate start, LocalDate end) {
        this.syncService.syncRange(start, end, true);
    }
}
