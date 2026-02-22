package com.crewmeister.cmcodingchallenge.sync;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private BankService bankService;

    @Mock
    private ExchangeRateRepository repo;

    @Mock
    private DbWriter dbWriter;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(bankService, repo, dbWriter);
    }

    @Test
    void syncRangeSkipsWhenAlreadyCoveredAndNotForced() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 3);
        when(repo.countDistinctDates(start, end)).thenReturn(3L);

        syncService.syncRange(start, end, false);

        verify(repo).countDistinctDates(start, end);
        verify(bankService, never()).retrieveRates(start, end);
        verify(dbWriter, never()).saveToDb(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void syncRangeForcedIgnoresCoverageAndFetchesFromBank() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 3);
        List<ExchangeRateRow> rows = List.of(
                new ExchangeRateRow(LocalDate.of(2026, 1, 1), "USD", new BigDecimal("1.10"))
        );
        when(bankService.retrieveRates(start, end)).thenReturn(rows);

        syncService.syncRange(start, end, true);

        verify(repo, never()).countDistinctDates(start, end);
        verify(bankService).retrieveRates(start, end);
        verify(dbWriter).saveToDb(rows);
    }

    @Test
    void syncRangeThrowsForNullDates() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.syncRange(null, LocalDate.of(2026, 1, 1), false));
        assertEquals("start and end dates must be provided", ex.getMessage());
    }

    @Test
    void syncRangeThrowsWhenEndBeforeStart() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.syncRange(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1), false));
        assertEquals("end date must be greater than or equal to start date", ex.getMessage());
    }

    @Test
    void syncRangeThrowsWhenRangeExceedsMaxDays() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.syncRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 5), false));
        assertEquals("date range cannot be greater than 90 days", ex.getMessage());
    }

    @Test
    void syncDayUsesSameStartAndEndDate() {
        LocalDate date = LocalDate.of(2026, 2, 1);
        when(repo.countDistinctDates(date, date)).thenReturn(0L);
        when(bankService.retrieveRates(date, date)).thenReturn(List.of());

        syncService.syncDay(date);

        verify(bankService).retrieveRates(date, date);
        verify(dbWriter).saveToDb(List.of());
    }

    @Test
    void syncLastDaysUsesThirtyDayWindow() {
        when(repo.countDistinctDates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(bankService.retrieveRates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        syncService.syncLastDays();

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(bankService).retrieveRates(startCaptor.capture(), endCaptor.capture());

        LocalDate start = startCaptor.getValue();
        LocalDate end = endCaptor.getValue();
        assertEquals(30L, java.time.temporal.ChronoUnit.DAYS.between(start, end));
    }
}
