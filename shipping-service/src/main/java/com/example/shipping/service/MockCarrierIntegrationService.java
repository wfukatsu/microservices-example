package com.example.shipping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class MockCarrierIntegrationService implements CarrierIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(MockCarrierIntegrationService.class);
    private final Random random = new Random();
    
    @Override
    public CarrierShipmentResponse createShipment(CarrierShipmentRequest request) {
        log.info("Mock shipment creation for recipient: {} to {}", 
            request.getRecipientName(), request.getShippingAddress());
        
        // Simulate carrier API call
        try {
            Thread.sleep(200); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate mock tracking number based on shipping method
        String trackingNumber = generateTrackingNumber(request.getShippingMethod());
        
        // Calculate estimated delivery (2-5 days from now)
        int deliveryDays = random.nextInt(4) + 2; // 2-5 days
        LocalDateTime estimatedDelivery = LocalDateTime.now().plusDays(deliveryDays);
        
        return CarrierShipmentResponse.builder()
            .trackingNumber(trackingNumber)
            .estimatedDeliveryDate(estimatedDelivery)
            .carrierShipmentId("carrier_" + UUID.randomUUID().toString())
            .build();
    }
    
    @Override
    public void cancelShipment(String carrier, String trackingNumber) {
        log.info("Mock shipment cancellation for carrier: {} tracking: {}", carrier, trackingNumber);
        
        // Simulate cancellation processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public TrackingInfo getTrackingInfo(String carrier, String trackingNumber) {
        log.info("Mock tracking info retrieval for carrier: {} tracking: {}", carrier, trackingNumber);
        
        // Simulate API call
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate mock tracking status
        String[] statuses = {"SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY"};
        String status = statuses[random.nextInt(statuses.length)];
        
        String[] locations = {"配送センター", "中継拠点", "最寄り営業所", "配達車両"};
        String currentLocation = locations[random.nextInt(locations.length)];
        
        return TrackingInfo.builder()
            .trackingNumber(trackingNumber)
            .status(status)
            .lastUpdated(LocalDateTime.now())
            .currentLocation(currentLocation)
            .estimatedDelivery(LocalDateTime.now().plusDays(1))
            .build();
    }
    
    private String generateTrackingNumber(String shippingMethod) {
        String prefix;
        switch (shippingMethod != null ? shippingMethod.toUpperCase() : "STANDARD") {
            case "EXPRESS":
                prefix = "EX";
                break;
            case "OVERNIGHT":
                prefix = "ON";
                break;
            default:
                prefix = "ST";
        }
        
        // Generate a 12-digit tracking number
        long number = 100000000000L + (long) (random.nextDouble() * 900000000000L);
        return prefix + number;
    }
}