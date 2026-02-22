package com.crewmeister.cmcodingchallenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "exchange_rate")
public class ExchangeRateEntity {

    @EmbeddedId
    private ExchangeRateId id;

    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    protected ExchangeRateEntity() {
    }

    /**
     * Creates a rate entity.
     *
     * @param date rate date
     * @param currency currency code
     * @param rate rate value
     */
    public ExchangeRateEntity(LocalDate date, String currency, BigDecimal rate) {
        this.id = new ExchangeRateId(date, currency);
        this.rate = rate;
    }

    /**
     * Returns the composite identifier.
     *
     * @return exchange rate identifier
     */
    public ExchangeRateId getId() {
        return this.id;
    }

    /**
     * Returns the rate date.
     *
     * @return rate date
     */
    public LocalDate getDate() {
        return this.id.getDate();
    }

    /**
     * Returns the currency code.
     *
     * @return currency code
     */
    public String getCurrency() {
        return this.id.getCurrency();
    }

    /**
     * Returns the stored rate value.
     *
     * @return rate value
     */
    public BigDecimal getRate() {
        return this.rate;
    }

    /**
     * Updates the stored rate value.
     *
     * @param rate new rate value
     */
    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
