package com.example.scalardb.processapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class UserOnboardingResponse {

    private String onboardingId;
    private String userId;
    private String userName;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    public UserOnboardingResponse() {}

    public UserOnboardingResponse(String onboardingId, String userId, String userName, 
                                 String status, LocalDateTime completedAt) {
        this.onboardingId = onboardingId;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.completedAt = completedAt;
    }

    public String getOnboardingId() {
        return onboardingId;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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
        return "UserOnboardingResponse{" +
                "onboardingId='" + onboardingId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", status='" + status + '\'' +
                ", completedAt=" + completedAt +
                '}';
    }
}