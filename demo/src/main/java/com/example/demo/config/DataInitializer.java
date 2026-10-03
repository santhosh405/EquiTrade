package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.model.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceHistoryRepository stockPriceHistoryRepository;

    @Autowired
    private com.example.demo.service.StockService stockService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private FeeRuleRepository feeRuleRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private SecurityIncidentRepository securityIncidentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private KycTimelineRepository kycTimelineRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Autowired
    private RegulatoryCommunicationRepository regulatoryCommunicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUsers();
        initStocks();
        initFeeRules();
        initPortfolioAndTrades();
        initRiskAlerts();
        initSecurityIncidents();
        initComplaints();
        initRegulatoryCommunications();
    }

    private void initUsers() {
        // Super Admins
        createOrUpdateUser("superadmin@equitrade.com", "superadmin", "superadmin123", "ROLE_SUPER_ADMIN", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 1000000.0, "9876543210");
        createOrUpdateUser("admin@equitrade.com", "admin", "admin123", "ROLE_SUPER_ADMIN", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 500000.0, "9876543211");

        // Role-Specific Admins
        createOrUpdateUser("kyc@equitrade.com", "kyc_officer", "kyc123", "ROLE_KYC_OFFICER", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 200000.0, "9876543212");
        createOrUpdateUser("support@equitrade.com", "support_agent", "support123", "ROLE_SUPPORT", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 200000.0, "9876543213");
        createOrUpdateUser("risk@equitrade.com", "risk_officer", "risk123", "ROLE_RISK_OFFICER", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 200000.0, "9876543214");
        createOrUpdateUser("finance@equitrade.com", "finance_manager", "finance123", "ROLE_FINANCE", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 500000.0, "9876543215");
        createOrUpdateUser("compliance@equitrade.com", "compliance_officer", "compliance123", "ROLE_COMPLIANCE_OFFICER", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 200000.0, "9876543216");

        // Regular Users in various statuses
        User trader = createOrUpdateUser("trader@equitrade.com", "rahul_trader", "trader123", "ROLE_USER", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.ACTIVE, null, 250000.0, "9811223344");
        trader.setPanNumber("ABCDE1234F");
        trader.setMaskedAadhaar("XXXX-XXXX-9876");
        userRepository.save(trader);

        createOrUpdateUser("demo@equitrade.com", "demo_user", "demo123", "ROLE_USER", AccessStatus.APPROVED, KycStatus.NOT_SUBMITTED, AccountStatus.ACTIVE, null, 100000.0, "9811223355");
        createOrUpdateUser("pending@equitrade.com", "new_applicant", "pending123", "ROLE_USER", AccessStatus.PENDING_APPROVAL, KycStatus.PENDING, AccountStatus.ACTIVE, null, 50000.0, "9811223366");
        createOrUpdateUser("declined@equitrade.com", "declined_applicant", "declined123", "ROLE_USER", AccessStatus.DECLINED, KycStatus.REJECTED, AccountStatus.ACTIVE, "Failed regulatory check", 0.0, "9811223377");
        createOrUpdateUser("frozen@equitrade.com", "frozen_trader", "frozen123", "ROLE_USER", AccessStatus.APPROVED, KycStatus.APPROVED, AccountStatus.FROZEN, "Suspicious circular trading pattern flagged", 150000.0, "9811223388");

        // Seed KYC Document and timeline for trader
        if (kycDocumentRepository.count() == 0) {
            KycDocument doc1 = new KycDocument(trader, "PAN", "pan_card.pdf", "application/pdf", 102400, "storage/pan_card.pdf", "ABXXXXX234F");
            doc1.setStatus("VERIFIED");
            doc1.setVerifiedByAdminEmail("kyc@equitrade.com");
            kycDocumentRepository.save(doc1);

            KycDocument doc2 = new KycDocument(trader, "AADHAAR_FRONT", "aadhaar_front.jpg", "image/jpeg", 204800, "storage/aadhaar_front.jpg", "XXXX-XXXX-9876");
            doc2.setStatus("VERIFIED");
            doc2.setVerifiedByAdminEmail("kyc@equitrade.com");
            kycDocumentRepository.save(doc2);

            kycTimelineRepository.save(new KycTimeline(trader, 3L, "kyc@equitrade.com", "APPROVED", "PENDING", "APPROVED", "Identity verified successfully"));
        }
    }

    private User createOrUpdateUser(String email, String username, String password, String role,
                                    AccessStatus accessStatus, KycStatus kycStatus, AccountStatus accountStatus,
                                    String declineReason, double balance, String phone) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            // If seeded user has plain text password, hash it with BCrypt
            if (user.getPassword() == null || !user.getPassword().startsWith("$2")) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
            return user;
        }
        user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccessStatus(accessStatus);
        user.setKycStatus(kycStatus);
        user.setAccountStatus(accountStatus);
        user.setDeclineReason(declineReason);
        user.setBalance(balance);
        user.setPhone(phone);
        user.setCreatedAt(LocalDateTime.now().minusDays(10));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void initStocks() {
        if (stockRepository.count() >= 20) {
            return;
        }

        Object[][] stockData = {
            {"TCS", "Tata Consultancy Services", 3650.50, 1.80, "IT", "NSE", "EQUITY"},
            {"INFY", "Infosys", 1480.75, -0.65, "IT", "NSE", "EQUITY"},
            {"RELIANCE", "Reliance Industries", 2750.25, 2.15, "Energy", "NSE", "EQUITY"},
            {"HDFCBANK", "HDFC Bank", 1655.80, 0.45, "Banking", "NSE", "EQUITY"},
            {"WIPRO", "Wipro", 495.60, -1.20, "IT", "BSE", "EQUITY"},
            {"ICICIBANK", "ICICI Bank", 1085.30, 1.65, "Banking", "NSE", "EQUITY"},
            {"TATAMOTORS", "Tata Motors", 940.10, 3.40, "Auto", "NSE", "EQUITY"},
            {"BHARTIARTL", "Bharti Airtel", 1210.00, 0.80, "Telecom", "NSE", "EQUITY"},
            {"ITC", "ITC Limited", 435.50, -0.30, "FMCG", "NSE", "EQUITY"},
            {"SBIN", "State Bank of India", 765.20, 1.10, "Banking", "NSE", "EQUITY"},
            {"SUNPHARMA", "Sun Pharmaceutical", 1520.40, 0.95, "Pharma", "NSE", "EQUITY"},
            {"MARUTI", "Maruti Suzuki", 12450.00, -0.75, "Auto", "NSE", "EQUITY"},
            {"LNT", "Larsen & Toubro", 3580.00, 1.45, "Infrastructure", "NSE", "EQUITY"},
            {"BAJFINANCE", "Bajaj Finance", 6890.00, -1.85, "Finance", "NSE", "EQUITY"},
            {"TATASTEEL", "Tata Steel", 156.40, 2.80, "Metals", "BSE", "EQUITY"},
            {"HINDUNILVR", "Hindustan Unilever", 2380.00, -0.40, "FMCG", "NSE", "EQUITY"},
            {"ADANIENT", "Adani Enterprises", 3120.00, 4.25, "Energy", "NSE", "EQUITY"},
            {"KOTAKBANK", "Kotak Mahindra Bank", 1780.00, -0.55, "Banking", "NSE", "EQUITY"},
            {"TITAN", "Titan Company", 3420.00, 1.30, "Consumer", "NSE", "EQUITY"},
            {"NIFTYBEES", "Nippon India Nifty 50 ETF", 265.40, 0.65, "ETF", "NSE", "ETF"}
        };

        LocalDateTime now = LocalDateTime.now();

        for (Object[] data : stockData) {
            String symbol = (String) data[0];
            String name = (String) data[1];
            double price = (Double) data[2];
            double change = (Double) data[3];
            String sector = (String) data[4];
            String exchangeStr = (String) data[5];
            String typeStr = (String) data[6];

            Stock stock = stockRepository.findBySymbolIgnoreCase(symbol).orElse(null);
            if (stock == null) {
                stock = new Stock();
                stock.setSymbol(symbol);
            }
            stock.setCompanyName(name);
            stock.setPrice(price);
            stock.setChangePercent(change);
            stock.setSector(sector);
            stock.setExchange(Exchange.valueOf(exchangeStr));
            stock.setInstrumentType(InstrumentType.valueOf(typeStr));
            stock.setInstrumentStatus(InstrumentStatus.ACTIVE);
            stock = stockRepository.save(stock);

            stockService.ensureStockHistorySeeded(stock);
        }
    }


    private void initFeeRules() {
        if (feeRuleRepository.count() == 0) {
            FeeRule rule1 = new FeeRule(
                    1, "Standard Retail Brokerage Plan", "FLAT", 20.0,
                    0.00345, 0.0, 18.0, 0.1,
                    "Initial base tariff structure", "superadmin@equitrade.com"
            );
            feeRuleRepository.save(rule1);
        }
    }

    private void initPortfolioAndTrades() {
        if (portfolioRepository.count() > 0) {
            return;
        }

        User trader = userRepository.findByEmail("trader@equitrade.com").orElse(null);
        if (trader == null) return;

        Stock tcs = stockRepository.findBySymbolIgnoreCase("TCS").orElse(null);
        Stock rel = stockRepository.findBySymbolIgnoreCase("RELIANCE").orElse(null);
        Stock infy = stockRepository.findBySymbolIgnoreCase("INFY").orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (tcs != null) {
            portfolioRepository.save(new Portfolio(null, trader, tcs, 15, 3500.0));
            Transaction t1 = new Transaction(null, trader, tcs, 15, 3500.0, "BUY", now.minusDays(5));
            t1.setStatus(OrderStatus.EXECUTED);
            t1.setOrderType(OrderType.MARKET);
            t1.setBrokerageFee(20.0);
            t1.setTaxes(62.5);
            t1.setTotalAmount(52582.5);
            transactionRepository.save(t1);
            watchlistRepository.save(new Watchlist(trader, tcs));
        }

        if (rel != null) {
            portfolioRepository.save(new Portfolio(null, trader, rel, 20, 2680.0));
            Transaction t2 = new Transaction(null, trader, rel, 20, 2680.0, "BUY", now.minusDays(3));
            t2.setStatus(OrderStatus.EXECUTED);
            t2.setOrderType(OrderType.MARKET);
            t2.setBrokerageFee(20.0);
            t2.setTaxes(64.1);
            t2.setTotalAmount(53684.1);
            transactionRepository.save(t2);
            watchlistRepository.save(new Watchlist(trader, rel));
        }

        if (infy != null) {
            portfolioRepository.save(new Portfolio(null, trader, infy, 25, 1490.0));
            Transaction t3 = new Transaction(null, trader, infy, 25, 1490.0, "BUY", now.minusDays(1));
            t3.setStatus(OrderStatus.EXECUTED);
            t3.setOrderType(OrderType.MARKET);
            t3.setBrokerageFee(20.0);
            t3.setTaxes(44.5);
            t3.setTotalAmount(37314.5);
            transactionRepository.save(t3);
            watchlistRepository.save(new Watchlist(trader, infy));
        }

        // Seed sample pending order and failed order
        if (tcs != null) {
            Transaction pending = new Transaction(null, trader, tcs, 5, 3600.0, "BUY", now.minusHours(2));
            pending.setStatus(OrderStatus.PENDING);
            pending.setOrderType(OrderType.LIMIT);
            pending.setLimitPrice(3600.0);
            pending.setStatusTimeline("Order placed at " + now.minusHours(2) + " awaiting execution matching");
            transactionRepository.save(pending);

            Transaction rejected = new Transaction(null, trader, tcs, 50, 3650.0, "BUY", now.minusHours(8));
            rejected.setStatus(OrderStatus.REJECTED);
            rejected.setInvestigationNotes("Margin shortfall detected at time of limit placement");
            rejected.setStatusTimeline("Order placed -> Rejected by risk validation engine due to insufficient available margin");
            transactionRepository.save(rejected);
        }

        // Seed sample withdrawal requests
        WithdrawalRequest w1 = new WithdrawalRequest(trader, 25000.0, "918237461234", "HDFC0001234", "HDFC Bank Ltd");
        w1.setStatus(WithdrawalStatus.PENDING_REVIEW);
        withdrawalRequestRepository.save(w1);
    }

    private void initRiskAlerts() {
        if (riskAlertRepository.count() == 0) {
            User trader = userRepository.findByEmail("trader@equitrade.com").orElse(null);
            Long userId = trader != null ? trader.getId() : 1L;

            RiskAlert a1 = new RiskAlert(userId, "trader@equitrade.com", "ABNORMAL_ORDER_SIZE",
                    RiskAlertSeverity.HIGH, "Single order volume of 500 TCS shares exceeded user 30-day average by 400%");
            riskAlertRepository.save(a1);

            RiskAlert a2 = new RiskAlert(userId, "trader@equitrade.com", "EXPOSURE_LIMIT_EXCEEDED",
                    RiskAlertSeverity.MEDIUM, "Total portfolio exposure across Energy sector reached 42% (Limit: 40%)");
            riskAlertRepository.save(a2);
        }
    }

    private void initSecurityIncidents() {
        if (securityIncidentRepository.count() == 0) {
            SecurityIncident s1 = new SecurityIncident(
                    "MULTIPLE_DEVICE_COLLISION", "USER", "2", "MEDIUM",
                    "Simultaneous sessions established from two distinct IP subnets (Bengaluru and Mumbai) within 4 minutes."
            );
            s1.setAssignedToAdminEmail("risk@equitrade.com");
            securityIncidentRepository.save(s1);
        }
    }

    private void initComplaints() {
        if (complaintRepository.count() == 0) {
            User trader = userRepository.findByEmail("trader@equitrade.com").orElse(null);
            Long userId = trader != null ? trader.getId() : 1L;

            Complaint c1 = new Complaint(
                    userId, "trader@equitrade.com", "Delayed contract note generation for TCS trade",
                    "Contract note email not received within 24 hours of execution for order #1",
                    "TRADING", "MEDIUM", 48
            );
            c1.setAssignedToAdminEmail("support@equitrade.com");
            complaintRepository.save(c1);
        }
    }

    private void initRegulatoryCommunications() {
        if (regulatoryCommunicationRepository.count() == 0) {
            RegulatoryCommunication r1 = new RegulatoryCommunication(
                    "SEBI", "SEBI/HO/MIRSD/2026/089", "Quarterly Client Funds Settlement Telemetry Report",
                    "OUTBOUND", "COMPLIED", "Submitted compliance report for Q2 client bank balance reconciliation",
                    "compliance@equitrade.com"
            );
            regulatoryCommunicationRepository.save(r1);
        }
    }
}
