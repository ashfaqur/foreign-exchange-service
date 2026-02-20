package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BankService {

    private final BankRestClient restClient;
    private final BankResponseParser parser;
    public BankService(BankRestClient restClient, BankResponseParser parser){
        this.restClient = restClient;
        this.parser = parser;
    }

    public List<ExchangeRateRow> retrieveRates(LocalDate startDate, LocalDate endDate){
        JsonNode jsonNode = this.restClient.fetchRates(startDate, endDate);
        return this.parser.parseRates(jsonNode);
    }
}
