package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.demo.model.*;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₹#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private void finalizeSheet(Sheet sheet, int colCount) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public byte[] exportUsers(List<User> users) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildUsersSheet(workbook, users);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildUsersSheet(Workbook workbook, List<User> users) {
        Sheet sheet = workbook.createSheet("Users");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currStyle = createCurrencyStyle(workbook);

        String[] headers = {"User ID", "Username", "Email", "Phone", "Role", "Access Status", "Account Status", "KYC Status", "Risk Category", "Cash Balance", "Created At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (User u : users) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(u.getId());
            row.createCell(1).setCellValue(u.getUsername() != null ? u.getUsername() : "");
            row.createCell(2).setCellValue(u.getEmail());
            row.createCell(3).setCellValue(u.getPhone() != null ? u.getPhone() : "");
            row.createCell(4).setCellValue(u.getRole());
            row.createCell(5).setCellValue(u.getAccessStatus() != null ? u.getAccessStatus().name() : "");
            row.createCell(6).setCellValue(u.getAccountStatus() != null ? u.getAccountStatus().name() : "");
            row.createCell(7).setCellValue(u.getKycStatus() != null ? u.getKycStatus().name() : "");
            row.createCell(8).setCellValue(u.getRiskCategory() != null ? u.getRiskCategory().name() : "");

            Cell balCell = row.createCell(9);
            balCell.setCellValue(u.getBalance());
            balCell.setCellStyle(currStyle);

            row.createCell(10).setCellValue(u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportOrders(List<Transaction> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildOrdersSheet(workbook, orders);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildOrdersSheet(Workbook workbook, List<Transaction> orders) {
        Sheet sheet = workbook.createSheet("Orders");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currStyle = createCurrencyStyle(workbook);

        String[] headers = {"Order ID", "Date", "User Email", "Symbol", "Side", "Quantity", "Price", "Brokerage Fee", "Taxes & Levies", "Total Amount", "Status", "Order Type", "Notes"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Transaction t : orders) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(t.getId());
            row.createCell(1).setCellValue(t.getDate() != null ? t.getDate().format(DATE_FMT) : "");
            row.createCell(2).setCellValue(t.getUser() != null ? t.getUser().getEmail() : "");
            row.createCell(3).setCellValue(t.getStock() != null ? t.getStock().getSymbol() : "");
            row.createCell(4).setCellValue(t.getType());
            row.createCell(5).setCellValue(t.getQuantity());

            Cell priceCell = row.createCell(6);
            priceCell.setCellValue(t.getPrice());
            priceCell.setCellStyle(currStyle);

            Cell brokCell = row.createCell(7);
            brokCell.setCellValue(t.getBrokerageFee());
            brokCell.setCellStyle(currStyle);

            Cell taxCell = row.createCell(8);
            taxCell.setCellValue(t.getTaxes());
            taxCell.setCellStyle(currStyle);

            Cell totCell = row.createCell(9);
            totCell.setCellValue(t.getTotalAmount());
            totCell.setCellStyle(currStyle);

            row.createCell(10).setCellValue(t.getStatus() != null ? t.getStatus().name() : "EXECUTED");
            row.createCell(11).setCellValue(t.getOrderType() != null ? t.getOrderType().name() : "MARKET");
            row.createCell(12).setCellValue(t.getInvestigationNotes() != null ? t.getInvestigationNotes() : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportKyc(List<KycDocument> documents) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildKycSheet(workbook, documents);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildKycSheet(Workbook workbook, List<KycDocument> documents) {
        Sheet sheet = workbook.createSheet("KYC Records");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Doc ID", "User ID", "User Email", "Doc Type", "Masked Number", "File Name", "Status", "Remarks", "Uploaded At", "Verified By"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (KycDocument d : documents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getId());
            row.createCell(1).setCellValue(d.getUser() != null ? d.getUser().getId() : 0);
            row.createCell(2).setCellValue(d.getUser() != null ? d.getUser().getEmail() : "");
            row.createCell(3).setCellValue(d.getDocumentType());
            row.createCell(4).setCellValue(d.getMaskedDocumentNumber() != null ? d.getMaskedDocumentNumber() : "");
            row.createCell(5).setCellValue(d.getFileName() != null ? d.getFileName() : "");
            row.createCell(6).setCellValue(d.getStatus());
            row.createCell(7).setCellValue(d.getRemarks() != null ? d.getRemarks() : "");
            row.createCell(8).setCellValue(d.getUploadedAt() != null ? d.getUploadedAt().format(DATE_FMT) : "");
            row.createCell(9).setCellValue(d.getVerifiedByAdminEmail() != null ? d.getVerifiedByAdminEmail() : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportWalletTransactions(List<WalletTransaction> list) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildWalletSheet(workbook, list);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildWalletSheet(Workbook workbook, List<WalletTransaction> list) {
        Sheet sheet = workbook.createSheet("Wallet Transactions");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currStyle = createCurrencyStyle(workbook);

        String[] headers = {"Txn ID", "Timestamp", "User Email", "Type", "Amount", "Balance After", "Status", "Reference No", "Method", "Remarks"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (WalletTransaction w : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(w.getId());
            row.createCell(1).setCellValue(w.getCreatedAt() != null ? w.getCreatedAt().format(DATE_FMT) : "");
            row.createCell(2).setCellValue(w.getUser() != null ? w.getUser().getEmail() : "");
            row.createCell(3).setCellValue(w.getType());

            Cell amtCell = row.createCell(4);
            amtCell.setCellValue(w.getAmount());
            amtCell.setCellStyle(currStyle);

            Cell balCell = row.createCell(5);
            balCell.setCellValue(w.getBalanceAfter());
            balCell.setCellStyle(currStyle);

            row.createCell(6).setCellValue(w.getStatus());
            row.createCell(7).setCellValue(w.getReferenceNumber() != null ? w.getReferenceNumber() : "");
            row.createCell(8).setCellValue(w.getPaymentMethod() != null ? w.getPaymentMethod() : "");
            row.createCell(9).setCellValue(w.getRemarks() != null ? w.getRemarks() : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportPortfolios(List<Portfolio> holdings) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildPortfolioSheet(workbook, holdings);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildPortfolioSheet(Workbook workbook, List<Portfolio> holdings) {
        Sheet sheet = workbook.createSheet("Portfolios");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currStyle = createCurrencyStyle(workbook);

        String[] headers = {"Holding ID", "User Email", "Symbol", "Company", "Quantity", "Avg Buy Price", "Current Price", "Current Value", "P&L", "P&L %"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Portfolio p : holdings) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getUser() != null ? p.getUser().getEmail() : "");
            row.createCell(2).setCellValue(p.getStock() != null ? p.getStock().getSymbol() : "");
            row.createCell(3).setCellValue(p.getStock() != null ? p.getStock().getCompanyName() : "");
            row.createCell(4).setCellValue(p.getQuantity());

            Cell avgCell = row.createCell(5);
            avgCell.setCellValue(p.getAverageBuyPrice());
            avgCell.setCellStyle(currStyle);

            double curPrice = p.getStock() != null ? p.getStock().getPrice() : 0.0;
            Cell curCell = row.createCell(6);
            curCell.setCellValue(curPrice);
            curCell.setCellStyle(currStyle);

            double curVal = p.getQuantity() * curPrice;
            Cell valCell = row.createCell(7);
            valCell.setCellValue(curVal);
            valCell.setCellStyle(currStyle);

            double costVal = p.getQuantity() * p.getAverageBuyPrice();
            double pnl = curVal - costVal;
            double pnlPct = costVal > 0 ? (pnl / costVal) * 100.0 : 0.0;

            Cell pnlCell = row.createCell(8);
            pnlCell.setCellValue(pnl);
            pnlCell.setCellStyle(currStyle);

            row.createCell(9).setCellValue(Math.round(pnlPct * 100.0) / 100.0 + "%");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportFeeRules(List<FeeRule> rules) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildFeeRulesSheet(workbook, rules);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildFeeRulesSheet(Workbook workbook, List<FeeRule> rules) {
        Sheet sheet = workbook.createSheet("Fee Rules");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Version", "Rule Name", "Brokerage Type", "Brokerage Value", "Turnover Charge %", "Platform Charge", "GST %", "STT %", "Active", "Reason For Change", "Created At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (FeeRule f : rules) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(f.getVersion());
            row.createCell(1).setCellValue(f.getName());
            row.createCell(2).setCellValue(f.getBrokerageType());
            row.createCell(3).setCellValue(f.getBrokerageValue());
            row.createCell(4).setCellValue(f.getTransactionChargePercent());
            row.createCell(5).setCellValue(f.getPlatformCharge());
            row.createCell(6).setCellValue(f.getGstPercent());
            row.createCell(7).setCellValue(f.getSttPercent());
            row.createCell(8).setCellValue(f.isActive() ? "YES" : "NO");
            row.createCell(9).setCellValue(f.getReasonForChange() != null ? f.getReasonForChange() : "");
            row.createCell(10).setCellValue(f.getCreatedAt() != null ? f.getCreatedAt().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportRiskAlerts(List<RiskAlert> alerts) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildRiskSheet(workbook, alerts);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildRiskSheet(Workbook workbook, List<RiskAlert> alerts) {
        Sheet sheet = workbook.createSheet("Risk Alerts");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Alert ID", "User ID", "User Email", "Alert Type", "Severity", "Description", "Status", "Action Taken", "Created At", "Resolved At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (RiskAlert a : alerts) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(a.getId());
            row.createCell(1).setCellValue(a.getUserId() != null ? a.getUserId() : 0);
            row.createCell(2).setCellValue(a.getUserEmail() != null ? a.getUserEmail() : "");
            row.createCell(3).setCellValue(a.getAlertType());
            row.createCell(4).setCellValue(a.getSeverity() != null ? a.getSeverity().name() : "");
            row.createCell(5).setCellValue(a.getDescription());
            row.createCell(6).setCellValue(a.getStatus() != null ? a.getStatus().name() : "");
            row.createCell(7).setCellValue(a.getActionTaken() != null ? a.getActionTaken() : "");
            row.createCell(8).setCellValue(a.getCreatedAt() != null ? a.getCreatedAt().format(DATE_FMT) : "");
            row.createCell(9).setCellValue(a.getResolvedAt() != null ? a.getResolvedAt().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportSecurityIncidents(List<SecurityIncident> incidents) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildFraudSheet(workbook, incidents);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildFraudSheet(Workbook workbook, List<SecurityIncident> incidents) {
        Sheet sheet = workbook.createSheet("Fraud & Incidents");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Incident ID", "Type", "Target Type", "Target ID", "Severity", "Status", "Details", "Resolution", "Assigned Admin", "Created At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SecurityIncident s : incidents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getIncidentType());
            row.createCell(2).setCellValue(s.getTargetType() != null ? s.getTargetType() : "");
            row.createCell(3).setCellValue(s.getTargetId() != null ? s.getTargetId() : "");
            row.createCell(4).setCellValue(s.getSeverity() != null ? s.getSeverity() : "");
            row.createCell(5).setCellValue(s.getStatus() != null ? s.getStatus().name() : "");
            row.createCell(6).setCellValue(s.getDetails() != null ? s.getDetails() : "");
            row.createCell(7).setCellValue(s.getResolution() != null ? s.getResolution() : "");
            row.createCell(8).setCellValue(s.getAssignedToAdminEmail() != null ? s.getAssignedToAdminEmail() : "");
            row.createCell(9).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportCompliance(List<Complaint> complaints, List<RegulatoryCommunication> comms) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildComplaintsSheet(workbook, complaints);
            buildRegulatorySheet(workbook, comms);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildComplaintsSheet(Workbook workbook, List<Complaint> complaints) {
        Sheet sheet = workbook.createSheet("Complaints & Grievances");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Complaint ID", "User ID", "User Email", "Subject", "Category", "Priority", "Status", "Assigned Admin", "SLA Due At", "Created At", "Resolved At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Complaint c : complaints) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getUserId() != null ? c.getUserId() : 0);
            row.createCell(2).setCellValue(c.getUserEmail() != null ? c.getUserEmail() : "");
            row.createCell(3).setCellValue(c.getSubject());
            row.createCell(4).setCellValue(c.getCategory());
            row.createCell(5).setCellValue(c.getPriority());
            row.createCell(6).setCellValue(c.getStatus() != null ? c.getStatus().name() : "");
            row.createCell(7).setCellValue(c.getAssignedToAdminEmail() != null ? c.getAssignedToAdminEmail() : "");
            row.createCell(8).setCellValue(c.getSlaDueAt() != null ? c.getSlaDueAt().format(DATE_FMT) : "");
            row.createCell(9).setCellValue(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FMT) : "");
            row.createCell(10).setCellValue(c.getResolvedAt() != null ? c.getResolvedAt().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public void buildRegulatorySheet(Workbook workbook, List<RegulatoryCommunication> comms) {
        Sheet sheet = workbook.createSheet("Regulatory Logs");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Comm ID", "Regulator", "Ref Number", "Subject", "Direction", "Status", "Recorded By", "Timestamp"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (RegulatoryCommunication r : comms) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getId());
            row.createCell(1).setCellValue(r.getRegulator());
            row.createCell(2).setCellValue(r.getReferenceNumber());
            row.createCell(3).setCellValue(r.getSubject());
            row.createCell(4).setCellValue(r.getDirection());
            row.createCell(5).setCellValue(r.getStatus());
            row.createCell(6).setCellValue(r.getRecordedByAdminEmail() != null ? r.getRecordedByAdminEmail() : "");
            row.createCell(7).setCellValue(r.getTimestamp() != null ? r.getTimestamp().format(DATE_FMT) : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportAuditLogs(List<AuditLog> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildAuditSheet(workbook, logs);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public void buildAuditSheet(Workbook workbook, List<AuditLog> logs) {
        Sheet sheet = workbook.createSheet("Audit Trail");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Log ID", "Timestamp", "Admin ID", "Admin Email", "Admin Role", "Action", "Target Type", "Target ID", "Reason", "IP Address", "Before State", "After State"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (AuditLog a : logs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(a.getId());
            row.createCell(1).setCellValue(a.getTimestamp() != null ? a.getTimestamp().format(DATE_FMT) : "");
            row.createCell(2).setCellValue(a.getAdminId() != null ? a.getAdminId() : 0);
            row.createCell(3).setCellValue(a.getAdminEmail());
            row.createCell(4).setCellValue(a.getRole() != null ? a.getRole() : "");
            row.createCell(5).setCellValue(a.getAction());
            row.createCell(6).setCellValue(a.getTargetType() != null ? a.getTargetType() : "");
            row.createCell(7).setCellValue(a.getTargetId() != null ? a.getTargetId() : "");
            row.createCell(8).setCellValue(a.getReason() != null ? a.getReason() : "");
            row.createCell(9).setCellValue(a.getIpAddress() != null ? a.getIpAddress() : "");
            row.createCell(10).setCellValue(a.getBeforeValues() != null ? a.getBeforeValues() : "");
            row.createCell(11).setCellValue(a.getAfterValues() != null ? a.getAfterValues() : "");
        }
        finalizeSheet(sheet, headers.length);
    }

    public byte[] exportFullBackup(
            List<User> users,
            List<Transaction> orders,
            List<KycDocument> documents,
            List<WalletTransaction> wallet,
            List<Portfolio> portfolios,
            List<FeeRule> feeRules,
            List<RiskAlert> riskAlerts,
            List<SecurityIncident> fraud,
            List<Complaint> complaints,
            List<RegulatoryCommunication> comms,
            List<AuditLog> auditLogs) throws IOException {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildUsersSheet(workbook, users);
            buildOrdersSheet(workbook, orders);
            buildKycSheet(workbook, documents);
            buildWalletSheet(workbook, wallet);
            buildPortfolioSheet(workbook, portfolios);
            buildFeeRulesSheet(workbook, feeRules);
            buildRiskSheet(workbook, riskAlerts);
            buildFraudSheet(workbook, fraud);
            buildComplaintsSheet(workbook, complaints);
            buildRegulatorySheet(workbook, comms);
            buildAuditSheet(workbook, auditLogs);

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
