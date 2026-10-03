package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.FeeRule;
import com.example.demo.repository.FeeRuleRepository;

@Service
public class FeeCalculationService {

    @Autowired
    private FeeRuleRepository feeRuleRepository;

    public static class FeeBreakdown {
        private final double brokerage;
        private final double transactionCharges;
        private final double platformCharges;
        private final double gst;
        private final double stt;
        private final double totalFees;

        public FeeBreakdown(double brokerage, double transactionCharges, double platformCharges,
                            double gst, double stt) {
            this.brokerage = Math.round(brokerage * 100.0) / 100.0;
            this.transactionCharges = Math.round(transactionCharges * 100.0) / 100.0;
            this.platformCharges = Math.round(platformCharges * 100.0) / 100.0;
            this.gst = Math.round(gst * 100.0) / 100.0;
            this.stt = Math.round(stt * 100.0) / 100.0;
            this.totalFees = Math.round((this.brokerage + this.transactionCharges + this.platformCharges + this.gst + this.stt) * 100.0) / 100.0;
        }

        public double getBrokerage() { return brokerage; }
        public double getTransactionCharges() { return transactionCharges; }
        public double getPlatformCharges() { return platformCharges; }
        public double getGst() { return gst; }
        public double getStt() { return stt; }
        public double getTotalFees() { return totalFees; }
    }

    public FeeBreakdown calculateFees(double turnover, String transactionType) {
        FeeRule rule = getActiveFeeRule();

        double brokerage;
        if ("PERCENTAGE".equalsIgnoreCase(rule.getBrokerageType())) {
            brokerage = turnover * (rule.getBrokerageValue() / 100.0);
        } else {
            // Flat fee per executed order
            brokerage = rule.getBrokerageValue() > 0 ? rule.getBrokerageValue() : 20.0;
        }

        double txnCharges = turnover * (rule.getTransactionChargePercent() / 100.0);
        double platformCharges = rule.getPlatformCharge();

        // 18% GST applies to Brokerage + Transaction Charges + Platform Charges
        double taxableServices = brokerage + txnCharges + platformCharges;
        double gst = taxableServices * (rule.getGstPercent() / 100.0);

        // STT applies only to equity delivery turnover (or standard 0.1%)
        double stt = turnover * (rule.getSttPercent() / 100.0);

        return new FeeBreakdown(brokerage, txnCharges, platformCharges, gst, stt);
    }

    public FeeRule getActiveFeeRule() {
        Optional<FeeRule> ruleOpt = feeRuleRepository.findFirstByActiveTrueOrderByVersionDesc();
        if (ruleOpt.isPresent()) {
            return ruleOpt.get();
        }

        // Return a default rule if none seeded yet
        FeeRule defaultRule = new FeeRule();
        defaultRule.setVersion(1);
        defaultRule.setName("Standard EquiTrade Retail Pricing v1");
        defaultRule.setBrokerageType("FLAT");
        defaultRule.setBrokerageValue(20.0);
        defaultRule.setTransactionChargePercent(0.00345);
        defaultRule.setPlatformCharge(0.0);
        defaultRule.setGstPercent(18.0);
        defaultRule.setSttPercent(0.1);
        defaultRule.setActive(true);
        defaultRule.setReasonForChange("System initial baseline tariff");
        defaultRule.setCreatedByAdminEmail("system@equitrade.com");
        defaultRule.setCreatedAt(LocalDateTime.now());
        return defaultRule;
    }
}
