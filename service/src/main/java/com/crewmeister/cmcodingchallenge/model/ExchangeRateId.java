package com.crewmeister.cmcodingchallenge.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;


import jakarta.persistence.Column;

import java.util.Objects;

@Embeddable
public class ExchangeRateId implements Serializable {

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected ExchangeRateId() {
    }

    /**
     * Creates a composite rate identifier.
     *
     * @param date rate date
     * @param currency currency code
     */
    public ExchangeRateId(LocalDate date, String currency) {
        this.date = Objects.requireNonNull(date, "date");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    /**
     * Returns the rate date.
     *
     * @return rate date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the currency code.
     *
     * @return currency code
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Compares composite key values.
     *
     * @param o object to compare
     * @return true when date and currency are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExchangeRateId given)){
            return false;
        }
        return Objects.equals(this.date, given.date) &&
                Objects.equals(this.currency, given.currency);
    }

    /**
     * Computes hash code from date and currency.
     *
     * @return hash code for this composite key
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.date, this.currency);
    }
}
