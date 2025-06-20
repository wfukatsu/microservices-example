package com.example.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SystemApiClient {

    private final WebClient webClient;

    public SystemApiClient(@Value("${services.inventory.url:http://localhost:8081}") String inventoryBaseUrl,
                          @Value("${services.payment.url:http://localhost:8082}") String paymentBaseUrl,
                          @Value("${services.shipping.url:http://localhost:8083}") String shippingBaseUrl) {
        this.webClient = WebClient.builder().build();
    }

    public Mono<String> checkInventoryAvailability(String productId, Integer quantity) {
        return webClient.get()
                .uri("http://localhost:8081/api/v1/inventory/{productId}/availability?quantity={quantity}", productId, quantity)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> reserveInventory(String productId, Integer quantity, String orderId) {
        return webClient.post()
                .uri("http://localhost:8081/api/v1/inventory/{productId}/reserve", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(String.format("{\"quantity\": %d, \"orderId\": \"%s\"}", quantity, orderId))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> processPayment(String orderId, Double amount, String paymentMethodId) {
        return webClient.post()
                .uri("http://localhost:8082/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(String.format("{\"orderId\": \"%s\", \"amount\": %.2f, \"paymentMethodId\": \"%s\"}", orderId, amount, paymentMethodId))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> createShipment(String orderId) {
        return webClient.post()
                .uri("http://localhost:8083/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(String.format("{\"orderId\": \"%s\"}", orderId))
                .retrieve()
                .bodyToMono(String.class);
    }
}