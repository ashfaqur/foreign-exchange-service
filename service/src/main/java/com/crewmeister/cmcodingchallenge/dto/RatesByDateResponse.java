package com.crewmeister.cmcodingchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RatesByDateResponse (
        String base,
        LocalDate date,
        Map<String, BigDecimal> rates
) {}