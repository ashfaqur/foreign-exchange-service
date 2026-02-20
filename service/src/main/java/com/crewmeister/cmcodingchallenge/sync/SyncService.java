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
    private Instant lastSyncAttempt = Instant.EPOCH;

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
        if (!this.lock.tryLock()){
            // prevent multiple threads requesting data update at the same time
            return;
        }
        if (Instant.now().isBefore(this.lastSyncAttempt.plus(MIN_SYNC_INTERVAL))) {
            // no need to sync if already done recently (rate limit)
            return;
        }
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(days);

            // simple stale data check if few days old
            LocalDate maxDate = this.repo.findMaxDate();
            if (maxDate != null && !maxDate.isBefore(end.minusDays(2))) {
                // data not stale
                return;
            }
            List<ExchangeRateRow> rows = this.bankService.retrieveRates(start, end);
            // Convert the row to entiies for storing to db
            List<ExchangeRateEntity> entities = rows.stream()
                    .map(row -> new ExchangeRateEntity(row.date(), row.currency(), row.rate()))
                    .toList();
            this.repo.saveAll(entities);
        } finally {
            this.lastSyncAttempt = Instant.now();
            this.lock.unlock();
        }
    }
}
