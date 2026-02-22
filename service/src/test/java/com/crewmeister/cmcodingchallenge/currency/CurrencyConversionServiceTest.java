package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    @Mock
    private SyncService syncService;

    @Mock
    private ExchangeRateRepository repo;

    private CurrencyConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new CurrencyConversionService(syncService, repo);
    }

    @Test
    void convertToEurReturnsSameAmountForEurCurrency() {
        LocalDate date = LocalDate.of(2026, 2, 18);

        ConversionResponse response = conversionService.convertToEur(date, "eur", new BigDecimal("100.125"));

        assertEquals("EUR", response.base());
        assertEquals("EUR", response.from().currency());
        assertEquals("EUR", response.to().currency());
        assertEquals(new BigDecimal("100.13"), response.from().amount());
        assertEquals(new BigDecimal("100.13"), response.to().amount());
        assertEquals("EUR/EUR", response.rate().pair());
        assertEquals(BigDecimal.ONE, response.rate().value());
        verify(syncService).syncDay(date);
        verify(repo, never()).findByIdDateAndIdCurrency(date, "EUR");
    }

    @Test
    void convertToEurUsesNormalizedCurrencyAndRepoRate() {
        LocalDate date = LocalDate.of(2026, 2, 18);
        when(repo.findByIdDateAndIdCurrency(date, "USD"))
                .thenReturn(Optional.of(new ExchangeRateEntity(date, "USD", new BigDecimal("1.0923"))));

        ConversionResponse response = conversionService.convertToEur(date, " usd ", new BigDecimal("100.00"));

        assertEquals("USD", response.from().currency());
        assertEquals(new BigDecimal("100.00"), response.from().amount());
        assertEquals("EUR", response.to().currency());
        assertEquals(new BigDecimal("91.55"), response.to().amount());
        assertEquals("EUR/USD", response.rate().pair());
        assertEquals(new BigDecimal("1.0923"), response.rate().value());
        InOrder ordered = inOrder(syncService, repo);
        ordered.verify(syncService).syncDay(date);
        ordered.verify(repo).findByIdDateAndIdCurrency(date, "USD");
    }

    @Test
    void convertToEurRoundsForeignAmountToTwoDecimalsInResponse() {
        LocalDate date = LocalDate.of(2026, 2, 18);
        when(repo.findByIdDateAndIdCurrency(date, "USD"))
                .thenReturn(Optional.of(new ExchangeRateEntity(date, "USD", new BigDecimal("1.0923"))));

        ConversionResponse response = conversionService.convertToEur(date, "USD", new BigDecimal("123.456"));

        assertEquals(new BigDecimal("123.46"), response.from().amount());
        assertEquals(new BigDecimal("113.02"), response.to().amount());
    }

    @Test
    void convertToEurThrowsRateNotFoundWhenRepoHasNoRate() {
        LocalDate date = LocalDate.of(2026, 2, 18);
        when(repo.findByIdDateAndIdCurrency(date, "USD")).thenReturn(Optional.empty());

        RateNotFoundException ex = assertThrows(RateNotFoundException.class,
                () -> conversionService.convertToEur(date, "usd", new BigDecimal("100")));

        assertEquals("no rate found", ex.getMessage());
        verify(syncService).syncDay(date);
        verify(repo).findByIdDateAndIdCurrency(date, "USD");
    }

    @Test
    void convertToEurThrowsWhenDateIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(null, "USD", new BigDecimal("1")));

        assertEquals("date is required", ex.getMessage());
        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void convertToEurThrowsWhenCurrencyIsNullOrBlank() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(LocalDate.of(2026, 2, 18), null, new BigDecimal("1")));
        assertEquals("currency is required", exNull.getMessage());

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(LocalDate.of(2026, 2, 18), "   ", new BigDecimal("1")));
        assertEquals("currency is required", exBlank.getMessage());

        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void convertToEurThrowsWhenCurrencyIsNotThreeLetters() {
        LocalDate date = LocalDate.of(2026, 2, 18);

        IllegalArgumentException exShort = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(date, "US", new BigDecimal("1")));
        assertEquals("currency must be a 3-letter ISO code", exShort.getMessage());

        IllegalArgumentException exLong = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(date, "USDT", new BigDecimal("1")));
        assertEquals("currency must be a 3-letter ISO code", exLong.getMessage());

        IllegalArgumentException exChar = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(date, "U1D", new BigDecimal("1")));
        assertEquals("currency must be a 3-letter ISO code", exChar.getMessage());

        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void convertToEurThrowsWhenAmountIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(LocalDate.of(2026, 2, 18), "USD", null));

        assertEquals("amount is required", ex.getMessage());
        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }

    @Test
    void convertToEurThrowsWhenAmountIsZeroOrNegative() {
        LocalDate date = LocalDate.of(2026, 2, 18);

        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(date, "USD", BigDecimal.ZERO));
        assertEquals("amount must be > 0", exZero.getMessage());

        IllegalArgumentException exNegative = assertThrows(IllegalArgumentException.class,
                () -> conversionService.convertToEur(date, "USD", new BigDecimal("-1")));
        assertEquals("amount must be > 0", exNegative.getMessage());

        verifyNoInteractions(syncService);
        verifyNoInteractions(repo);
    }
}
