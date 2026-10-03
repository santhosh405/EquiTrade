package com.example.demo.dto;

import java.time.LocalDateTime;

public class TransactionDTO {

    private Long id;
    private String type;
    private Long stockId;
    private String stockSymbol;
    private String companyName;
    private int quantity;
    private double price;
    private double total;
    private LocalDateTime date;

    public TransactionDTO() {
    }

    public TransactionDTO(Long id, String type, Long stockId, String stockSymbol, String companyName, int quantity, double price, LocalDateTime date) {
        this.id = id;
        this.type = type;
        this.stockId = stockId;
        this.stockSymbol = stockSymbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.price = price;
        this.total = quantity * price;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
