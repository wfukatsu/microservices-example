package com.example.inventory.controller;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.ReserveInventoryRequest;
import com.example.inventory.dto.InventoryCheckResponse;
import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    
    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/items")
    public ResponseEntity<InventoryItem> createInventoryItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItem item = inventoryService.createInventoryItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }
    
    @GetMapping("/items/{productId}")
    public ResponseEntity<InventoryItem> getInventoryItem(@PathVariable String productId) {
        Optional<InventoryItem> item = inventoryService.getInventoryItem(productId);
        return item.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems(
            @RequestParam(required = false) String status) {
        List<InventoryItem> items = inventoryService.getAllInventoryItems();
        
        // Filter by status if provided
        if (status != null) {
            items = items.stream()
                        .filter(item -> status.equals(item.getStatus()))
                        .toList();
        }
        
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/items/{productId}/reservations")
    public ResponseEntity<InventoryReservation> reserveInventory(
            @PathVariable String productId,
            @Valid @RequestBody ReserveInventoryRequest request) {
        
        request.setProductId(productId);
        InventoryReservation reservation = inventoryService.reserveInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
    
    @PostMapping("/reservations/{reservationId}/consume")
    public ResponseEntity<Void> consumeReservation(@PathVariable String reservationId) {
        inventoryService.consumeReservation(reservationId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable String reservationId) {
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<InventoryReservation> getReservation(@PathVariable String reservationId) {
        Optional<InventoryReservation> reservation = inventoryService.getReservation(reservationId);
        return reservation.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/customers/{customerId}/reservations")
    public ResponseEntity<List<InventoryReservation>> getCustomerReservations(@PathVariable String customerId) {
        List<InventoryReservation> reservations = inventoryService.getReservationsByCustomer(customerId);
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservation> reserveInventoryDirect(@Valid @RequestBody ReserveInventoryRequest request) {
        InventoryReservation reservation = inventoryService.reserveInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("/confirm/{reservationId}")
    public ResponseEntity<Void> confirmReservation(@PathVariable String reservationId) {
        inventoryService.confirmReservation(reservationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String reservationId) {
        inventoryService.cancelReservation(reservationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check")
    public ResponseEntity<InventoryCheckResponse> checkInventory(@RequestParam String productId,
            @RequestParam int quantity) {
        InventoryCheckResponse response = inventoryService.checkInventory(productId, quantity);
        return ResponseEntity.ok(response);
    }
}