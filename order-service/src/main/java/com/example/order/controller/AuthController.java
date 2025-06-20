package com.example.order.controller;

import com.example.order.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller for JWT token generation
 * This is a simplified implementation for demonstration purposes
 * In production, this would integrate with a proper authentication service
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final JwtUtil jwtUtil;
    
    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Generate JWT token for a customer
     * This is a simplified endpoint for testing purposes
     * In production, this would validate credentials against a user store
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestBody Map<String, String> request) {
        String customerId = request.get("customerId");
        
        if (customerId == null || customerId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Customer ID is required"));
        }
        
        // In production, you would validate credentials here
        // For now, we'll generate a token for any valid customer ID
        String token = jwtUtil.generateToken(customerId);
        
        return ResponseEntity.ok(Map.of(
            "token", token,
            "customerId", customerId,
            "type", "Bearer"
        ));
    }
    
    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token is required"));
        }
        
        boolean isValid = jwtUtil.validateToken(token);
        
        if (isValid) {
            String customerId = jwtUtil.extractCustomerId(token);
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "customerId", customerId
            ));
        } else {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
}