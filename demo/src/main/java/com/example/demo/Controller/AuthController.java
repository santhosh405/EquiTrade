package com.example.demo.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.AuthService;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        String result = authService.register(request);
        if (result != null && result.toLowerCase().contains("registration successful")) {
            return ResponseEntity.ok(new ApiResponse(true, result));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, result));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "User not authenticated"));
        }
        // Return DTO — never the User entity (which exposes password hash, PAN, Aadhaar, etc.)
        return ResponseEntity.ok(UserDTO.fromUserSelf(user));
    }

    @PostMapping("/verify-approval-token")
    public ResponseEntity<ApiResponse> verifyApprovalToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String reason = body.get("reason");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Token parameter is required"));
        }
        try {
            String message = authService.processApprovalToken(token, reason);
            return ResponseEntity.ok(new ApiResponse(true, message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        }
    }
}