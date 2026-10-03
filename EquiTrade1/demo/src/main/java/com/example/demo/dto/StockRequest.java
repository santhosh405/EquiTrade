package com.example.demo.dto;

public class StockRequest {

    private String symbol;
    private String companyName;
    private double price;
    private double changePercent;
    private String sector;

    public StockRequest() {
    }

    public StockRequest(String symbol, String companyName, double price, double changePercent, String sector) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.changePercent = changePercent;
        this.sector = sector;
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
