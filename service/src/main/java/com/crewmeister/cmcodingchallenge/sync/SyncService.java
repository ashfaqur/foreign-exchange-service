package com.crewmeister.cmcodingchallenge.sync;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SyncService {

    private static final int DEFAULT_DAYS = 30;
    private static final Duration MIN_SYNC_INTERVAL = Duration.ofHours(6);

    private final BankService bankService;
    private final ExchangeRateRepository repo;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile Instant lastSyncAttempt = Instant.EPOCH;

    public SyncService(BankService bankService, ExchangeRateRepository repo) {
        this.bankService = bankService;
        this.repo = repo;
    }

    @Transactional
    public void syncLastDaysIfStale() {
        syncLastDaysIfStale(DEFAULT_DAYS);
    }

    @Transactional
    public void syncLastDaysIfStale(int days) {
        // simpple rate-limit 
        if (Instant.now().isBefore(lastSyncAttempt.plus(MIN_SYNC_INTERVAL))) {
            return;
        }

        if (!lock.tryLock()) return;
        try {
            lastSyncAttempt = Instant.now();

            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(days);

            // simple stale rule
            LocalDate maxDate = repo.findMaxDate();
            if (maxDate != null && !maxDate.isBefore(end.minusDays(2))) {
                // good enough recent data
                return;
            }

            List<ExchangeRateRow> rows = bankService.retrieveRates(start, end);
            save(rows);

        } finally {
            lock.unlock();
        }
    }

    private void save(List<ExchangeRateRow> rows) {
        List<ExchangeRateEntity> entities = rows.stream()
                .map(r -> new ExchangeRateEntity(r.date(), r.currency(), r.rate()))
                .toList();
        repo.saveAll(entities);
    }
}
