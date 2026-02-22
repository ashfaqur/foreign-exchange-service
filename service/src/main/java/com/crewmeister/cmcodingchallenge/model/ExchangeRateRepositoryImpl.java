package com.crewmeister.cmcodingchallenge.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class ExchangeRateRepositoryImpl implements ExchangeRateRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Implements rate lookup with optional filters and row-based pagination.
     *
     * @param start optional start date (inclusive)
     * @param end optional end date (inclusive)
     * @param currency optional currency code
     * @param limit maximum rows to return
     * @param offset row offset
     * @return matching rates ordered by date and currency
     */
    @Override
    public List<ExchangeRateEntity> findRates(LocalDate start, LocalDate end, String currency, int limit, int offset) {

        String jpql = """
            SELECT e
            FROM ExchangeRateEntity e
            WHERE (:currency IS NULL OR UPPER(e.id.currency) = UPPER(:currency))
              AND (:start IS NULL OR e.id.date >= :start)
              AND (:end IS NULL OR e.id.date <= :end)
            ORDER BY e.id.date ASC, e.id.currency ASC
        """;

        TypedQuery<ExchangeRateEntity> q = em.createQuery(jpql, ExchangeRateEntity.class);
        q.setParameter("currency", currency);
        q.setParameter("start", start);
        q.setParameter("end", end);

        q.setFirstResult(offset); // ✅ true row offset
        q.setMaxResults(limit);   // ✅ true limit

        return q.getResultList();
    }
}
