package com.example.demo.dto;

import java.time.LocalDateTime;

public class StockPriceHistoryDTO {

    private Long id;
    private double price;
    private LocalDateTime recordedAt;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private String time;
    private double changePercent;

    public StockPriceHistoryDTO() {
    }

    public StockPriceHistoryDTO(Long id, double price, LocalDateTime recordedAt) {
        this.id = id;
        this.price = price;
        this.recordedAt = recordedAt;
        this.open = price;
        this.high = price;
        this.low = price;
        this.close = price;
        this.volume = 15000L;
        if (recordedAt != null) {
            this.time = recordedAt.toLocalDate().toString();
        }
    }

    public StockPriceHistoryDTO(Long id, double open, double high, double low, double close,
                                long volume, String time, LocalDateTime recordedAt) {
        this.id = id;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.price = close;
        this.volume = volume;
        this.time = time;
        this.recordedAt = recordedAt;
        if (open > 0) {
            this.changePercent = Math.round(((close - open) / open * 100.0) * 100.0) / 100.0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getPrice() {
        return price > 0 ? price : close;
    }

    public void setPrice(double price) {
        this.price = price;
        if (this.close == 0) this.close = price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public double getOpen() {
        return open > 0 ? open : price;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high > 0 ? high : Math.max(price, open);
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low > 0 ? low : Math.min(price, open);
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close > 0 ? close : price;
    }

    public void setClose(double close) {
        this.close = close;
        this.price = close;
    }

    public long getVolume() {
        return volume > 0 ? volume : 15000L;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public String getTime() {
        if (time != null && !time.isBlank()) return time;
        if (recordedAt != null) return recordedAt.toLocalDate().toString();
        return "";
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }
}
