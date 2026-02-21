package com.crewmeister.cmcodingchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(
        LocalDate date,
        String base,
        Money from,
        Money to,
        Rate rate
) {
    public record Money(String currency, BigDecimal amount) {}
    public record Rate(String pair, BigDecimal value) {}
}