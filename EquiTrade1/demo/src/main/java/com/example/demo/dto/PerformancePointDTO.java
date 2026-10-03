package com.example.demo.dto;

public class PerformancePointDTO {

    private String time;
    private long timestamp;
    private double portfolioValue;
    private double investedValue;
    private double benchmarkValue;
    private double portfolioReturnPercent;
    private double benchmarkReturnPercent;

    public PerformancePointDTO() {
    }

    public PerformancePointDTO(String time, long timestamp, double portfolioValue, double investedValue,
                               double benchmarkValue, double portfolioReturnPercent, double benchmarkReturnPercent) {
        this.time = time;
        this.timestamp = timestamp;
        this.portfolioValue = portfolioValue;
        this.investedValue = investedValue;
        this.benchmarkValue = benchmarkValue;
        this.portfolioReturnPercent = portfolioReturnPercent;
        this.benchmarkReturnPercent = benchmarkReturnPercent;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(double portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public double getInvestedValue() {
        return investedValue;
    }

    public void setInvestedValue(double investedValue) {
        this.investedValue = investedValue;
    }

    public double getBenchmarkValue() {
        return benchmarkValue;
    }

    public void setBenchmarkValue(double benchmarkValue) {
        this.benchmarkValue = benchmarkValue;
    }

    public double getPortfolioReturnPercent() {
        return portfolioReturnPercent;
    }

    public void setPortfolioReturnPercent(double portfolioReturnPercent) {
        this.portfolioReturnPercent = portfolioReturnPercent;
    }

    public double getBenchmarkReturnPercent() {
        return benchmarkReturnPercent;
    }

    public void setBenchmarkReturnPercent(double benchmarkReturnPercent) {
        this.benchmarkReturnPercent = benchmarkReturnPercent;
    }
}
