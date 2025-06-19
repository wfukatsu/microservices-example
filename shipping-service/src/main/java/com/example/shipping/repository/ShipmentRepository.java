package com.example.shipping.repository;

import com.example.shipping.entity.Shipment;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ShipmentRepository {
    
    private static final String NAMESPACE = "shipping";
    private static final String TABLE_NAME = "shipments";
    
    public Optional<Shipment> findById(DistributedTransaction transaction, String shipmentId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("shipment_id", shipmentId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToShipment);
    }
    
    public List<Shipment> findByCustomerId(DistributedTransaction transaction, String customerId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Shipment> shipments = new ArrayList<>();
        for (Result result : results) {
            Shipment shipment = mapResultToShipment(result);
            if (customerId.equals(shipment.getCustomerId())) {
                shipments.add(shipment);
            }
        }
        return shipments;
    }
    
    public List<Shipment> findByOrderId(DistributedTransaction transaction, String orderId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Shipment> shipments = new ArrayList<>();
        for (Result result : results) {
            Shipment shipment = mapResultToShipment(result);
            if (orderId.equals(shipment.getOrderId())) {
                shipments.add(shipment);
            }
        }
        return shipments;
    }
    
    public List<Shipment> findByStatus(DistributedTransaction transaction, String status) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Shipment> shipments = new ArrayList<>();
        for (Result result : results) {
            Shipment shipment = mapResultToShipment(result);
            if (status.equals(shipment.getShippingStatus())) {
                shipments.add(shipment);
            }
        }
        return shipments;
    }
    
    public List<Shipment> findActiveShipments(DistributedTransaction transaction) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Shipment> activeShipments = new ArrayList<>();
        for (Result result : results) {
            Shipment shipment = mapResultToShipment(result);
            String status = shipment.getShippingStatus();
            if ("SHIPPED".equals(status) || "IN_TRANSIT".equals(status) || "OUT_FOR_DELIVERY".equals(status)) {
                activeShipments.add(shipment);
            }
        }
        return activeShipments;
    }
    
    public void save(DistributedTransaction transaction, Shipment shipment) throws TransactionException {
        Put.Builder putBuilder = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("shipment_id", shipment.getShipmentId()))
            .textValue("order_id", shipment.getOrderId())
            .textValue("customer_id", shipment.getCustomerId())
            .textValue("carrier", shipment.getCarrier())
            .textValue("shipping_status", shipment.getShippingStatus())
            .bigIntValue("created_at", shipment.getCreatedAt())
            .bigIntValue("updated_at", shipment.getUpdatedAt())
            .intValue("version", shipment.getVersion());
        
        if (shipment.getShippingMethod() != null) {
            putBuilder.textValue("shipping_method", shipment.getShippingMethod());
        }
        if (shipment.getTrackingNumber() != null) {
            putBuilder.textValue("tracking_number", shipment.getTrackingNumber());
        }
        if (shipment.getRecipientName() != null) {
            putBuilder.textValue("recipient_name", shipment.getRecipientName());
        }
        if (shipment.getRecipientPhone() != null) {
            putBuilder.textValue("recipient_phone", shipment.getRecipientPhone());
        }
        if (shipment.getShippingAddress() != null) {
            putBuilder.textValue("shipping_address", shipment.getShippingAddress());
        }
        if (shipment.getShippingCity() != null) {
            putBuilder.textValue("shipping_city", shipment.getShippingCity());
        }
        if (shipment.getShippingState() != null) {
            putBuilder.textValue("shipping_state", shipment.getShippingState());
        }
        if (shipment.getShippingPostalCode() != null) {
            putBuilder.textValue("shipping_postal_code", shipment.getShippingPostalCode());
        }
        if (shipment.getShippingCountry() != null) {
            putBuilder.textValue("shipping_country", shipment.getShippingCountry());
        }
        if (shipment.getEstimatedDeliveryDate() != null) {
            putBuilder.bigIntValue("estimated_delivery_date", shipment.getEstimatedDeliveryDate());
        }
        if (shipment.getActualDeliveryDate() != null) {
            putBuilder.bigIntValue("actual_delivery_date", shipment.getActualDeliveryDate());
        }
        if (shipment.getShippingCost() != null) {
            putBuilder.bigIntValue("shipping_cost", shipment.getShippingCost());
        }
        if (shipment.getCurrency() != null) {
            putBuilder.textValue("currency", shipment.getCurrency());
        }
        if (shipment.getWeight() != null) {
            putBuilder.doubleValue("weight", shipment.getWeight());
        }
        if (shipment.getDimensions() != null) {
            putBuilder.textValue("dimensions", shipment.getDimensions());
        }
        if (shipment.getSpecialInstructions() != null) {
            putBuilder.textValue("special_instructions", shipment.getSpecialInstructions());
        }
        
        transaction.put(putBuilder.build());
    }
    
    public boolean existsById(DistributedTransaction transaction, String shipmentId) 
            throws TransactionException {
        return findById(transaction, shipmentId).isPresent();
    }
    
    public long countDelayedShipments(DistributedTransaction transaction) throws TransactionException {
        long currentTime = System.currentTimeMillis();
        List<Shipment> activeShipments = findActiveShipments(transaction);
        
        return activeShipments.stream()
            .filter(shipment -> shipment.getEstimatedDeliveryDate() != null)
            .filter(shipment -> shipment.getEstimatedDeliveryDate() < currentTime)
            .count();
    }
    
    private Shipment mapResultToShipment(Result result) {
        Shipment shipment = new Shipment();
        
        result.getValue("shipment_id").ifPresent(v -> shipment.setShipmentId(((TextValue) v).get()));
        result.getValue("order_id").ifPresent(v -> shipment.setOrderId(((TextValue) v).get()));
        result.getValue("customer_id").ifPresent(v -> shipment.setCustomerId(((TextValue) v).get()));
        result.getValue("shipping_method").ifPresent(v -> shipment.setShippingMethod(((TextValue) v).get()));
        result.getValue("carrier").ifPresent(v -> shipment.setCarrier(((TextValue) v).get()));
        result.getValue("tracking_number").ifPresent(v -> shipment.setTrackingNumber(((TextValue) v).get()));
        result.getValue("shipping_status").ifPresent(v -> shipment.setShippingStatus(((TextValue) v).get()));
        result.getValue("recipient_name").ifPresent(v -> shipment.setRecipientName(((TextValue) v).get()));
        result.getValue("recipient_phone").ifPresent(v -> shipment.setRecipientPhone(((TextValue) v).get()));
        result.getValue("shipping_address").ifPresent(v -> shipment.setShippingAddress(((TextValue) v).get()));
        result.getValue("shipping_city").ifPresent(v -> shipment.setShippingCity(((TextValue) v).get()));
        result.getValue("shipping_state").ifPresent(v -> shipment.setShippingState(((TextValue) v).get()));
        result.getValue("shipping_postal_code").ifPresent(v -> shipment.setShippingPostalCode(((TextValue) v).get()));
        result.getValue("shipping_country").ifPresent(v -> shipment.setShippingCountry(((TextValue) v).get()));
        result.getValue("estimated_delivery_date").ifPresent(v -> shipment.setEstimatedDeliveryDate(v.getAsLong()));
        result.getValue("actual_delivery_date").ifPresent(v -> shipment.setActualDeliveryDate(v.getAsLong()));
        result.getValue("shipping_cost").ifPresent(v -> shipment.setShippingCost(v.getAsLong()));
        result.getValue("currency").ifPresent(v -> shipment.setCurrency(((TextValue) v).get()));
        result.getValue("weight").ifPresent(v -> shipment.setWeight(v.getAsDouble()));
        result.getValue("dimensions").ifPresent(v -> shipment.setDimensions(((TextValue) v).get()));
        result.getValue("special_instructions").ifPresent(v -> shipment.setSpecialInstructions(((TextValue) v).get()));
        result.getValue("created_at").ifPresent(v -> shipment.setCreatedAt(v.getAsLong()));
        result.getValue("updated_at").ifPresent(v -> shipment.setUpdatedAt(v.getAsLong()));
        result.getValue("version").ifPresent(v -> shipment.setVersion(v.getAsInt()));
        
        return shipment;
    }
}