package com.crewmeister.cmcodingchallenge.sync;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private static final int DEFAULT_DAYS = 30;
    private static final Duration MIN_SYNC_INTERVAL = Duration.ofHours(6);
    private static final int DATA_FRESH_DAYS = 2;

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
            log.debug("Sync skipped as another sync is already running");
            return;
        }
        if (Instant.now().isBefore(this.lastSyncAttempt.plus(MIN_SYNC_INTERVAL))) {
            // no need to sync if already done recently (rate limit)
            return;
        }
        try {
            Instant now = Instant.now();
            Instant timeForNextSync = this.lastSyncAttempt.plus(MIN_SYNC_INTERVAL);
            if (now.isBefore(timeForNextSync)) {
                // no need to sync if already done recently (rate limit)
                log.debug("Sync skipped as rate-limited until {}", timeForNextSync);
                return;
            }

            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(days);

            // simple stale data check if few days old
            LocalDate maxDate = this.repo.findMaxDate();
            if (maxDate != null && !maxDate.isBefore(end.minusDays(DATA_FRESH_DAYS))) {
                // data not stale
                log.debug("Sync skipped as data is fresh (maxDate={})", maxDate);
                return;
            }
            this.lastSyncAttempt = now;
            List<ExchangeRateRow> rows = this.bankService.retrieveRates(start, end);
            saveToDb(rows);
            log.info("Sync completed with stored {} rates ({}..{})", rows.size(), start, end);
        } finally {
            this.lock.unlock();
        }
    }

    @Transactional
    void saveToDb(List<ExchangeRateRow> rows) {
        // Convert the row to entiies for storing to db
        List<ExchangeRateEntity> entities = rows.stream()
                .map(r -> new ExchangeRateEntity(r.date(), r.currency(), r.rate()))
                .toList();
        this.repo.saveAll(entities);
    }
}
