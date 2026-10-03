package com.example.demo.model;

import java.time.LocalDateTime;
import com.example.demo.model.enums.OrderStatus;
import com.example.demo.model.enums.OrderType;
import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    private Stock stock;

    private int quantity;

    private double price;

    private String type; // BUY, SELL

    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.EXECUTED;

    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.MARKET;

    private double limitPrice;

    private double brokerageFee = 0.0;

    private double taxes = 0.0;

    private double totalAmount;

    @Column(length = 2000)
    private String statusTimeline;

    @Column(length = 2000)
    private String investigationNotes;

    private String cancelledReason;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Transaction() {
    }

    public Transaction(Long id, User user, Stock stock, int quantity, double price, String type, LocalDateTime date) {
        this.id = id;
        this.user = user;
        this.stock = stock;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
        this.date = date;
        this.status = OrderStatus.EXECUTED;
        this.orderType = OrderType.MARKET;
        this.totalAmount = quantity * price;
        this.statusTimeline = "Placed and executed at " + (date != null ? date.toString() : LocalDateTime.now().toString());
        this.updatedAt = date != null ? date : LocalDateTime.now();
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(double limitPrice) {
        this.limitPrice = limitPrice;
    }

    public double getBrokerageFee() {
        return brokerageFee;
    }

    public void setBrokerageFee(double brokerageFee) {
        this.brokerageFee = brokerageFee;
    }

    public double getTaxes() {
        return taxes;
    }

    public void setTaxes(double taxes) {
        this.taxes = taxes;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatusTimeline() {
        return statusTimeline;
    }

    public void setStatusTimeline(String statusTimeline) {
        this.statusTimeline = statusTimeline;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}