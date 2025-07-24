package com.example.inventory.service;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.ReserveInventoryRequest;
import com.example.inventory.dto.InventoryCheckResponse;
import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.entity.InventoryStatus;
import com.example.inventory.entity.ReservationStatus;
import com.example.inventory.exception.InventoryNotFoundException;
import com.example.inventory.exception.InsufficientInventoryException;
import com.example.inventory.exception.ReservationNotFoundException;
import com.example.inventory.exception.InvalidReservationStatusException;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    public InventoryItem createInventoryItem(CreateInventoryItemRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Check if product already exists
            if (inventoryRepository.existsById(transaction, request.getProductId())) {
                throw new IllegalArgumentException("Product already exists: " + request.getProductId());
            }
            
            InventoryItem item = new InventoryItem(
                request.getProductId(),
                request.getProductName(),
                request.getTotalQuantity(),
                request.getUnitPrice(),
                request.getCurrency()
            );
            item.setStatusEnum(InventoryStatus.valueOf(request.getStatus()));
            
            inventoryRepository.save(transaction, item);
            transaction.commit();
            
            log.info("Created inventory item: {}", item.getProductId());
            return item;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to create inventory item: {}", request.getProductId(), e);
            throw new RuntimeException("Failed to create inventory item", e);
        }
    }
    
    public Optional<InventoryItem> getInventoryItem(String productId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<InventoryItem> item = inventoryRepository.findById(transaction, productId);
            transaction.commit();
            return item;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get inventory item: {}", productId, e);
            throw new RuntimeException("Failed to get inventory item", e);
        }
    }
    
    public List<InventoryItem> getAllInventoryItems() {
        DistributedTransaction transaction = transactionManager.start();
        try {
            List<InventoryItem> items = inventoryRepository.findAll(transaction);
            transaction.commit();
            return items;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get all inventory items", e);
            throw new RuntimeException("Failed to get all inventory items", e);
        }
    }
    
    public InventoryReservation reserveInventory(ReserveInventoryRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get inventory item
            Optional<InventoryItem> itemOpt = inventoryRepository.findById(transaction, request.getProductId());
            if (itemOpt.isEmpty()) {
                throw new InventoryNotFoundException("Product not found: " + request.getProductId());
            }
            
            InventoryItem item = itemOpt.get();
            
            // Check if sufficient inventory is available
            if (item.getAvailableQuantity() < request.getReservedQuantity()) {
                throw new InsufficientInventoryException(
                    String.format("Insufficient inventory. Available: %d, Requested: %d", 
                        item.getAvailableQuantity(), request.getReservedQuantity()));
            }
            
            // Create reservation
            String reservationId = UUID.randomUUID().toString();
            InventoryReservation reservation = new InventoryReservation(
                reservationId,
                request.getProductId(),
                request.getCustomerId(),
                request.getReservedQuantity(),
                request.getExpiresAt()
            );
            
            reservationRepository.save(transaction, reservation);
            
            // Update inventory quantities
            item.setAvailableQuantity(item.getAvailableQuantity() - request.getReservedQuantity());
            item.setReservedQuantity(item.getReservedQuantity() + request.getReservedQuantity());
            item.setUpdatedAt(System.currentTimeMillis());
            item.setVersion(item.getVersion() + 1);
            
            inventoryRepository.save(transaction, item);
            
            transaction.commit();
            
            log.info("Reserved inventory: productId={}, quantity={}, reservationId={}", 
                request.getProductId(), request.getReservedQuantity(), reservationId);
            
            return reservation;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to reserve inventory: {}", request.getProductId(), e);
            throw new RuntimeException("Failed to reserve inventory", e);
        }
    }
    
    public void consumeReservation(String reservationId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get reservation
            Optional<InventoryReservation> reservationOpt = reservationRepository.findById(transaction, reservationId);
            if (reservationOpt.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found: " + reservationId);
            }
            
            InventoryReservation reservation = reservationOpt.get();
            if (!ReservationStatus.ACTIVE.name().equals(reservation.getReservationStatus())) {
                throw new InvalidReservationStatusException("Reservation is not active: " + reservationId);
            }
            
            // Get inventory item
            Optional<InventoryItem> itemOpt = inventoryRepository.findById(transaction, reservation.getProductId());
            InventoryItem item = itemOpt.orElseThrow();
            
            // Update inventory quantities
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQuantity());
            item.setTotalQuantity(item.getTotalQuantity() - reservation.getReservedQuantity());
            item.setUpdatedAt(System.currentTimeMillis());
            item.setVersion(item.getVersion() + 1);
            
            inventoryRepository.save(transaction, item);
            
            // Update reservation status
            reservation.setReservationStatusEnum(ReservationStatus.CONSUMED);
            reservation.setUpdatedAt(System.currentTimeMillis());
            
            reservationRepository.save(transaction, reservation);
            
            transaction.commit();
            
            log.info("Consumed reservation: reservationId={}, productId={}, quantity={}", 
                reservationId, reservation.getProductId(), reservation.getReservedQuantity());
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to consume reservation: {}", reservationId, e);
            throw new RuntimeException("Failed to consume reservation", e);
        }
    }
    
    public void releaseReservation(String reservationId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get reservation
            Optional<InventoryReservation> reservationOpt = reservationRepository.findById(transaction, reservationId);
            if (reservationOpt.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found: " + reservationId);
            }
            
            InventoryReservation reservation = reservationOpt.get();
            if (!ReservationStatus.ACTIVE.name().equals(reservation.getReservationStatus())) {
                throw new InvalidReservationStatusException("Reservation is not active: " + reservationId);
            }
            
            // Get inventory item
            Optional<InventoryItem> itemOpt = inventoryRepository.findById(transaction, reservation.getProductId());
            InventoryItem item = itemOpt.orElseThrow();
            
            // Release reserved quantity back to available
            item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getReservedQuantity());
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQuantity());
            item.setUpdatedAt(System.currentTimeMillis());
            item.setVersion(item.getVersion() + 1);
            
            inventoryRepository.save(transaction, item);
            
            // Update reservation status
            reservation.setReservationStatusEnum(ReservationStatus.CANCELLED);
            reservation.setUpdatedAt(System.currentTimeMillis());
            
            reservationRepository.save(transaction, reservation);
            
            transaction.commit();
            
            log.info("Released reservation: reservationId={}, productId={}, quantity={}", 
                reservationId, reservation.getProductId(), reservation.getReservedQuantity());
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to release reservation: {}", reservationId, e);
            throw new RuntimeException("Failed to release reservation", e);
        }
    }
    
    public Optional<InventoryReservation> getReservation(String reservationId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<InventoryReservation> reservation = reservationRepository.findById(transaction, reservationId);
            transaction.commit();
            return reservation;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get reservation: {}", reservationId, e);
            throw new RuntimeException("Failed to get reservation", e);
        }
    }
    
    public List<InventoryReservation> getReservationsByCustomer(String customerId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            List<InventoryReservation> reservations = reservationRepository.findByCustomerId(transaction, customerId);
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get reservations for customer: {}", customerId, e);
            throw new RuntimeException("Failed to get reservations for customer", e);
        }
    }

    public void confirmReservation(String reservationId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<InventoryReservation> reservationOpt = reservationRepository.findById(transaction, reservationId);
            if (reservationOpt.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found: " + reservationId);
            }

            InventoryReservation reservation = reservationOpt.get();
            if (!ReservationStatus.ACTIVE.name().equals(reservation.getReservationStatus())) {
                throw new InvalidReservationStatusException("Reservation is not active: " + reservationId);
            }

            reservation.setReservationStatusEnum(ReservationStatus.CONFIRMED);
            reservation.setUpdatedAt(System.currentTimeMillis());
            reservationRepository.save(transaction, reservation);

            transaction.commit();

            log.info("Confirmed reservation: {}", reservationId);
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to confirm reservation: {}", reservationId, e);
            throw new RuntimeException("Failed to confirm reservation", e);
        }
    }

    public void cancelReservation(String reservationId) {
        releaseReservation(reservationId);
    }

    public InventoryCheckResponse checkInventory(String productId, int quantity) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<InventoryItem> itemOpt = inventoryRepository.findById(transaction, productId);
            transaction.commit();

            InventoryCheckResponse response = new InventoryCheckResponse();
            response.setProductId(productId);
            if (itemOpt.isPresent()) {
                InventoryItem item = itemOpt.get();
                response.setAvailableQuantity(item.getAvailableQuantity());
                response.setAvailable(item.getAvailableQuantity() >= quantity);
            } else {
                response.setAvailableQuantity(0);
                response.setAvailable(false);
            }
            return response;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to check inventory for product: {}", productId, e);
            throw new RuntimeException("Failed to check inventory", e);
        }
    }
}