package com.example.demo.model;

import java.util.List;

import com.example.demo.model.enums.Exchange;
import com.example.demo.model.enums.InstrumentStatus;
import com.example.demo.model.enums.InstrumentType;
import jakarta.persistence.*;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private List<Double> sparkline;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    private double price;

    private double changePercent;

    private String sector;

    @Enumerated(EnumType.STRING)
    private Exchange exchange = Exchange.NSE;

    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType = InstrumentType.EQUITY;

    @Enumerated(EnumType.STRING)
    private InstrumentStatus instrumentStatus = InstrumentStatus.ACTIVE;

    private boolean tradingRestricted = false;

    private String restrictionReason;

    private int lotSize = 1;

    private double tickSize = 0.05;

    public Stock() {
    }

    public Stock(Long id, String symbol, String companyName, double price) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.changePercent = 0.0;
        this.sector = "General";
        this.exchange = Exchange.NSE;
        this.instrumentType = InstrumentType.EQUITY;
        this.instrumentStatus = InstrumentStatus.ACTIVE;
    }

    public Stock(Long id, String symbol, String companyName, double price, double changePercent, String sector) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.changePercent = changePercent;
        this.sector = sector != null ? sector : "General";
        this.exchange = Exchange.NSE;
        this.instrumentType = InstrumentType.EQUITY;
        this.instrumentStatus = InstrumentStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    public InstrumentStatus getInstrumentStatus() {
        return instrumentStatus;
    }

    public void setInstrumentStatus(InstrumentStatus instrumentStatus) {
        this.instrumentStatus = instrumentStatus;
    }

    public boolean isTradingRestricted() {
        return tradingRestricted;
    }

    public void setTradingRestricted(boolean tradingRestricted) {
        this.tradingRestricted = tradingRestricted;
    }

    public String getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(String restrictionReason) {
        this.restrictionReason = restrictionReason;
    }

    public int getLotSize() {
        return lotSize;
    }

    public void setLotSize(int lotSize) {
        this.lotSize = lotSize;
    }

    public double getTickSize() {
        return tickSize;
    }

    public void setTickSize(double tickSize) {
        this.tickSize = tickSize;
    }

    public List<Double> getSparkline() {
        return sparkline;
    }

    public void setSparkline(List<Double> sparkline) {
        this.sparkline = sparkline;
    }
}