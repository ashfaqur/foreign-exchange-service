package com.crewmeister.cmcodingchallenge.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId> {

    @Query("SELECT MAX(e.id.date) FROM ExchangeRateEntity e")
    LocalDate findMaxDate();

    @Query("SELECT DISTINCT e.id.currency FROM ExchangeRateEntity e ORDER BY e.id.currency")
    List<String> findDistinctCurrencies();

    boolean existsByIdDate(LocalDate date);
}
