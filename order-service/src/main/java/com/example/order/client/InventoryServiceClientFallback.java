package com.example.order.client;

import org.springframework.stereotype.Component;
import java.util.Collections;

@Component
public class InventoryServiceClientFallback implements InventoryServiceClient {

    @Override
    public InventoryReservationResponse reserveInventory(ReserveInventoryRequest request) {
        throw new RuntimeException("Inventory service is currently unavailable");
    }

    @Override
    public void confirmReservation(String reservationId) {
        throw new RuntimeException("Inventory service is currently unavailable");
    }

    @Override
    public void cancelReservation(String reservationId) {
        // Fallback for cancellation - log but don't fail
        System.err.println("Failed to cancel inventory reservation: " + reservationId + " - Inventory service unavailable");
    }

    @Override
    public InventoryCheckResponse checkInventory(String productId, int quantity) {
        InventoryCheckResponse response = new InventoryCheckResponse();
        response.setProductId(productId);
        response.setAvailableQuantity(0);
        response.setAvailable(false);
        return response;
    }
}