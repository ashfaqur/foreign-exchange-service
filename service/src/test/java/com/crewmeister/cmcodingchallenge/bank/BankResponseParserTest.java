package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BankResponseParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final BankResponseParser parser = new BankResponseParser();

    @Test
    void parseValidInput() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/happy-path.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactlyInAnyOrder(
                new ExchangeRateRow(LocalDate.of(2026, 2, 18), "USD", new BigDecimal("1.2")),
                new ExchangeRateRow(LocalDate.of(2026, 2, 19), "USD", new BigDecimal("1.3")),
                new ExchangeRateRow(LocalDate.of(2026, 2, 18), "GBP", new BigDecimal("0.88"))
        );
    }

    @Test
    void parseRatesInvalidObservationValuesAreSkipped() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/invalid-observation-values.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactly(
                new ExchangeRateRow(LocalDate.of(2026, 2, 22), "USD", new BigDecimal("1.25"))
        );
    }

    @Test
    void parseRatesMalformedSeriesKeyIsSkipped() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/malformed-series-key.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRatesMalformedObservationIndexIsSkipped() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/malformed-observation-index.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRatesMalformedDateIsSkippedByIndexAlignment() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/malformed-date.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactly(
                new ExchangeRateRow(LocalDate.of(2026, 2, 19), "USD", new BigDecimal("1.3"))
        );
    }

    @Test
    void parseRatesMissingDimensionsReturnsEmpty() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/missing-dimensions.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRatesMissingSeriesOrDatasetReturnsEmpty() throws Exception {
        JsonNode payload = jsonFromResource("bank/parser/missing-series-or-dataset.json");

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRatesNullOrInvalidRootReturnsEmpty() {
        assertThat(parser.parseRates(null)).isEmpty();
        assertThat(parser.parseRates(MAPPER.nullNode())).isEmpty();
        assertThat(parser.parseRates(MAPPER.createArrayNode())).isEmpty();
    }

    private static JsonNode jsonFromResource(String resourcePath) throws IOException {
        try (InputStream in = BankResponseParserTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            return MAPPER.readTree(in);
        }
    }
}
