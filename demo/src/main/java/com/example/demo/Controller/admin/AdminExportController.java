package com.example.demo.Controller.admin;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.ExcelExportService;

@RestController
@RequestMapping({"/admin/export", "/api/admin/export"})
public class AdminExportController {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private FeeRuleRepository feeRuleRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private SecurityIncidentRepository securityIncidentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private RegulatoryCommunicationRepository regulatoryCommunicationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    private ResponseEntity<byte[]> serveExcel(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportUsers(HttpServletRequest request) throws IOException {
        List<User> list = userRepository.findAll();
        byte[] data = excelExportService.exportUsers(list);
        auditLogService.logAction("EXPORT_EXCEL", "USER", "ALL", null, list.size() + " rows", "Exported Users Excel", request);
        return serveExcel(data, "EquiTrade_Users_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/kyc")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportKyc(HttpServletRequest request) throws IOException {
        List<KycDocument> list = kycDocumentRepository.findAll();
        byte[] data = excelExportService.exportKyc(list);
        auditLogService.logAction("EXPORT_EXCEL", "KYC", "ALL", null, list.size() + " rows", "Exported KYC Excel", request);
        return serveExcel(data, "EquiTrade_KYC_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportOrders(HttpServletRequest request) throws IOException {
        List<Transaction> list = transactionRepository.findAllByOrderByDateDesc();
        byte[] data = excelExportService.exportOrders(list);
        auditLogService.logAction("EXPORT_EXCEL", "ORDER", "ALL", null, list.size() + " rows", "Exported Orders Excel", request);
        return serveExcel(data, "EquiTrade_Orders_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/wallet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<byte[]> exportWallet(HttpServletRequest request) throws IOException {
        List<WalletTransaction> list = walletTransactionRepository.findAllByOrderByCreatedAtDesc();
        byte[] data = excelExportService.exportWalletTransactions(list);
        auditLogService.logAction("EXPORT_EXCEL", "WALLET", "ALL", null, list.size() + " rows", "Exported Wallet Transactions Excel", request);
        return serveExcel(data, "EquiTrade_Wallet_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/portfolios")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE', 'SUPPORT')")
    public ResponseEntity<byte[]> exportPortfolios(HttpServletRequest request) throws IOException {
        List<Portfolio> list = portfolioRepository.findAll();
        byte[] data = excelExportService.exportPortfolios(list);
        auditLogService.logAction("EXPORT_EXCEL", "PORTFOLIO", "ALL", null, list.size() + " rows", "Exported Portfolios Excel", request);
        return serveExcel(data, "EquiTrade_Portfolios_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/charges")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<byte[]> exportCharges(HttpServletRequest request) throws IOException {
        List<FeeRule> list = feeRuleRepository.findAllByOrderByVersionDesc();
        byte[] data = excelExportService.exportFeeRules(list);
        auditLogService.logAction("EXPORT_EXCEL", "FEE_RULE", "ALL", null, list.size() + " rows", "Exported Fee Rules Excel", request);
        return serveExcel(data, "EquiTrade_FeeRules_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/risk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<byte[]> exportRisk(HttpServletRequest request) throws IOException {
        List<RiskAlert> list = riskAlertRepository.findAllByOrderByCreatedAtDesc();
        byte[] data = excelExportService.exportRiskAlerts(list);
        auditLogService.logAction("EXPORT_EXCEL", "RISK_ALERT", "ALL", null, list.size() + " rows", "Exported Risk Alerts Excel", request);
        return serveExcel(data, "EquiTrade_RiskAlerts_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/fraud")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportFraud(HttpServletRequest request) throws IOException {
        List<SecurityIncident> list = securityIncidentRepository.findAllByOrderByCreatedAtDesc();
        byte[] data = excelExportService.exportSecurityIncidents(list);
        auditLogService.logAction("EXPORT_EXCEL", "SECURITY_INCIDENT", "ALL", null, list.size() + " rows", "Exported Security Incidents Excel", request);
        return serveExcel(data, "EquiTrade_Incidents_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportCompliance(HttpServletRequest request) throws IOException {
        List<Complaint> complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
        List<RegulatoryCommunication> comms = regulatoryCommunicationRepository.findAllByOrderByTimestampDesc();
        byte[] data = excelExportService.exportCompliance(complaints, comms);
        auditLogService.logAction("EXPORT_EXCEL", "COMPLIANCE", "ALL", null, (complaints.size() + comms.size()) + " rows", "Exported Compliance Excel", request);
        return serveExcel(data, "EquiTrade_Compliance_" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportAuditLogs(HttpServletRequest request) throws IOException {
        List<AuditLog> list = auditLogRepository.findAllByOrderByTimestampDesc();
        byte[] data = excelExportService.exportAuditLogs(list);
        auditLogService.logAction("EXPORT_EXCEL", "AUDIT_LOG", "ALL", null, list.size() + " rows", "Exported Audit Trail Excel", request);
        return serveExcel(data, "EquiTrade_AuditLogs_" + System.currentTimeMillis() + ".xlsx");
    }

    // FULL SYSTEM BACKUP EXPORT: Multi-sheet workbook available to SUPER_ADMIN only
    @GetMapping("/backup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportFullBackup(HttpServletRequest request) throws IOException {
        List<User> users = userRepository.findAll();
        List<Transaction> orders = transactionRepository.findAllByOrderByDateDesc();
        List<KycDocument> documents = kycDocumentRepository.findAll();
        List<WalletTransaction> wallet = walletTransactionRepository.findAllByOrderByCreatedAtDesc();
        List<Portfolio> portfolios = portfolioRepository.findAll();
        List<FeeRule> feeRules = feeRuleRepository.findAllByOrderByVersionDesc();
        List<RiskAlert> riskAlerts = riskAlertRepository.findAllByOrderByCreatedAtDesc();
        List<SecurityIncident> fraud = securityIncidentRepository.findAllByOrderByCreatedAtDesc();
        List<Complaint> complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
        List<RegulatoryCommunication> comms = regulatoryCommunicationRepository.findAllByOrderByTimestampDesc();
        List<AuditLog> auditLogs = auditLogRepository.findAllByOrderByTimestampDesc();

        byte[] data = excelExportService.exportFullBackup(
                users, orders, documents, wallet, portfolios, feeRules, riskAlerts, fraud, complaints, comms, auditLogs
        );

        int totalRecords = users.size() + orders.size() + documents.size() + wallet.size() +
                           portfolios.size() + feeRules.size() + riskAlerts.size() + fraud.size() +
                           complaints.size() + comms.size() + auditLogs.size();

        auditLogService.logAction("FULL_BACKUP_EXPORT", "SYSTEM", "ALL_MODULES", null, totalRecords + " total records", "Full system backup workbook exported", request);

        return serveExcel(data, "EquiTrade_FULL_SYSTEM_BACKUP_" + System.currentTimeMillis() + ".xlsx");
    }
}
