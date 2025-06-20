package com.example.scalardb.processapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class UserRegistrationResponse {

    private String registrationId;
    private String userId;
    private String name;
    private String email;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    public UserRegistrationResponse() {}

    public UserRegistrationResponse(String registrationId, String userId, String name, 
                                   String email, String status, LocalDateTime completedAt) {
        this.registrationId = registrationId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.status = status;
        this.completedAt = completedAt;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "UserRegistrationResponse{" +
                "registrationId='" + registrationId + '\'' +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", completedAt=" + completedAt +
                '}';
    }
}