package com.crewmeister.cmcodingchallenge.dto;


import java.time.LocalDate;
import java.util.List;

public record RatesResponse(String base, LocalDate start, LocalDate end,
                            List<RateItem> items, PageMeta page) {}