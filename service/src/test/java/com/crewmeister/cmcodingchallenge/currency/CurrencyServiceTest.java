package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.RatesByDateResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private SyncService syncService;

    @Mock
    private ExchangeRateRepository repo;

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService(syncService, repo);
    }

    @Test
    void getCurrenciesReturnsDistinctCurrenciesAfterSync() {
        when(repo.findDistinctCurrencies()).thenReturn(List.of("GBP", "USD"));

        List<String> currencies = currencyService.getCurrencies();

        assertEquals(List.of("GBP", "USD"), currencies);
        verify(syncService).syncLastDays();
        verify(repo).findDistinctCurrencies();
    }

    @Test
    void getRatesWithStartAndEndCallsSyncRangeAndBuildsResponse() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 3);
        when(repo.countRates(start, end, "USD")).thenReturn(3L);
        List<ExchangeRateEntity> entities = List.of(
                new ExchangeRateEntity(LocalDate.of(2026, 1, 1), "USD", new BigDecimal("1.1010")),
                new ExchangeRateEntity(LocalDate.of(2026, 1, 2), "USD", new BigDecimal("1.1020"))
        );
        when(repo.findRates(start, end, "USD", 2, 0)).thenReturn(entities);

        RatesResponse response = currencyService.getRates(start, end, "usd", 2, 0);

        assertEquals("EUR", response.base());
        assertEquals(start, response.start());
        assertEquals(end, response.end());
        assertEquals(2, response.items().size());
        assertEquals(LocalDate.of(2026, 1, 1), response.items().get(0).date());
        assertEquals("USD", response.items().get(0).currency());
        assertEquals(new BigDecimal("1.1010"), response.items().get(0).rate());
        assertEquals(2, response.page().limit());
        assertEquals(0, response.page().offset());
        assertEquals(3L, response.page().total());

        verify(syncService).syncRange(start, end, false);
        verify(syncService, never()).syncLastDays();
        verify(repo).countRates(start, end, "USD");
        verify(repo).findRates(start, end, "USD", 2, 0);
    }

    @Test
    void getRatesWithoutRangeCallsSyncLastDays() {
        when(repo.countRates(null, null, null)).thenReturn(0L);

        RatesResponse response = currencyService.getRates(null, null, null, 1000, 0);

        assertEquals("EUR", response.base());
        assertNull(response.start());
        assertNull(response.end());
        assertEquals(0, response.items().size());
        assertEquals(1000, response.page().limit());
        assertEquals(0, response.page().offset());
        assertEquals(0L, response.page().total());

        verify(syncService).syncLastDays();
        verify(syncService, never()).syncRange(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(repo).countRates(null, null, null);
        verify(repo, never()).findRates(null, null, null, 1000, 0);
    }

    @Test
    void getRatesReturnsEmptyItemsWhenOffsetAtOrBeyondTotal() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 3);
        when(repo.countRates(start, end, null)).thenReturn(5L);

        RatesResponse response = currencyService.getRates(start, end, null, 2, 5);

        assertEquals(0, response.items().size());
        assertEquals(2, response.page().limit());
        assertEquals(5, response.page().offset());
        assertEquals(5L, response.page().total());
        verify(repo).countRates(start, end, null);
        verify(repo, never()).findRates(start, end, null, 2, 5);
    }

    @Test
    void getRatesThrowsWhenInvalidRange() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> currencyService.getRates(LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1), null, 1000, 0));

        assertEquals("end must be greater than or equal to start", ex.getMessage());
        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void getRatesThrowsWhenLimitOutOfBounds() {
        IllegalArgumentException exLow = assertThrows(IllegalArgumentException.class,
                () -> currencyService.getRates(null, null, null, 0, 0));
        assertEquals("limit must be between 1 and 5000", exLow.getMessage());

        IllegalArgumentException exHigh = assertThrows(IllegalArgumentException.class,
                () -> currencyService.getRates(null, null, null, 5001, 0));
        assertEquals("limit must be between 1 and 5000", exHigh.getMessage());

        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void getRatesThrowsWhenOffsetNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> currencyService.getRates(null, null, null, 1000, -1));

        assertEquals("offset must be >= 0", ex.getMessage());
        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void getRatesByDateReturnsMappedRatesAndCallsSyncDay() {
        LocalDate date = LocalDate.of(2026, 2, 18);
        List<ExchangeRateEntity> rows = List.of(
                new ExchangeRateEntity(date, "GBP", new BigDecimal("0.8541")),
                new ExchangeRateEntity(date, "USD", new BigDecimal("1.0923"))
        );
        when(repo.findByDateAndOptionalCurrency(date, null)).thenReturn(rows);

        RatesByDateResponse response = currencyService.getRatesByDate(date, null);

        assertEquals("EUR", response.base());
        assertEquals(date, response.date());
        assertEquals(2, response.rates().size());
        assertEquals(new BigDecimal("0.8541"), response.rates().get("GBP"));
        assertEquals(new BigDecimal("1.0923"), response.rates().get("USD"));
        verify(syncService).syncDay(date);
        verify(repo).findByDateAndOptionalCurrency(date, null);
    }

    @Test
    void getRatesByDateThrowsNotFoundWhenNoRows() {
        LocalDate date = LocalDate.of(2026, 2, 18);
        when(repo.findByDateAndOptionalCurrency(date, null)).thenReturn(List.of());

        RateNotFoundException ex = assertThrows(RateNotFoundException.class,
                () -> currencyService.getRatesByDate(date, null));

        assertEquals("No rate exists for that date", ex.getMessage());
        verify(syncService).syncDay(date);
        verify(repo).findByDateAndOptionalCurrency(date, null);
    }

    @Test
    void forceUpdateDataDelegatesToSyncServiceWithForceTrue() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        currencyService.forceUpdateData(start, end);

        verify(syncService).syncRange(start, end, true);
    }
}
