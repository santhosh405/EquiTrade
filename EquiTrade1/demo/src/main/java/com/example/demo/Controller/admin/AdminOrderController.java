package com.example.demo.Controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Transaction;
import com.example.demo.model.enums.OrderStatus;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.service.TradeService;

@RestController
@RequestMapping({"/admin/orders", "/api/admin/orders"})
public class AdminOrderController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TradeService tradeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Transaction> list = transactionRepository.findAllByOrderByDateDesc();

        List<Transaction> filtered = list.stream().filter(t -> {
            if (userId != null && (t.getUser() == null || !userId.equals(t.getUser().getId()))) {
                return false;
            }
            if (symbol != null && !symbol.isBlank() &&
                    (t.getStock() == null || !t.getStock().getSymbol().equalsIgnoreCase(symbol.trim()))) {
                return false;
            }
            if (status != null && !status.isBlank() &&
                    (t.getStatus() == null || !t.getStatus().name().equalsIgnoreCase(status.trim()))) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<Transaction> paginated = filtered.subList(start, end);

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", paginated);
        resp.put("totalElements", filtered.size());
        resp.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
        resp.put("page", page);
        resp.put("size", size);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Transaction> getOrderDetails(@PathVariable Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        return ResponseEntity.ok(t);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'SUPPORT')")
    public ResponseEntity<ApiResponse> cancelPendingOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "A mandatory reason is required to cancel an order"));
        }

        tradeService.cancelPendingOrder(id, reason, request);
        return ResponseEntity.ok(new ApiResponse(true, "Pending order #" + id + " has been cancelled."));
    }

    @PostMapping("/{id}/investigation-note")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse> addInvestigationNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String note = body.get("note");
        if (note == null || note.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Investigation note cannot be blank"));
        }

        tradeService.addInvestigationNote(id, note, request);
        return ResponseEntity.ok(new ApiResponse(true, "Investigation note recorded on order #" + id));
    }

    // STRICT COMPLIANCE: Executed trades are READ-ONLY; no edits or deletions allowed
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> forbidEditOrder(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Executed trades are READ-ONLY and immutable under regulatory compliance rules. Edits are strictly prohibited."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> forbidDeleteOrder(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Executed trades are READ-ONLY and immutable under regulatory compliance rules. Deletion is strictly prohibited."));
    }
}
