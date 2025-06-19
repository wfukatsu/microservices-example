package com.example.inventory.repository;

import com.example.inventory.entity.InventoryReservation;
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
public class ReservationRepository {
    
    private static final String NAMESPACE = "inventory";
    private static final String TABLE_NAME = "inventory_reservations";
    
    public Optional<InventoryReservation> findById(DistributedTransaction transaction, String reservationId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("reservation_id", reservationId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToReservation);
    }
    
    public List<InventoryReservation> findByProductId(DistributedTransaction transaction, String productId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<InventoryReservation> reservations = new ArrayList<>();
        for (Result result : results) {
            InventoryReservation reservation = mapResultToReservation(result);
            if (productId.equals(reservation.getProductId())) {
                reservations.add(reservation);
            }
        }
        return reservations;
    }
    
    public List<InventoryReservation> findByCustomerId(DistributedTransaction transaction, String customerId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<InventoryReservation> reservations = new ArrayList<>();
        for (Result result : results) {
            InventoryReservation reservation = mapResultToReservation(result);
            if (customerId.equals(reservation.getCustomerId())) {
                reservations.add(reservation);
            }
        }
        return reservations;
    }
    
    public List<InventoryReservation> findExpiredReservations(DistributedTransaction transaction) 
            throws TransactionException {
        long currentTime = System.currentTimeMillis();
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<InventoryReservation> expiredReservations = new ArrayList<>();
        for (Result result : results) {
            InventoryReservation reservation = mapResultToReservation(result);
            if (reservation.getExpiresAt() < currentTime && 
                "ACTIVE".equals(reservation.getReservationStatus())) {
                expiredReservations.add(reservation);
            }
        }
        return expiredReservations;
    }
    
    public void save(DistributedTransaction transaction, InventoryReservation reservation) 
            throws TransactionException {
        Put put = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("reservation_id", reservation.getReservationId()))
            .textValue("product_id", reservation.getProductId())
            .textValue("customer_id", reservation.getCustomerId())
            .intValue("reserved_quantity", reservation.getReservedQuantity())
            .textValue("reservation_status", reservation.getReservationStatus())
            .bigIntValue("expires_at", reservation.getExpiresAt())
            .bigIntValue("created_at", reservation.getCreatedAt())
            .bigIntValue("updated_at", reservation.getUpdatedAt())
            .build();
        
        transaction.put(put);
    }
    
    public boolean existsById(DistributedTransaction transaction, String reservationId) 
            throws TransactionException {
        return findById(transaction, reservationId).isPresent();
    }
    
    private InventoryReservation mapResultToReservation(Result result) {
        InventoryReservation reservation = new InventoryReservation();
        
        result.getValue("reservation_id").ifPresent(v -> reservation.setReservationId(((TextValue) v).get()));
        result.getValue("product_id").ifPresent(v -> reservation.setProductId(((TextValue) v).get()));
        result.getValue("customer_id").ifPresent(v -> reservation.setCustomerId(((TextValue) v).get()));
        result.getValue("reserved_quantity").ifPresent(v -> reservation.setReservedQuantity(v.getAsInt()));
        result.getValue("reservation_status").ifPresent(v -> reservation.setReservationStatus(((TextValue) v).get()));
        result.getValue("expires_at").ifPresent(v -> reservation.setExpiresAt(v.getAsLong()));
        result.getValue("created_at").ifPresent(v -> reservation.setCreatedAt(v.getAsLong()));
        result.getValue("updated_at").ifPresent(v -> reservation.setUpdatedAt(v.getAsLong()));
        
        return reservation;
    }
}