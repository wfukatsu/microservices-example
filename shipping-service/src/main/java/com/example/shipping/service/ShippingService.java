package com.example.shipping.service;

import com.example.shipping.dto.CreateShipmentRequest;
import com.example.shipping.dto.UpdateShippingStatusRequest;
import com.example.shipping.entity.Shipment;
import com.example.shipping.entity.ShippingItem;
import com.example.shipping.entity.ShippingStatus;
import com.example.shipping.exception.InvalidShippingStatusException;
import com.example.shipping.exception.ShipmentNotFoundException;
import com.example.shipping.repository.ShipmentRepository;
import com.example.shipping.repository.ShippingItemRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShippingService {
    
    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private ShippingItemRepository shippingItemRepository;
    
    @Autowired
    private CarrierIntegrationService carrierIntegrationService;
    
    public Shipment createShipment(CreateShipmentRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            String shipmentId = UUID.randomUUID().toString();
            
            // Create shipment
            Shipment shipment = new Shipment(shipmentId, request.getOrderId(), 
                request.getCustomerId(), request.getCarrier());
            
            // Set shipping details
            shipment.setShippingMethod(request.getShippingMethod());
            if (request.getRecipientInfo() != null) {
                shipment.setRecipientName(request.getRecipientInfo().getName());
                shipment.setRecipientPhone(request.getRecipientInfo().getPhone());
                shipment.setShippingAddress(request.getRecipientInfo().getAddress());
                shipment.setShippingCity(request.getRecipientInfo().getCity());
                shipment.setShippingState(request.getRecipientInfo().getState());
                shipment.setShippingPostalCode(request.getRecipientInfo().getPostalCode());
                shipment.setShippingCountry(request.getRecipientInfo().getCountry());
            }
            
            if (request.getPackageInfo() != null) {
                shipment.setWeight(request.getPackageInfo().getWeight());
                shipment.setDimensions(request.getPackageInfo().getDimensions());
                shipment.setSpecialInstructions(request.getPackageInfo().getSpecialInstructions());
            }
            
            // Calculate shipping cost (mock implementation)
            Long shippingCost = calculateShippingCost(request);
            shipment.setShippingCost(shippingCost);
            shipment.setCurrency("JPY");
            
            // Create shipment with carrier
            try {
                CarrierShipmentResponse carrierResponse = carrierIntegrationService.createShipment(
                    buildCarrierShipmentRequest(shipment, request.getItems()));
                
                shipment.setTrackingNumber(carrierResponse.getTrackingNumber());
                shipment.setEstimatedDeliveryDateAsDateTime(carrierResponse.getEstimatedDeliveryDate());
                shipment.setShippingStatusEnum(ShippingStatus.PROCESSING);
            } catch (Exception e) {
                log.warn("Failed to create shipment with carrier, proceeding without tracking: {}", e.getMessage());
                shipment.setShippingStatusEnum(ShippingStatus.PENDING);
            }
            
            shipmentRepository.save(transaction, shipment);
            
            // Create shipping items
            List<ShippingItem> shippingItems = new ArrayList<>();
            for (int i = 0; i < request.getItems().size(); i++) {
                CreateShipmentRequest.ShippingItemRequest itemRequest = request.getItems().get(i);
                String itemId = UUID.randomUUID().toString();
                
                ShippingItem item = new ShippingItem(shipmentId, itemId, 
                    itemRequest.getProductId(), itemRequest.getProductName(), itemRequest.getQuantity());
                item.setWeight(itemRequest.getWeight());
                item.setDimensions(itemRequest.getDimensions());
                item.setIsFragile(itemRequest.getIsFragile());
                item.setIsHazardous(itemRequest.getIsHazardous());
                
                shippingItems.add(item);
            }
            
            shippingItemRepository.saveAll(transaction, shippingItems);
            
            transaction.commit();
            
            log.info("Created shipment: {} for order: {}", shipmentId, request.getOrderId());
            return shipment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to create shipment for order: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to create shipment", e);
        }
    }
    
    public Shipment updateShippingStatus(String shipmentId, UpdateShippingStatusRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findById(transaction, shipmentId);
            if (shipmentOpt.isEmpty()) {
                throw new ShipmentNotFoundException("Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            ShippingStatus previousStatus = shipment.getShippingStatusEnum();
            ShippingStatus newStatus = ShippingStatus.valueOf(request.getShippingStatus());
            
            // Validate status transition
            if (!isValidStatusTransition(previousStatus, newStatus)) {
                throw new InvalidShippingStatusException(
                    "Invalid status transition from " + previousStatus + " to " + newStatus);
            }
            
            // Update shipment
            shipment.setShippingStatusEnum(newStatus);
            if (request.getTrackingNumber() != null) {
                shipment.setTrackingNumber(request.getTrackingNumber());
            }
            if (request.getEstimatedDeliveryDate() != null) {
                shipment.setEstimatedDeliveryDateAsDateTime(request.getEstimatedDeliveryDate());
            }
            if (request.getActualDeliveryDate() != null && newStatus == ShippingStatus.DELIVERED) {
                shipment.setActualDeliveryDateAsDateTime(request.getActualDeliveryDate());
            }
            
            shipment.setUpdatedAt(System.currentTimeMillis());
            shipment.setVersion(shipment.getVersion() + 1);
            
            shipmentRepository.save(transaction, shipment);
            
            transaction.commit();
            
            log.info("Updated shipment status: {} from {} to {}", shipmentId, previousStatus, newStatus);
            return shipment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to update shipping status for shipment: {}", shipmentId, e);
            throw new RuntimeException("Failed to update shipping status", e);
        }
    }
    
    public void cancelShipment(String shipmentId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findById(transaction, shipmentId);
            if (shipmentOpt.isEmpty()) {
                throw new ShipmentNotFoundException("Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            ShippingStatus currentStatus = shipment.getShippingStatusEnum();
            
            // Check if shipment can be cancelled
            if (currentStatus == ShippingStatus.DELIVERED || currentStatus == ShippingStatus.CANCELLED) {
                throw new InvalidShippingStatusException("Shipment cannot be cancelled in current status: " + currentStatus);
            }
            
            // Cancel with carrier if necessary
            if (shipment.getTrackingNumber() != null) {
                try {
                    carrierIntegrationService.cancelShipment(shipment.getCarrier(), shipment.getTrackingNumber());
                } catch (Exception e) {
                    log.warn("Failed to cancel shipment with carrier: {}", e.getMessage());
                }
            }
            
            shipment.setShippingStatusEnum(ShippingStatus.CANCELLED);
            shipment.setUpdatedAt(System.currentTimeMillis());
            shipment.setVersion(shipment.getVersion() + 1);
            
            shipmentRepository.save(transaction, shipment);
            
            transaction.commit();
            
            log.info("Cancelled shipment: {}", shipmentId);
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to cancel shipment: {}", shipmentId, e);
            throw new RuntimeException("Failed to cancel shipment", e);
        }
    }
    
    public Optional<Shipment> getShipment(String shipmentId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<Shipment> shipment = shipmentRepository.findById(transaction, shipmentId);
            transaction.commit();
            return shipment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get shipment: {}", shipmentId, e);
            throw new RuntimeException("Failed to get shipment", e);
        }
    }
    
    public List<Shipment> getShipmentsByCustomer(String customerId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            List<Shipment> shipments = shipmentRepository.findByCustomerId(transaction, customerId);
            transaction.commit();
            return shipments;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get shipments for customer: {}", customerId, e);
            throw new RuntimeException("Failed to get shipments for customer", e);
        }
    }
    
    public List<ShippingItem> getShipmentItems(String shipmentId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            List<ShippingItem> items = shippingItemRepository.findByShipmentId(transaction, shipmentId);
            transaction.commit();
            return items;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get shipping items for shipment: {}", shipmentId, e);
            throw new RuntimeException("Failed to get shipping items", e);
        }
    }
    
    private boolean isValidStatusTransition(ShippingStatus from, ShippingStatus to) {
        // Define valid status transitions
        switch (from) {
            case PENDING:
                return to == ShippingStatus.PROCESSING || to == ShippingStatus.CANCELLED;
            case PROCESSING:
                return to == ShippingStatus.SHIPPED || to == ShippingStatus.CANCELLED;
            case SHIPPED:
                return to == ShippingStatus.IN_TRANSIT || to == ShippingStatus.EXCEPTION;
            case IN_TRANSIT:
                return to == ShippingStatus.OUT_FOR_DELIVERY || to == ShippingStatus.EXCEPTION;
            case OUT_FOR_DELIVERY:
                return to == ShippingStatus.DELIVERED || to == ShippingStatus.EXCEPTION;
            case EXCEPTION:
                return to == ShippingStatus.IN_TRANSIT || to == ShippingStatus.RETURNED || to == ShippingStatus.LOST;
            default:
                return false;
        }
    }
    
    private Long calculateShippingCost(CreateShipmentRequest request) {
        // Simple shipping cost calculation (mock implementation)
        double weight = request.getPackageInfo() != null && request.getPackageInfo().getWeight() != null ? 
            request.getPackageInfo().getWeight() : 1.0;
        
        // Base cost + weight-based cost
        return (long) (500 + (weight * 100));
    }
    
    private CarrierShipmentRequest buildCarrierShipmentRequest(Shipment shipment, 
            List<CreateShipmentRequest.ShippingItemRequest> items) {
        return CarrierShipmentRequest.builder()
            .shipmentId(shipment.getShipmentId())
            .recipientName(shipment.getRecipientName())
            .recipientPhone(shipment.getRecipientPhone())
            .shippingAddress(shipment.getShippingAddress())
            .shippingCity(shipment.getShippingCity())
            .shippingState(shipment.getShippingState())
            .shippingPostalCode(shipment.getShippingPostalCode())
            .shippingCountry(shipment.getShippingCountry())
            .weight(shipment.getWeight())
            .dimensions(shipment.getDimensions())
            .specialInstructions(shipment.getSpecialInstructions())
            .shippingMethod(shipment.getShippingMethod())
            .items(items)
            .build();
    }
}