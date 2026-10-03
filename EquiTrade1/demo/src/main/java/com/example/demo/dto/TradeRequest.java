package com.example.demo.dto;

public class TradeRequest {

    private Long userId;
    private Long stockId;
    private Long holdingId;
    private Long id;
    private int quantity;

    public TradeRequest() {
    }

    public TradeRequest(Long userId, Long stockId, int quantity) {
        this.userId = userId;
        this.stockId = stockId;
        this.id = stockId;
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStockId() {
        if (stockId != null) return stockId;
        return id;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
        if (this.id == null) {
            this.id = stockId;
        }
    }

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public Long getId() {
        if (id != null) return id;
        return stockId;
    }

    public void setId(Long id) {
        this.id = id;
        if (this.stockId == null) {
            this.stockId = id;
        }
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}