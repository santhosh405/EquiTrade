package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_price_history")
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    private double price;

    private LocalDateTime recordedAt;

    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private long volume;

    public StockPriceHistory() {
    }

    public StockPriceHistory(Long id, Stock stock, double price, LocalDateTime recordedAt) {
        this.id = id;
        this.stock = stock;
        this.price = price;
        this.recordedAt = recordedAt;
        this.openPrice = price;
        this.highPrice = price;
        this.lowPrice = price;
        this.closePrice = price;
        this.volume = 10000L;
    }

    public StockPriceHistory(Long id, Stock stock, double price, LocalDateTime recordedAt,
                             double openPrice, double highPrice, double lowPrice, double closePrice, long volume) {
        this.id = id;
        this.stock = stock;
        this.price = price;
        this.recordedAt = recordedAt;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        if (this.closePrice == 0) this.closePrice = price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public double getOpenPrice() {
        return openPrice > 0 ? openPrice : price;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getHighPrice() {
        return highPrice > 0 ? highPrice : Math.max(price, getOpenPrice());
    }

    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    public double getLowPrice() {
        return lowPrice > 0 ? lowPrice : Math.min(price, getOpenPrice());
    }

    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public double getClosePrice() {
        return closePrice > 0 ? closePrice : price;
    }

    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public long getVolume() {
        return volume > 0 ? volume : 15000L;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }
}

