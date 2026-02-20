package com.crewmeister.cmcodingchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateItem(LocalDate date, String currency, BigDecimal rate) {}

