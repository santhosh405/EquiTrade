package com.example.demo.dto;

public class MostTradedStockDTO {

    private Long stockId;
    private String symbol;
    private String companyName;
    private long tradeCount;

    public MostTradedStockDTO() {
    }

    public MostTradedStockDTO(Long stockId, String symbol, String companyName, long tradeCount) {
        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.tradeCount = tradeCount;
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

    public long getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(long tradeCount) {
        this.tradeCount = tradeCount;
    }
}
