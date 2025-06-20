package com.example.order.controller;

import com.example.order.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController
 */
@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private JwtUtil jwtUtil;
    
    @Test
    void generateToken_ValidCustomerId_ReturnsToken() throws Exception {
        // Given
        String customerId = "CUST-001";
        String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.token";
        Map<String, String> request = Map.of("customerId", customerId);
        
        when(jwtUtil.generateToken(customerId)).thenReturn(expectedToken);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }
    
    @Test
    void generateToken_EmptyCustomerId_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, String> request = Map.of("customerId", "");
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Customer ID is required"));
    }
    
    @Test
    void generateToken_MissingCustomerId_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, String> request = Map.of();
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Customer ID is required"));
    }
    
    @Test
    void validateToken_ValidToken_ReturnsValid() throws Exception {
        // Given
        String token = "eyJhbGciOiJIUzI1NiJ9.test.token";
        String customerId = "CUST-001";
        Map<String, String> request = Map.of("token", token);
        
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractCustomerId(token)).thenReturn(customerId);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.customerId").value(customerId));
    }
    
    @Test
    void validateToken_InvalidToken_ReturnsInvalid() throws Exception {
        // Given
        String token = "invalid.token";
        Map<String, String> request = Map.of("token", token);
        
        when(jwtUtil.validateToken(token)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void validateToken_EmptyToken_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, String> request = Map.of("token", "");
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token is required"));
    }
}