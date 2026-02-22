package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.dto.PageMeta;
import com.crewmeister.cmcodingchallenge.dto.RateItem;
import com.crewmeister.cmcodingchallenge.dto.RatesByDateResponse;
import com.crewmeister.cmcodingchallenge.dto.RatesResponse;
import com.crewmeister.cmcodingchallenge.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrencyController.class)
@Import(ApiExceptionHandler.class)
class CurrencyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private CurrencyConversionService conversionService;

    @Test
    void getCurrenciesSuccess() throws Exception {
        when(currencyService.getCurrencies()).thenReturn(List.of("USD", "GBP", "JPY"));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("USD"))
                .andExpect(jsonPath("$[1]").value("GBP"))
                .andExpect(jsonPath("$[2]").value("JPY"));

        verify(currencyService).getCurrencies();
    }

    @Test
    void getRatesSuccessWithAllParams() throws Exception {
        RatesResponse response = new RatesResponse(
                "EUR",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                List.of(
                        new RateItem(LocalDate.of(2026, 1, 2), "USD", new BigDecimal("1.0923")),
                        new RateItem(LocalDate.of(2026, 1, 2), "GBP", new BigDecimal("0.8541"))
                ),
                new PageMeta(1000, 0, 2)
        );
        when(currencyService.getRates(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "USD",
                1000,
                0
        )).thenReturn(response);

        mockMvc.perform(get("/api/rates")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31")
                        .param("currency", "USD")
                        .param("limit", "1000")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.start").value("2026-01-01"))
                .andExpect(jsonPath("$.end").value("2026-01-31"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page.limit").value(1000))
                .andExpect(jsonPath("$.page.offset").value(0))
                .andExpect(jsonPath("$.page.total").value(2));

        verify(currencyService).getRates(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "USD",
                1000,
                0
        );
    }

    @Test
    void getRatesSuccessWithDefaults() throws Exception {
        RatesResponse response = new RatesResponse(
                "EUR",
                null,
                null,
                List.of(),
                new PageMeta(1000, 0, 0)
        );
        when(currencyService.getRates(null, null, null, 1000, 0)).thenReturn(response);

        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page.limit").value(1000))
                .andExpect(jsonPath("$.page.offset").value(0));

        verify(currencyService).getRates(null, null, null, 1000, 0);
    }

    @Test
    void getRatesInvalidDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates").param("start", "bad-date"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input"));
    }

    @Test
    void getRatesServiceValidationFailureReturnsBadRequest() throws Exception {
        when(currencyService.getRates(any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new IllegalArgumentException("limit must be between 1 and 5000"));

        mockMvc.perform(get("/api/rates")
                        .param("limit", "99999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("limit must be between 1 and 5000"));
    }

    @Test
    void getRatesByDateSuccessAllCurrencies() throws Exception {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.0923"));
        rates.put("GBP", new BigDecimal("0.8541"));
        rates.put("JPY", new BigDecimal("161.22"));
        RatesByDateResponse response = new RatesByDateResponse(
                "EUR",
                LocalDate.of(2026, 2, 18),
                rates
        );
        when(currencyService.getRatesByDate(LocalDate.of(2026, 2, 18), null)).thenReturn(response);

        mockMvc.perform(get("/api/rates/2026-02-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.date").value("2026-02-18"))
                .andExpect(jsonPath("$.rates.USD").value(1.0923))
                .andExpect(jsonPath("$.rates.GBP").value(0.8541))
                .andExpect(jsonPath("$.rates.JPY").value(161.22));

        verify(currencyService).getRatesByDate(LocalDate.of(2026, 2, 18), null);
    }

    @Test
    void getRatesByDateSuccessSingleCurrencyFilter() throws Exception {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.0923"));
        RatesByDateResponse response = new RatesByDateResponse(
                "EUR",
                LocalDate.of(2026, 2, 18),
                rates
        );
        when(currencyService.getRatesByDate(LocalDate.of(2026, 2, 18), "USD")).thenReturn(response);

        mockMvc.perform(get("/api/rates/2026-02-18").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.date").value("2026-02-18"))
                .andExpect(jsonPath("$.rates.USD").value(1.0923));

        verify(currencyService).getRatesByDate(LocalDate.of(2026, 2, 18), "USD");
    }

    @Test
    void getRatesByDateNotFoundReturns404() throws Exception {
        when(currencyService.getRatesByDate(LocalDate.of(2026, 2, 18), null))
                .thenThrow(new RateNotFoundException("No rate exists for that date"));

        mockMvc.perform(get("/api/rates/2026-02-18"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No rate exists for that date"));
    }

    @Test
    void getRatesByDateInvalidPathDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input"));
    }

    @Test
    void convertToEurSuccess() throws Exception {
        ConversionResponse response = new ConversionResponse(
                LocalDate.of(2026, 2, 18),
                "EUR",
                new ConversionResponse.Money("USD", new BigDecimal("100.00")),
                new ConversionResponse.Money("EUR", new BigDecimal("91.56")),
                new ConversionResponse.Rate("EUR/USD", new BigDecimal("1.0923"))
        );
        when(conversionService.convertToEur(
                LocalDate.of(2026, 2, 18),
                "USD",
                new BigDecimal("100.00")
        )).thenReturn(response);

        mockMvc.perform(get("/api/conversions/to-eur")
                        .param("date", "2026-02-18")
                        .param("currency", "USD")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-02-18"))
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.from.currency").value("USD"))
                .andExpect(jsonPath("$.to.currency").value("EUR"))
                .andExpect(jsonPath("$.rate.pair").value("EUR/USD"));

        verify(conversionService).convertToEur(
                LocalDate.of(2026, 2, 18),
                "USD",
                new BigDecimal("100.00")
        );
    }

    @Test
    void convertToEurMissingRequiredParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/conversions/to-eur")
                        .param("date", "2026-02-18")
                        .param("currency", "USD"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void convertToEurServiceValidationFailureReturnsBadRequest() throws Exception {
        when(conversionService.convertToEur(
                LocalDate.of(2026, 2, 18),
                "USD",
                new BigDecimal("-10")
        )).thenThrow(new IllegalArgumentException("amount must be > 0"));

        mockMvc.perform(get("/api/conversions/to-eur")
                        .param("date", "2026-02-18")
                        .param("currency", "USD")
                        .param("amount", "-10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("amount must be > 0"));
    }

    @Test
    void convertToEurNotFoundReturns404() throws Exception {
        when(conversionService.convertToEur(
                LocalDate.of(2026, 2, 18),
                "USD",
                new BigDecimal("100")
        )).thenThrow(new RateNotFoundException("no rate found"));

        mockMvc.perform(get("/api/conversions/to-eur")
                        .param("date", "2026-02-18")
                        .param("currency", "USD")
                        .param("amount", "100"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("no rate found"));
    }

    @Test
    void updateSuccessReturns204() throws Exception {
        doNothing().when(currencyService).forceUpdateData(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        mockMvc.perform(post("/api/update")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(currencyService).forceUpdateData(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );
    }

    @Test
    void updateInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/update")
                        .param("start", "bad-date")
                        .param("end", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input"));
    }

    @Test
    void updateServiceValidationFailureReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("end date must be greater than or equal to start date"))
                .when(currencyService)
                .forceUpdateData(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/api/update")
                        .param("start", "2026-01-31")
                        .param("end", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("end date must be greater than or equal to start date"));
    }
    
}
