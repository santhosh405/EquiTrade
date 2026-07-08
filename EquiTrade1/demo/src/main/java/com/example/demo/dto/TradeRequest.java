package com.example.demo.dto;

public class TradeRequest {

    private Long userId;
    private Long stockId;
    private int quantity;

    public TradeRequest() {
    }

    public TradeRequest(Long userId, Long stockId, int quantity) {
        this.userId = userId;
        this.stockId = stockId;
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}