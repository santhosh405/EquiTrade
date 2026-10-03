package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "fee_rules")
public class FeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brokerageType; // FLAT or PERCENTAGE

    private double brokerageValue; // e.g. 20.0 (flat INR) or 0.05 (%)

    private double transactionChargePercent = 0.00345; // 0.00345% NSE turn-over charges

    private double platformCharge = 0.0; // Flat INR per order

    private double gstPercent = 18.0; // 18% GST on brokerage + txn charge

    private double sttPercent = 0.1; // 0.1% Securities Transaction Tax on delivery

    private double stampDutyPercent = 0.015; // 0.015%

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean active = true;

    @Column(length = 1000)
    private String reasonForChange;

    private String createdByAdminEmail;

    private LocalDateTime createdAt = LocalDateTime.now();

    public FeeRule() {
    }

    public FeeRule(int version, String name, String brokerageType, double brokerageValue,
                   double transactionChargePercent, double platformCharge, double gstPercent,
                   double sttPercent, String reasonForChange, String createdByAdminEmail) {
        this.version = version;
        this.name = name;
        this.brokerageType = brokerageType;
        this.brokerageValue = brokerageValue;
        this.transactionChargePercent = transactionChargePercent;
        this.platformCharge = platformCharge;
        this.gstPercent = gstPercent;
        this.sttPercent = sttPercent;
        this.reasonForChange = reasonForChange;
        this.createdByAdminEmail = createdByAdminEmail;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrokerageType() {
        return brokerageType;
    }

    public void setBrokerageType(String brokerageType) {
        this.brokerageType = brokerageType;
    }

    public double getBrokerageValue() {
        return brokerageValue;
    }

    public void setBrokerageValue(double brokerageValue) {
        this.brokerageValue = brokerageValue;
    }

    public double getTransactionChargePercent() {
        return transactionChargePercent;
    }

    public void setTransactionChargePercent(double transactionChargePercent) {
        this.transactionChargePercent = transactionChargePercent;
    }

    public double getPlatformCharge() {
        return platformCharge;
    }

    public void setPlatformCharge(double platformCharge) {
        this.platformCharge = platformCharge;
    }

    public double getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(double gstPercent) {
        this.gstPercent = gstPercent;
    }

    public double getSttPercent() {
        return sttPercent;
    }

    public void setSttPercent(double sttPercent) {
        this.sttPercent = sttPercent;
    }

    public double getStampDutyPercent() {
        return stampDutyPercent;
    }

    public void setStampDutyPercent(double stampDutyPercent) {
        this.stampDutyPercent = stampDutyPercent;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getReasonForChange() {
        return reasonForChange;
    }

    public void setReasonForChange(String reasonForChange) {
        this.reasonForChange = reasonForChange;
    }

    public String getCreatedByAdminEmail() {
        return createdByAdminEmail;
    }

    public void setCreatedByAdminEmail(String createdByAdminEmail) {
        this.createdByAdminEmail = createdByAdminEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
