package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BankResponseParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final BankResponseParser parser = new BankResponseParser();

    @Test
    void parseRates_happyPath_returnsRows() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"FREQ","values":[{"id":"D"}]},
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"},{"id":"GBP"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"2026-02-18"},{"id":"2026-02-19"}]}
                        ]
                      }
                    },
                    "dataSets": [
                      {
                        "series": {
                          "0:0:0:0:0:0": {"observations": {"0":[1.2], "1":[1.3]}},
                          "0:1:0:0:0:0": {"observations": {"0":["0.88"]}}
                        }
                      }
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactlyInAnyOrder(
                new ExchangeRateRow(LocalDate.of(2026, 2, 18), "USD", new BigDecimal("1.2")),
                new ExchangeRateRow(LocalDate.of(2026, 2, 19), "USD", new BigDecimal("1.3")),
                new ExchangeRateRow(LocalDate.of(2026, 2, 18), "GBP", new BigDecimal("0.88"))
        );
    }

    @Test
    void parseRates_invalidObservationValues_areSkipped() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"FREQ","values":[{"id":"D"}]},
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"2026-02-18"},{"id":"2026-02-19"},{"id":"2026-02-20"},{"id":"2026-02-21"},{"id":"2026-02-22"}]}
                        ]
                      }
                    },
                    "dataSets": [
                      {
                        "series": {
                          "0:0:0:0:0:0": {
                            "observations": {
                              "0":[null],
                              "1":["null"],
                              "2":[" "],
                              "3":["abc"],
                              "4":["1.25"]
                            }
                          }
                        }
                      }
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactly(
                new ExchangeRateRow(LocalDate.of(2026, 2, 22), "USD", new BigDecimal("1.25"))
        );
    }

    @Test
    void parseRates_malformedSeriesKey_isSkipped() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"FREQ","values":[{"id":"D"}]},
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"2026-02-18"}]}
                        ]
                      }
                    },
                    "dataSets": [
                      {
                        "series": {
                          "0:x:0:0:0:0": {"observations": {"0":[1.2]}}
                        }
                      }
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRates_malformedObservationIndex_isSkipped() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"FREQ","values":[{"id":"D"}]},
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"2026-02-18"}]}
                        ]
                      }
                    },
                    "dataSets": [
                      {
                        "series": {
                          "0:0:0:0:0:0": {"observations": {"x":[1.2]}}
                        }
                      }
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRates_malformedDate_isSkippedByIndexAlignment() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"FREQ","values":[{"id":"D"}]},
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"bad-date"},{"id":"2026-02-19"}]}
                        ]
                      }
                    },
                    "dataSets": [
                      {
                        "series": {
                          "0:0:0:0:0:0": {"observations": {"0":[1.2], "1":[1.3]}}
                        }
                      }
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);

        assertThat(rates).containsExactly(
                new ExchangeRateRow(LocalDate.of(2026, 2, 19), "USD", new BigDecimal("1.3"))
        );
    }

    @Test
    void parseRates_missingDimensions_returnsEmpty() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [],
                        "observation": []
                      }
                    },
                    "dataSets": [
                      {"series": {"0:0:0:0:0:0": {"observations":{"0":[1.2]}}}}
                    ]
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRates_missingSeriesOrDataset_returnsEmpty() throws Exception {
        JsonNode payload = json("""
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          {"id":"BBK_STD_CURRENCY","values":[{"id":"USD"}]}
                        ],
                        "observation": [
                          {"id":"TIME_PERIOD","values":[{"id":"2026-02-18"}]}
                        ]
                      }
                    },
                    "dataSets": []
                  }
                }
                """);

        List<ExchangeRateRow> rates = parser.parseRates(payload);
        assertThat(rates).isEmpty();
    }

    @Test
    void parseRates_nullOrInvalidRoot_returnsEmpty() {
        assertThat(parser.parseRates(null)).isEmpty();
        assertThat(parser.parseRates(MAPPER.nullNode())).isEmpty();
        assertThat(parser.parseRates(MAPPER.createArrayNode())).isEmpty();
    }

    private static JsonNode json(String value) throws IOException {
        return MAPPER.readTree(value);
    }
}
