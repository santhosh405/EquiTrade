package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    private Stock stock;

    private int quantity;

    private double averageBuyPrice;

    public Portfolio() {
    }

    public Portfolio(Long id, User user, Stock stock, int quantity) {
        this.id = id;
        this.user = user;
        this.stock = stock;
        this.quantity = quantity;
        this.averageBuyPrice = stock != null ? stock.getPrice() : 0.0;
    }

    public Portfolio(Long id, User user, Stock stock, int quantity, double averageBuyPrice) {
        this.id = id;
        this.user = user;
        this.stock = stock;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAverageBuyPrice() {
        if (averageBuyPrice <= 0 && stock != null) {
            return stock.getPrice();
        }
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(double averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }
}