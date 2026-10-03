package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "corporate_actions")
public class CorporateAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private String actionType; // DIVIDEND, SPLIT, BONUS

    private double ratioOrAmount; // Dividend per share (e.g. 15.0) or split ratio (e.g. 2 for 1:2)

    private String description;

    private int affectedHoldingsCount = 0;

    private double totalDisbursedOrAdjusted = 0.0;

    private String executedByAdminEmail;

    private LocalDateTime executionDate = LocalDateTime.now();

    public CorporateAction() {
    }

    public CorporateAction(Stock stock, String actionType, double ratioOrAmount, String description, String executedByAdminEmail) {
        this.stock = stock;
        this.actionType = actionType;
        this.ratioOrAmount = ratioOrAmount;
        this.description = description;
        this.executedByAdminEmail = executedByAdminEmail;
        this.executionDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public double getRatioOrAmount() {
        return ratioOrAmount;
    }

    public void setRatioOrAmount(double ratioOrAmount) {
        this.ratioOrAmount = ratioOrAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAffectedHoldingsCount() {
        return affectedHoldingsCount;
    }

    public void setAffectedHoldingsCount(int affectedHoldingsCount) {
        this.affectedHoldingsCount = affectedHoldingsCount;
    }

    public double getTotalDisbursedOrAdjusted() {
        return totalDisbursedOrAdjusted;
    }

    public void setTotalDisbursedOrAdjusted(double totalDisbursedOrAdjusted) {
        this.totalDisbursedOrAdjusted = totalDisbursedOrAdjusted;
    }

    public String getExecutedByAdminEmail() {
        return executedByAdminEmail;
    }

    public void setExecutedByAdminEmail(String executedByAdminEmail) {
        this.executedByAdminEmail = executedByAdminEmail;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }
}
