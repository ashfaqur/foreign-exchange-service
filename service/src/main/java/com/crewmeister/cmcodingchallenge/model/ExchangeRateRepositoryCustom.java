package com.crewmeister.cmcodingchallenge.model;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateRepositoryCustom {
    /**
     * Finds rates using optional filters and row-based pagination.
     *
     * @param start    optional start date (inclusive)
     * @param end      optional end date (inclusive)
     * @param currency optional currency code
     * @param limit    maximum rows to return
     * @param offset   row offset
     * @return matching rates ordered by date and currency
     */
    List<ExchangeRateEntity> findRates(LocalDate start, LocalDate end, String currency, int limit, int offset);
}
