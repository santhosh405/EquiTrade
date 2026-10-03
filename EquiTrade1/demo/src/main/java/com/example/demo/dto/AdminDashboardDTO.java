package com.example.demo.dto;

import java.util.List;

public class AdminDashboardDTO {

    private long totalUsers;
    private long totalTrades;
    private long totalStocks;
    private double totalTradeVolume;
    private List<MostTradedStockDTO> mostTradedStocks;

    // Enriched telemetry
    private long pendingAccessRequests;
    private long pendingKycCount;
    private long openRiskAlertsCount;
    private long ordersTodayCount;
    private long pendingWithdrawalsCount;

    public AdminDashboardDTO() {
    }

    public AdminDashboardDTO(long totalUsers, long totalTrades, long totalStocks, double totalTradeVolume, List<MostTradedStockDTO> mostTradedStocks) {
        this.totalUsers = totalUsers;
        this.totalTrades = totalTrades;
        this.totalStocks = totalStocks;
        this.totalTradeVolume = totalTradeVolume;
        this.mostTradedStocks = mostTradedStocks;
    }

    public AdminDashboardDTO(long totalUsers, long totalTrades, long totalStocks, double totalTradeVolume,
                             List<MostTradedStockDTO> mostTradedStocks, long pendingAccessRequests,
                             long pendingKycCount, long openRiskAlertsCount, long ordersTodayCount,
                             long pendingWithdrawalsCount) {
        this.totalUsers = totalUsers;
        this.totalTrades = totalTrades;
        this.totalStocks = totalStocks;
        this.totalTradeVolume = totalTradeVolume;
        this.mostTradedStocks = mostTradedStocks;
        this.pendingAccessRequests = pendingAccessRequests;
        this.pendingKycCount = pendingKycCount;
        this.openRiskAlertsCount = openRiskAlertsCount;
        this.ordersTodayCount = ordersTodayCount;
        this.pendingWithdrawalsCount = pendingWithdrawalsCount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(long totalTrades) {
        this.totalTrades = totalTrades;
    }

    public long getTotalStocks() {
        return totalStocks;
    }

    public void setTotalStocks(long totalStocks) {
        this.totalStocks = totalStocks;
    }

    public double getTotalTradeVolume() {
        return totalTradeVolume;
    }

    public void setTotalTradeVolume(double totalTradeVolume) {
        this.totalTradeVolume = totalTradeVolume;
    }

    public List<MostTradedStockDTO> getMostTradedStocks() {
        return mostTradedStocks;
    }

    public void setMostTradedStocks(List<MostTradedStockDTO> mostTradedStocks) {
        this.mostTradedStocks = mostTradedStocks;
    }

    public long getPendingAccessRequests() {
        return pendingAccessRequests;
    }

    public void setPendingAccessRequests(long pendingAccessRequests) {
        this.pendingAccessRequests = pendingAccessRequests;
    }

    public long getPendingKycCount() {
        return pendingKycCount;
    }

    public void setPendingKycCount(long pendingKycCount) {
        this.pendingKycCount = pendingKycCount;
    }

    public long getOpenRiskAlertsCount() {
        return openRiskAlertsCount;
    }

    public void setOpenRiskAlertsCount(long openRiskAlertsCount) {
        this.openRiskAlertsCount = openRiskAlertsCount;
    }

    public long getOrdersTodayCount() {
        return ordersTodayCount;
    }

    public void setOrdersTodayCount(long ordersTodayCount) {
        this.ordersTodayCount = ordersTodayCount;
    }

    public long getPendingWithdrawalsCount() {
        return pendingWithdrawalsCount;
    }

    public void setPendingWithdrawalsCount(long pendingWithdrawalsCount) {
        this.pendingWithdrawalsCount = pendingWithdrawalsCount;
    }
}
