package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BankService {

    private final BankRestClient restClient;
    private final BankResponseParser parser;

    /**
     * Creates a service for retrieving and parsing Bundesbank rates.
     *
     * @param restClient REST client for Bundesbank calls
     * @param parser     parser for SDMX JSON payloads
     */
    public BankService(BankRestClient restClient, BankResponseParser parser) {
        this.restClient = restClient;
        this.parser = parser;
    }

    /**
     * Retrieves EUR-based exchange rates for an inclusive date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return parsed exchange rate rows
     */
    public List<ExchangeRateRow> retrieveRates(LocalDate startDate, LocalDate endDate) {
        JsonNode jsonNode = this.restClient.fetchRates(startDate, endDate);
        return this.parser.parseRates(jsonNode);
    }
}
