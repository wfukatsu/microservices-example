package com.example.order.client;

import org.springframework.stereotype.Component;

@Component
public class ShippingServiceClientFallback implements ShippingServiceClient {

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        throw new RuntimeException("Shipping service is currently unavailable");
    }

    @Override
    public void cancelShipment(String shipmentId) {
        // Fallback for cancellation - log but don't fail
        System.err.println("Failed to cancel shipment: " + shipmentId + " - Shipping service unavailable");
    }

    @Override
    public ShipmentResponse getShipment(String shipmentId) {
        throw new RuntimeException("Shipping service is currently unavailable");
    }
}