package com.crewmeister.cmcodingchallenge.sync;

import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DbWriter {

    private final ExchangeRateRepository repo;

    /**
     * Creates the DB writer for exchange rates.
     *
     * @param repo exchange rate repository
     */
    public DbWriter(ExchangeRateRepository repo) {
        this.repo = repo;
    }

    /**
     * Persists exchange rates as entities in a single transaction.
     *
     * @param rows parsed exchange rate rows
     */
    @Transactional
    public void saveToDb(List<ExchangeRateRow> rows) {
        List<ExchangeRateEntity> entities = rows.stream()
                .map(rate -> new ExchangeRateEntity(rate.date(), rate.currency(), rate.rate()))
                .toList();
        this.repo.saveAll(entities);
    }
}
