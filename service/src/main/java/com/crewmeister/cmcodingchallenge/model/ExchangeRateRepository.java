package com.crewmeister.cmcodingchallenge.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId>, ExchangeRateRepositoryCustom {

    /**
     * Finds the latest date currently stored in the database.
     *
     * @return latest stored date, or null when no data exists
     */
    @Query("SELECT MAX(e.id.date) FROM ExchangeRateEntity e")
    LocalDate findMaxDate();

    /**
     * Returns all distinct currencies sorted alphabetically.
     *
     * @return distinct currency codes
     */
    @Query("SELECT DISTINCT e.id.currency FROM ExchangeRateEntity e ORDER BY e.id.currency")
    List<String> findDistinctCurrencies();

    /**
     * Counts rates for optional date and currency filters.
     *
     * @param start    optional start date
     * @param end      optional end date
     * @param currency optional currency code
     * @return number of matching rows
     */
    @Query("""
                SELECT COUNT(e)
                FROM ExchangeRateEntity e
                WHERE (:currency IS NULL OR UPPER(e.id.currency) = UPPER(:currency))
                AND (:start IS NULL OR e.id.date >= :start)
                AND (:end IS NULL OR e.id.date <= :end)
            """)
    long countRates(@Param("start") LocalDate start,
                    @Param("end") LocalDate end,
                    @Param("currency") String currency);

    /**
     * Finds rates for a specific date and optional currency filter.
     *
     * @param date     requested date
     * @param currency optional currency code
     * @return matching rates ordered by currency
     */
    @Query("""
                SELECT e
                FROM ExchangeRateEntity e
                WHERE e.id.date = :date
                  AND (:currency IS NULL OR UPPER(e.id.currency) = UPPER(:currency))
                ORDER BY e.id.currency ASC
            """)
    List<ExchangeRateEntity> findByDateAndOptionalCurrency(@Param("date") LocalDate date,
                                                           @Param("currency") String currency);

    /**
     * Finds a single rate by composite key.
     *
     * @param date     rate date
     * @param currency currency code
     * @return matching rate if present
     */
    Optional<ExchangeRateEntity> findByIdDateAndIdCurrency(LocalDate date, String currency);


    /**
     * Counts distinct dates stored for an inclusive date range.
     *
     * @param start start date (inclusive)
     * @param end   end date (inclusive)
     * @return number of distinct dates in the range
     */
    @Query("""
                SELECT COUNT(DISTINCT e.id.date)
                FROM ExchangeRateEntity e
                WHERE e.id.date >= :start AND e.id.date <= :end
            """)
    long countDistinctDates(@Param("start") LocalDate start,
                            @Param("end") LocalDate end);

}
