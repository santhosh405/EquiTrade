package com.example.demo.dto;

public class WatchlistDTO {

    private Long id;
    private Long stockId;
    private String symbol;
    private String companyName;
    private double price;
    private double changePercent;
    private String sector;

    public WatchlistDTO() {
    }

    public WatchlistDTO(Long id, Long stockId, String symbol, String companyName, double price, double changePercent, String sector) {
        this.id = id;
        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.changePercent = changePercent;
        this.sector = sector;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
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
}
