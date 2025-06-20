package com.example.scalardb.processapi.dto;

import javax.validation.constraints.NotBlank;

public class UserOnboardingRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String preferences;

    public UserOnboardingRequest() {}

    public UserOnboardingRequest(String userId, String preferences) {
        this.userId = userId;
        this.preferences = preferences;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return "UserOnboardingRequest{" +
                "userId='" + userId + '\'' +
                ", preferences='" + preferences + '\'' +
                '}';
    }
}