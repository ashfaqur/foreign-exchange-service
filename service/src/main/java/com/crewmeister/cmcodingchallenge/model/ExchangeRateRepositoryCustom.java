package com.crewmeister.cmcodingchallenge.model;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateRepositoryCustom {
    List<ExchangeRateEntity> findRates(LocalDate start, LocalDate end, String currency, int limit, int offset);
}
