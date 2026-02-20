package com.crewmeister.cmcodingchallenge.currency;

import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyService {


    private final SyncService syncService;
    private final ExchangeRateRepository repo;

    public CurrencyService(SyncService syncService, ExchangeRateRepository repo) {
        this.syncService = syncService;
        this.repo = repo;
    }

    public List<String> getCurrencies() {
        this.syncService.syncLastDaysIfStale(30);
        return repo.findDistinctCurrencies();
    }
}
