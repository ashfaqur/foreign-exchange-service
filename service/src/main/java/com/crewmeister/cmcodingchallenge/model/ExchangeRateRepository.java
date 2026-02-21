package com.crewmeister.cmcodingchallenge.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId>, ExchangeRateRepositoryCustom {

    @Query("SELECT MAX(e.id.date) FROM ExchangeRateEntity e")
    LocalDate findMaxDate();

    @Query("SELECT DISTINCT e.id.currency FROM ExchangeRateEntity e ORDER BY e.id.currency")
    List<String> findDistinctCurrencies();

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

    @Query("""
            SELECT e
            FROM ExchangeRateEntity e
            WHERE e.id.date = :date
              AND (:currency IS NULL OR UPPER(e.id.currency) = UPPER(:currency))
            ORDER BY e.id.currency ASC
        """)
    List<ExchangeRateEntity> findByDateAndOptionalCurrency(@Param("date") LocalDate date,
                                                           @Param("currency") String currency);

    Optional<ExchangeRateEntity> findByIdDateAndIdCurrency(LocalDate date, String currency);


    @Query("""
            SELECT COUNT(DISTINCT e.id.date)
            FROM ExchangeRateEntity e
            WHERE e.id.date >= :start AND e.id.date <= :end
        """)
    long countDistinctDates(@Param("start") LocalDate start,
                            @Param("end") LocalDate end);

}
