package com.example.inventory.service;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.ReserveInventoryRequest;
import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.exception.InsufficientInventoryException;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private DistributedTransaction transaction;
    
    @InjectMocks
    private InventoryService inventoryService;
    
    @BeforeEach
    void setUp() {
        when(transactionManager.start()).thenReturn(transaction);
    }
    
    @Test
    void createInventoryItem_Success() throws Exception {
        // Given
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
            "PROD-001", "Test Product", 100, 1500L, "JPY");
        
        when(inventoryRepository.existsById(transaction, "PROD-001")).thenReturn(false);
        
        // When
        InventoryItem result = inventoryService.createInventoryItem(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("PROD-001");
        assertThat(result.getProductName()).isEqualTo("Test Product");
        assertThat(result.getTotalQuantity()).isEqualTo(100);
        assertThat(result.getAvailableQuantity()).isEqualTo(100);
        assertThat(result.getReservedQuantity()).isEqualTo(0);
        
        verify(inventoryRepository).save(transaction, any(InventoryItem.class));
        verify(transaction).commit();
    }
    
    @Test
    void createInventoryItem_ProductAlreadyExists_ThrowsException() throws Exception {
        // Given
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
            "PROD-001", "Test Product", 100, 1500L, "JPY");
        
        when(inventoryRepository.existsById(transaction, "PROD-001")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> inventoryService.createInventoryItem(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create inventory item");
        
        verify(transaction).abort();
    }
    
    @Test
    void reserveInventory_Success() throws Exception {
        // Given
        ReserveInventoryRequest request = new ReserveInventoryRequest(
            "PROD-001", "CUST-001", 5);
        request.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        InventoryItem item = new InventoryItem("PROD-001", "Test Product", 100, 1500L, "JPY");
        
        when(inventoryRepository.findById(transaction, "PROD-001")).thenReturn(Optional.of(item));
        
        // When
        InventoryReservation result = inventoryService.reserveInventory(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("PROD-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getReservedQuantity()).isEqualTo(5);
        
        verify(reservationRepository).save(transaction, any(InventoryReservation.class));
        verify(inventoryRepository).save(transaction, any(InventoryItem.class));
        verify(transaction).commit();
    }
    
    @Test
    void reserveInventory_InsufficientQuantity_ThrowsException() throws Exception {
        // Given
        ReserveInventoryRequest request = new ReserveInventoryRequest(
            "PROD-001", "CUST-001", 150);
        
        InventoryItem item = new InventoryItem("PROD-001", "Test Product", 100, 1500L, "JPY");
        
        when(inventoryRepository.findById(transaction, "PROD-001")).thenReturn(Optional.of(item));
        
        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to reserve inventory");
        
        verify(transaction).abort();
    }
    
    @Test
    void getInventoryItem_Success() throws Exception {
        // Given
        InventoryItem item = new InventoryItem("PROD-001", "Test Product", 100, 1500L, "JPY");
        when(inventoryRepository.findById(transaction, "PROD-001")).thenReturn(Optional.of(item));
        
        // When
        Optional<InventoryItem> result = inventoryService.getInventoryItem("PROD-001");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("PROD-001");
        
        verify(transaction).commit();
    }
    
    @Test
    void getInventoryItem_NotFound() throws Exception {
        // Given
        when(inventoryRepository.findById(transaction, "PROD-999")).thenReturn(Optional.empty());
        
        // When
        Optional<InventoryItem> result = inventoryService.getInventoryItem("PROD-999");
        
        // Then
        assertThat(result).isEmpty();
        
        verify(transaction).commit();
    }
}