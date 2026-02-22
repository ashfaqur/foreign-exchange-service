package com.crewmeister.cmcodingchallenge.sync;

import com.crewmeister.cmcodingchallenge.bank.BankService;
import com.crewmeister.cmcodingchallenge.bank.ExchangeRateRow;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateRepository;
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

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 90;

    private final BankService bankService;
    private final ExchangeRateRepository repo;
    private final DbWriter dbWriter;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates the sync service.
     *
     * @param bankService service used to fetch Bundesbank data
     * @param repo repository used for coverage checks
     * @param dbWriter writer used for transactional persistence
     */
    public SyncService(BankService bankService, ExchangeRateRepository repo, DbWriter dbWriter) {
        this.bankService = bankService;
        this.repo = repo;
        this.dbWriter = dbWriter;
    }

    /**
     * Syncs EUR-based rates for the default recent time window.
     */
    public void syncLastDays() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(DEFAULT_DAYS);
        syncRange(start, end, false);
    }

    /**
     * Syncs EUR-based rates for a single day.
     *
     * @param date date to sync
     */
    public void syncDay(LocalDate date) {
        syncRange(date, date, false);
    }

    /**
     * Syncs EUR-based rates for an inclusive date range.
     *
     * @param start start date (inclusive)
     * @param end end date (inclusive)
     * @param force whether to bypass DB coverage checks
     * @throws IllegalArgumentException if the date range is invalid
     * @throws SyncInProgressException if another sync is already running
     */
    public void syncRange(LocalDate start, LocalDate end, boolean force) {
        long days = validateInput(start, end);
        if (!this.lock.tryLock()) {
            throw new SyncInProgressException("sync in progress, retry");
        }
        try {
            if (!force && isRangeCoveredInDb(start, end, days)) {
                LOG.debug("Sync not needed as range already covered  ({}..{}) in DB", start, end);
                return;
            }
            List<ExchangeRateRow> rows = this.bankService.retrieveRates(start, end);
            this.dbWriter.saveToDb(rows);
            LOG.info("Sync completed with stored {} rates ({}..{})", rows.size(), start, end);
        } finally {
            this.lock.unlock();
        }
    }

    private static long validateInput(LocalDate start, LocalDate end){
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end dates must be provided");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end date must be greater than or equal to start date");
        }
        long daysInclusive = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (daysInclusive <= 0) {
            throw new IllegalArgumentException("date range must be at least 1 day");
        }
        if (daysInclusive > MAX_DAYS) {
            throw new IllegalArgumentException("date range cannot be greater than " + MAX_DAYS + " days");
        }
        return daysInclusive;
    }

    /**
     * Checks if the db already has info for the given dates
     * Assume that a date in database means that all info for
     * that day is fully stored
     */
    private boolean isRangeCoveredInDb(LocalDate start, LocalDate end, long daysInclusive) {
        long distinctDates = this.repo.countDistinctDates(start, end);
        return distinctDates >= daysInclusive;
    }
}
