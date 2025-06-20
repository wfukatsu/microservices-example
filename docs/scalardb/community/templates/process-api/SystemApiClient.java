package com.example.scalardb.processapi.client;

import com.example.scalardb.processapi.dto.CreateUserRequest;
import com.example.scalardb.processapi.dto.UpdateUserRequest;
import com.example.scalardb.processapi.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SystemApiClient {

    private final WebClient webClient;

    public SystemApiClient(@Value("${system-api.base-url:http://localhost:8080}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<UserResponse> createUser(CreateUserRequest request) {
        return webClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Mono<UserResponse> getUserById(String id) {
        return webClient.get()
                .uri("/api/v1/users/{id}", id)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Flux<UserResponse> getAllUsers() {
        return webClient.get()
                .uri("/api/v1/users")
                .retrieve()
                .bodyToFlux(UserResponse.class);
    }

    public Mono<UserResponse> updateUser(String id, UpdateUserRequest request) {
        return webClient.put()
                .uri("/api/v1/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Mono<Void> deleteUser(String id) {
        return webClient.delete()
                .uri("/api/v1/users/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<UserResponse> activateUser(String id) {
        return webClient.post()
                .uri("/api/v1/users/{id}/activate", id)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Mono<UserResponse> deactivateUser(String id) {
        return webClient.post()
                .uri("/api/v1/users/{id}/deactivate", id)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }
}