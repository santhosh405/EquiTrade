package com.example.demo.dto;

public class PortfolioHoldingDTO {

    private Long portfolioId;
    private Long holdingId;
    private Long stockId;
    private Long id;
    private String symbol;
    private String stockSymbol;
    private String companyName;
    private int quantity;
    private double avgBuyPrice;
    private double averagePrice;
    private double currentPrice;
    private double currentValue;
    private double profitLoss;
    private double profitOrLoss;
    private double profitLossPercent;
    private String sector;

    public PortfolioHoldingDTO() {
    }

    public PortfolioHoldingDTO(Long portfolioId, Long stockId, String symbol, String companyName, int quantity,
                               double avgBuyPrice, double currentPrice, double currentValue, double profitLoss, double profitLossPercent) {
        this(portfolioId, stockId, symbol, companyName, quantity, avgBuyPrice, currentPrice, currentValue, profitLoss, profitLossPercent, "General");
    }

    public PortfolioHoldingDTO(Long portfolioId, Long stockId, String symbol, String companyName, int quantity,
                               double avgBuyPrice, double currentPrice, double currentValue, double profitLoss, double profitLossPercent, String sector) {
        this.portfolioId = portfolioId;
        this.holdingId = portfolioId;
        this.stockId = stockId;
        this.id = stockId;
        this.symbol = symbol;
        this.stockSymbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.averagePrice = avgBuyPrice;
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
        this.profitLoss = profitLoss;
        this.profitOrLoss = profitLoss;
        this.profitLossPercent = profitLossPercent;
        this.sector = sector != null && !sector.isBlank() ? sector : "General";
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
        if (this.holdingId == null) {
            this.holdingId = portfolioId;
        }
    }

    public Long getHoldingId() {
        return holdingId != null ? holdingId : portfolioId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
        if (this.portfolioId == null) {
            this.portfolioId = holdingId;
        }
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
        if (this.id == null) {
            this.id = stockId;
        }
    }

    public Long getId() {
        return id != null ? id : stockId;
    }

    public void setId(Long id) {
        this.id = id;
        if (this.stockId == null) {
            this.stockId = id;
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
        if (this.stockSymbol == null) {
            this.stockSymbol = symbol;
        }
    }

    public String getStockSymbol() {
        return stockSymbol != null ? stockSymbol : symbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
        if (this.symbol == null) {
            this.symbol = stockSymbol;
        }
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public void setAvgBuyPrice(double avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
        this.averagePrice = avgBuyPrice;
    }

    public double getAveragePrice() {
        return averagePrice != 0.0 ? averagePrice : avgBuyPrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
        if (this.avgBuyPrice == 0.0) {
            this.avgBuyPrice = averagePrice;
        }
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
        this.profitOrLoss = profitLoss;
    }

    public double getProfitOrLoss() {
        return profitOrLoss != 0.0 ? profitOrLoss : profitLoss;
    }

    public void setProfitOrLoss(double profitOrLoss) {
        this.profitOrLoss = profitOrLoss;
        if (this.profitLoss == 0.0) {
            this.profitLoss = profitOrLoss;
        }
    }

    public double getProfitLossPercent() {
        return profitLossPercent;
    }

    public void setProfitLossPercent(double profitLossPercent) {
        this.profitLossPercent = profitLossPercent;
    }

    public String getSector() {
        return sector != null ? sector : "General";
    }

    public void setSector(String sector) {
        this.sector = sector;
    }
}
