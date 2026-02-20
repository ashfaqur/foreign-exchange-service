package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.bank.BankRestClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

@SpringBootTest
class BankRestClientSmokeTest {

    @Test
    void fetchRates_realCall_smokeTest() {
        RestClient restClient = RestClient.create();
        BankRestClient client = new BankRestClient(restClient);

        LocalDate start = LocalDate.of(2026,02,18);
        LocalDate end = LocalDate.of(2026,02,19);

        JsonNode json = client.fetchRates(start, end);
        System.out.println(json);

        assertThat(json).isNotNull();
        assertThat(json.path("meta").isMissingNode()).isFalse();
        assertThat(json.path("data").isMissingNode()).isFalse();
    }
}