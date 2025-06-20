package com.example.order.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility for sanitizing sensitive data in logs and responses
 */
@Component
public class SensitiveDataFilter {
    
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "paymentToken", "cardNumber", "cvv", "password", "token", "secret"
    );
    
    private static final String MASKED_VALUE = "[REDACTED]";
    
    private final ObjectMapper objectMapper;
    
    public SensitiveDataFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Sanitize an object for logging by masking sensitive fields
     */
    public String sanitizeForLogging(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            JsonNode jsonNode = objectMapper.valueToTree(obj);
            maskSensitiveFields(jsonNode);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return "Failed to sanitize object: " + obj.getClass().getSimpleName();
        }
    }
    
    /**
     * Recursively mask sensitive fields in a JSON tree
     */
    private void maskSensitiveFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (SENSITIVE_FIELDS.stream().anyMatch(field -> 
                    fieldName.toLowerCase().contains(field.toLowerCase()))) {
                    objectNode.put(fieldName, MASKED_VALUE);
                } else {
                    maskSensitiveFields(objectNode.get(fieldName));
                }
            });
        } else if (node.isArray()) {
            for (JsonNode arrayItem : node) {
                maskSensitiveFields(arrayItem);
            }
        }
    }
    
    /**
     * Check if a field name represents sensitive data
     */
    public boolean isSensitiveField(String fieldName) {
        return SENSITIVE_FIELDS.stream().anyMatch(field -> 
            fieldName.toLowerCase().contains(field.toLowerCase()));
    }
}