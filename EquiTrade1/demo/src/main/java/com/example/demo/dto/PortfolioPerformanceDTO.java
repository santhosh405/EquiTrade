package com.example.demo.dto;

import java.util.List;

public class PortfolioPerformanceDTO {

    private String range;
    private double currentPortfolioValue;
    private double totalInvested;
    private double totalProfitLoss;
    private double totalProfitLossPercent;
    private String benchmarkName;
    private double benchmarkReturnPercent;
    private List<PerformancePointDTO> points;

    public PortfolioPerformanceDTO() {
    }

    public PortfolioPerformanceDTO(String range, double currentPortfolioValue, double totalInvested,
                                  double totalProfitLoss, double totalProfitLossPercent,
                                  String benchmarkName, double benchmarkReturnPercent,
                                  List<PerformancePointDTO> points) {
        this.range = range;
        this.currentPortfolioValue = currentPortfolioValue;
        this.totalInvested = totalInvested;
        this.totalProfitLoss = totalProfitLoss;
        this.totalProfitLossPercent = totalProfitLossPercent;
        this.benchmarkName = benchmarkName;
        this.benchmarkReturnPercent = benchmarkReturnPercent;
        this.points = points;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public double getCurrentPortfolioValue() {
        return currentPortfolioValue;
    }

    public void setCurrentPortfolioValue(double currentPortfolioValue) {
        this.currentPortfolioValue = currentPortfolioValue;
    }

    public double getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(double totalInvested) {
        this.totalInvested = totalInvested;
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

    public String getBenchmarkName() {
        return benchmarkName;
    }

    public void setBenchmarkName(String benchmarkName) {
        this.benchmarkName = benchmarkName;
    }

    public double getBenchmarkReturnPercent() {
        return benchmarkReturnPercent;
    }

    public void setBenchmarkReturnPercent(double benchmarkReturnPercent) {
        this.benchmarkReturnPercent = benchmarkReturnPercent;
    }

    public List<PerformancePointDTO> getPoints() {
        return points;
    }

    public void setPoints(List<PerformancePointDTO> points) {
        this.points = points;
    }
}
