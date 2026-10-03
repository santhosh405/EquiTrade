package com.example.demo.dto;

import java.util.List;

public class PortfolioSummaryDTO {

    private double cashBalance;
    private double totalInvested;
    private double totalCurrentValue;
    private double totalProfitLoss;
    private double totalProfitLossPercent;
    private List<PortfolioHoldingDTO> holdings;

    public PortfolioSummaryDTO() {
    }

    public PortfolioSummaryDTO(double cashBalance, double totalInvested, double totalCurrentValue,
                               double totalProfitLoss, double totalProfitLossPercent, List<PortfolioHoldingDTO> holdings) {
        this.cashBalance = cashBalance;
        this.totalInvested = totalInvested;
        this.totalCurrentValue = totalCurrentValue;
        this.totalProfitLoss = totalProfitLoss;
        this.totalProfitLossPercent = totalProfitLossPercent;
        this.holdings = holdings;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public double getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(double totalInvested) {
        this.totalInvested = totalInvested;
    }

    public double getTotalCurrentValue() {
        return totalCurrentValue;
    }

    public void setTotalCurrentValue(double totalCurrentValue) {
        this.totalCurrentValue = totalCurrentValue;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setTotalProfitLoss(double totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }

    public double getTotalProfitLossPercent() {
        return totalProfitLossPercent;
    }

    public void setTotalProfitLossPercent(double totalProfitLossPercent) {
        this.totalProfitLossPercent = totalProfitLossPercent;
    }

    public List<PortfolioHoldingDTO> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<PortfolioHoldingDTO> holdings) {
        this.holdings = holdings;
    }
}
