package com.crewmeister.cmcodingchallenge.bank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateRow(LocalDate date, String currency, BigDecimal rate) {}

