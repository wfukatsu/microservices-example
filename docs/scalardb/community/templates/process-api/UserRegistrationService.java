package com.example.scalardb.processapi.service;

import com.example.scalardb.processapi.client.SystemApiClient;
import com.example.scalardb.processapi.dto.*;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.db.exception.storage.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private static final String NAMESPACE = "process_service";
    private static final String REGISTRATION_TABLE = "user_registrations";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private SystemApiClient systemApiClient;

    @Autowired
    private DistributedTransactionManager transactionManager;

    public Mono<UserRegistrationResponse> registerUser(UserRegistrationRequest request) {
        return Mono.fromCallable(() -> executeRegistrationProcess(request))
                .onErrorMap(Exception.class, e -> new RuntimeException("User registration failed", e));
    }

    private UserRegistrationResponse executeRegistrationProcess(UserRegistrationRequest request) 
            throws ExecutionException {
        
        String registrationId = UUID.randomUUID().toString();
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            // Step 1: Create registration record
            createRegistrationRecord(transaction, registrationId, request);
            
            // Step 2: Create user via System API
            CreateUserRequest createUserRequest = new CreateUserRequest(
                request.getName(), 
                request.getEmail()
            );
            
            UserResponse userResponse = systemApiClient.createUser(createUserRequest).block();
            
            if (userResponse == null) {
                throw new ExecutionException("Failed to create user via System API");
            }
            
            // Step 3: Update registration status to completed
            updateRegistrationStatus(transaction, registrationId, "COMPLETED", userResponse.getId());
            
            // Step 4: Send welcome notification (simulated)
            sendWelcomeNotification(userResponse);
            
            transaction.commit();
            
            return new UserRegistrationResponse(
                registrationId,
                userResponse.getId(),
                userResponse.getName(),
                userResponse.getEmail(),
                "COMPLETED",
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            try {
                // Update registration status to failed
                updateRegistrationStatus(transaction, registrationId, "FAILED", null);
                transaction.commit();
            } catch (Exception rollbackException) {
                transaction.abort();
            }
            throw new ExecutionException("Registration process failed", e);
        }
    }

    private void createRegistrationRecord(DistributedTransaction transaction, 
                                        String registrationId, 
                                        UserRegistrationRequest request) 
            throws ExecutionException {
        
        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(REGISTRATION_TABLE)
                .partitionKey(Key.ofText("registration_id", registrationId))
                .textValue("name", request.getName())
                .textValue("email", request.getEmail())
                .textValue("status", "PROCESSING")
                .textValue("created_at", LocalDateTime.now().format(FORMATTER))
                .build();

        transaction.put(put);
    }

    private void updateRegistrationStatus(DistributedTransaction transaction,
                                        String registrationId,
                                        String status,
                                        String userId) throws ExecutionException {
        
        Put.Builder putBuilder = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(REGISTRATION_TABLE)
                .partitionKey(Key.ofText("registration_id", registrationId))
                .textValue("status", status)
                .textValue("updated_at", LocalDateTime.now().format(FORMATTER));
        
        if (userId != null) {
            putBuilder.textValue("user_id", userId);
        }
        
        transaction.put(putBuilder.build());
    }

    private void sendWelcomeNotification(UserResponse user) {
        // Simulate sending welcome notification
        // In real implementation, this could be:
        // - Email service call
        // - Message queue publication
        // - External notification service call
        System.out.println("Sending welcome notification to: " + user.getEmail());
    }

    public Mono<UserOnboardingResponse> onboardUser(UserOnboardingRequest request) {
        return Mono.fromCallable(() -> executeOnboardingProcess(request))
                .onErrorMap(Exception.class, e -> new RuntimeException("User onboarding failed", e));
    }

    private UserOnboardingResponse executeOnboardingProcess(UserOnboardingRequest request) 
            throws ExecutionException {
        
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            // Step 1: Activate user
            UserResponse activatedUser = systemApiClient.activateUser(request.getUserId()).block();
            
            if (activatedUser == null) {
                throw new ExecutionException("Failed to activate user");
            }
            
            // Step 2: Create onboarding record
            String onboardingId = UUID.randomUUID().toString();
            createOnboardingRecord(transaction, onboardingId, request, activatedUser);
            
            // Step 3: Setup user preferences (simulated)
            setupUserPreferences(request.getUserId(), request.getPreferences());
            
            // Step 4: Send onboarding completion notification
            sendOnboardingNotification(activatedUser);
            
            transaction.commit();
            
            return new UserOnboardingResponse(
                onboardingId,
                activatedUser.getId(),
                activatedUser.getName(),
                "COMPLETED",
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Onboarding process failed", e);
        }
    }

    private void createOnboardingRecord(DistributedTransaction transaction,
                                      String onboardingId,
                                      UserOnboardingRequest request,
                                      UserResponse user) throws ExecutionException {
        
        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table("user_onboarding")
                .partitionKey(Key.ofText("onboarding_id", onboardingId))
                .textValue("user_id", request.getUserId())
                .textValue("user_name", user.getName())
                .textValue("preferences", request.getPreferences())
                .textValue("status", "COMPLETED")
                .textValue("created_at", LocalDateTime.now().format(FORMATTER))
                .build();

        transaction.put(put);
    }

    private void setupUserPreferences(String userId, String preferences) {
        // Simulate setting up user preferences
        System.out.println("Setting up preferences for user " + userId + ": " + preferences);
    }

    private void sendOnboardingNotification(UserResponse user) {
        // Simulate sending onboarding completion notification
        System.out.println("Sending onboarding completion notification to: " + user.getEmail());
    }
}