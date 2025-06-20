package com.example.scalardb.processapi.controller;

import com.example.scalardb.processapi.dto.*;
import com.example.scalardb.processapi.service.UserRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/process")
@Validated
public class ProcessController {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @PostMapping("/user-registration")
    public Mono<ResponseEntity<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        return userRegistrationService.registerUser(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/user-onboarding")
    public Mono<ResponseEntity<UserOnboardingResponse>> onboardUser(
            @Valid @RequestBody UserOnboardingRequest request) {
        
        return userRegistrationService.onboardUser(request)
                .map(response -> ResponseEntity.ok(response))  
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}