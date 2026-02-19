package com.crewmeister.cmcodingchallenge.bank;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

public class BankRestClient {

    private static final String BASE_URL = "https://api.statistiken.bundesbank.de";
    private static final String REQUEST_PATH = "/rest/data/BBEX3/D..EUR.BB.AC.000";
    private static final String RESPONSE_FORMAT = "sdmx_json";

    private final RestClient restClient;

    public BankRestClient(RestClient restClient){
        this.restClient = restClient;
    }

    public URI buildGeocodeRequestFullUri(LocalDate start, LocalDate end) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path(REQUEST_PATH)
                .queryParam("format", RESPONSE_FORMAT)
                .queryParam("startPeriod", start)
                .queryParam("endPeriod", end)
                .build()
                .toUri();
    }
}
