package com.aitutor.controller;

import com.aitutor.config.JwtUtil;
import com.aitutor.dto.AuthRequest;
import com.aitutor.dto.AuthResponse;
import com.aitutor.service.AuditLogService;
import com.aitutor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    @Autowired
    public AuthController(UserService userService, JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          AuditLogService auditLogService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            userService.register(request.getUsername(), request.getPassword());
            String token = jwtUtil.generateToken(request.getUsername());
            auditLogService.log(request.getUsername(), "REGISTER", "User registered successfully", httpRequest);
            return ResponseEntity.ok(new AuthResponse(token, request.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String token = jwtUtil.generateToken(request.getUsername());
            auditLogService.log(request.getUsername(), "LOGIN", "User logged in", httpRequest);
            return ResponseEntity.ok(new AuthResponse(token, request.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String confirmPassword = body.get("confirmPassword");

            if (username == null || password == null || confirmPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
            }
            if (!password.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
            }

            userService.updatePassword(username, password);
            auditLogService.log(username, "PASSWORD_RESET", "Password reset", httpRequest);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
