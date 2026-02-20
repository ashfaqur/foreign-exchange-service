package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class BankRestClient {

    private static final String REQUEST_PATH = "/rest/data/BBEX3/D..EUR.BB.AC.000";
    private static final String RESPONSE_FORMAT = "sdmx_json";

    private final RestClient restClient;

    public BankRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public JsonNode fetchRates(LocalDate start, LocalDate end) {
        validateRange(start, end);

        return this.restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(REQUEST_PATH)
                        .queryParam("format", RESPONSE_FORMAT)
                        .queryParam("startPeriod", start)
                        .queryParam("endPeriod", end)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BundesbankClientException(
                            res.getStatusCode().value(), extractErrorResponseBodyInfo(res));
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new BundesbankServerException(
                            res.getStatusCode().value(), extractErrorResponseBodyInfo(res));
                })
                .body(JsonNode.class);
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start date must not be null");
        Objects.requireNonNull(end, "end date must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end date must be greater than or equal to start date");
        }
    }

    private static String extractErrorResponseBodyInfo(ClientHttpResponse response) {
        try {
            byte[] bytes = response.getBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unable to read error body>";
        }
    }
}
