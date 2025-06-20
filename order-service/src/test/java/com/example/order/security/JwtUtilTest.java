package com.example.order.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 */
class JwtUtilTest {
    
    private JwtUtil jwtUtil;
    
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "myTestSecretKeyForJwtTokenGenerationAndValidation123456789");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
    }
    
    @Test
    void generateToken_ValidCustomerId_ReturnsToken() {
        // Given
        String customerId = "CUST-001";
        
        // When
        String token = jwtUtil.generateToken(customerId);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }
    
    @Test
    void extractCustomerId_ValidToken_ReturnsCustomerId() {
        // Given
        String customerId = "CUST-001";
        String token = jwtUtil.generateToken(customerId);
        
        // When
        String extractedCustomerId = jwtUtil.extractCustomerId(token);
        
        // Then
        assertThat(extractedCustomerId).isEqualTo(customerId);
    }
    
    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        String customerId = "CUST-001";
        String token = jwtUtil.generateToken(customerId);
        
        // When
        Boolean isValid = jwtUtil.validateToken(token, customerId);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    void validateToken_InvalidCustomerId_ReturnsFalse() {
        // Given
        String customerId = "CUST-001";
        String token = jwtUtil.generateToken(customerId);
        String wrongCustomerId = "CUST-002";
        
        // When
        Boolean isValid = jwtUtil.validateToken(token, wrongCustomerId);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Given
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJDVVNULTAwMSIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NDU5MjAwfQ.invalid_signature";
        
        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void extractExpiration_ValidToken_ReturnsExpiration() {
        // Given
        String customerId = "CUST-001";
        String token = jwtUtil.generateToken(customerId);
        
        // When
        Date expiration = jwtUtil.extractExpiration(token);
        
        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }
}