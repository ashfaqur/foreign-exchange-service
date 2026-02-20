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

    public ExchangeRateEntity(LocalDate date, String currency, BigDecimal rate) {
        this.id = new ExchangeRateId(date, currency);
        this.rate = rate;
    }

    public ExchangeRateId getId() {
        return this.id;
    }

    public LocalDate getDate() {
        return this.id.getDate();
    }

    public String getCurrency() {
        return this.id.getCurrency();
    }

    public BigDecimal getRate() {
        return this.rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}