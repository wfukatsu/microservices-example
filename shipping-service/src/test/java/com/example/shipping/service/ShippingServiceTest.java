package com.example.shipping.service;

import com.example.shipping.dto.CreateShipmentRequest;
import com.example.shipping.dto.UpdateShippingStatusRequest;
import com.example.shipping.entity.Shipment;
import com.example.shipping.entity.ShippingStatus;
import com.example.shipping.repository.ShipmentRepository;
import com.example.shipping.repository.ShippingItemRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private ShipmentRepository shipmentRepository;
    
    @Mock
    private ShippingItemRepository shippingItemRepository;
    
    @Mock
    private CarrierIntegrationService carrierIntegrationService;
    
    @Mock
    private DistributedTransaction transaction;
    
    @InjectMocks
    private ShippingService shippingService;
    
    @BeforeEach
    void setUp() {
        when(transactionManager.start()).thenReturn(transaction);
    }
    
    @Test
    void createShipment_Success() throws Exception {
        // Given
        CreateShipmentRequest request = createTestShipmentRequest();
        
        CarrierShipmentResponse carrierResponse = CarrierShipmentResponse.builder()
            .trackingNumber("ST123456789012")
            .estimatedDeliveryDate(LocalDateTime.now().plusDays(3))
            .carrierShipmentId("carrier_12345")
            .build();
        
        when(carrierIntegrationService.createShipment(any())).thenReturn(carrierResponse);
        
        // When
        Shipment result = shippingService.createShipment(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getCarrier()).isEqualTo("YAMATO");
        assertThat(result.getTrackingNumber()).isEqualTo("ST123456789012");
        assertThat(result.getShippingStatusEnum()).isEqualTo(ShippingStatus.PROCESSING);
        
        verify(shipmentRepository).save(transaction, any(Shipment.class));
        verify(shippingItemRepository).saveAll(transaction, any());
        verify(transaction).commit();
    }
    
    @Test
    void createShipment_CarrierFailure_StillCreatesShipment() throws Exception {
        // Given
        CreateShipmentRequest request = createTestShipmentRequest();
        
        when(carrierIntegrationService.createShipment(any()))
            .thenThrow(new RuntimeException("Carrier API unavailable"));
        
        // When
        Shipment result = shippingService.createShipment(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShippingStatusEnum()).isEqualTo(ShippingStatus.PENDING);
        assertThat(result.getTrackingNumber()).isNull();
        
        verify(shipmentRepository).save(transaction, any(Shipment.class));
        verify(transaction).commit();
    }
    
    @Test
    void updateShippingStatus_Success() throws Exception {
        // Given
        String shipmentId = "SHIP-001";
        UpdateShippingStatusRequest request = new UpdateShippingStatusRequest("SHIPPED");
        request.setTrackingNumber("ST123456789012");
        
        Shipment shipment = new Shipment(shipmentId, "ORDER-001", "CUST-001", "YAMATO");
        shipment.setShippingStatusEnum(ShippingStatus.PROCESSING);
        
        when(shipmentRepository.findById(transaction, shipmentId)).thenReturn(Optional.of(shipment));
        
        // When
        Shipment result = shippingService.updateShippingStatus(shipmentId, request);
        
        // Then
        assertThat(result.getShippingStatusEnum()).isEqualTo(ShippingStatus.SHIPPED);
        assertThat(result.getTrackingNumber()).isEqualTo("ST123456789012");
        
        verify(shipmentRepository).save(transaction, any(Shipment.class));
        verify(transaction).commit();
    }
    
    @Test
    void updateShippingStatus_InvalidTransition_ThrowsException() throws Exception {
        // Given
        String shipmentId = "SHIP-001";
        UpdateShippingStatusRequest request = new UpdateShippingStatusRequest("PENDING");
        
        Shipment shipment = new Shipment(shipmentId, "ORDER-001", "CUST-001", "YAMATO");
        shipment.setShippingStatusEnum(ShippingStatus.DELIVERED);
        
        when(shipmentRepository.findById(transaction, shipmentId)).thenReturn(Optional.of(shipment));
        
        // When & Then
        assertThatThrownBy(() -> shippingService.updateShippingStatus(shipmentId, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to update shipping status");
        
        verify(transaction).abort();
    }
    
    @Test
    void getShipment_Success() throws Exception {
        // Given
        String shipmentId = "SHIP-001";
        Shipment shipment = new Shipment(shipmentId, "ORDER-001", "CUST-001", "YAMATO");
        
        when(shipmentRepository.findById(transaction, shipmentId)).thenReturn(Optional.of(shipment));
        
        // When
        Optional<Shipment> result = shippingService.getShipment(shipmentId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getShipmentId()).isEqualTo(shipmentId);
        
        verify(transaction).commit();
    }
    
    @Test
    void cancelShipment_Success() throws Exception {
        // Given
        String shipmentId = "SHIP-001";
        Shipment shipment = new Shipment(shipmentId, "ORDER-001", "CUST-001", "YAMATO");
        shipment.setShippingStatusEnum(ShippingStatus.PROCESSING);
        shipment.setTrackingNumber("ST123456789012");
        
        when(shipmentRepository.findById(transaction, shipmentId)).thenReturn(Optional.of(shipment));
        
        // When
        shippingService.cancelShipment(shipmentId);
        
        // Then
        verify(carrierIntegrationService).cancelShipment("YAMATO", "ST123456789012");
        verify(shipmentRepository).save(transaction, any(Shipment.class));
        verify(transaction).commit();
    }
    
    private CreateShipmentRequest createTestShipmentRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setOrderId("ORDER-001");
        request.setCustomerId("CUST-001");
        request.setShippingMethod("STANDARD");
        request.setCarrier("YAMATO");
        
        CreateShipmentRequest.RecipientInfo recipientInfo = new CreateShipmentRequest.RecipientInfo();
        recipientInfo.setName("田中太郎");
        recipientInfo.setPhone("090-1234-5678");
        recipientInfo.setAddress("東京都渋谷区渋谷1-1-1");
        recipientInfo.setCity("渋谷区");
        recipientInfo.setState("東京都");
        recipientInfo.setPostalCode("150-0002");
        recipientInfo.setCountry("JP");
        request.setRecipientInfo(recipientInfo);
        
        CreateShipmentRequest.PackageInfo packageInfo = new CreateShipmentRequest.PackageInfo();
        packageInfo.setWeight(1.5);
        packageInfo.setDimensions("30x20x10");
        packageInfo.setSpecialInstructions("午前中配達希望");
        request.setPackageInfo(packageInfo);
        
        CreateShipmentRequest.ShippingItemRequest item = new CreateShipmentRequest.ShippingItemRequest();
        item.setProductId("PROD-001");
        item.setProductName("Test Product");
        item.setQuantity(1);
        item.setWeight(1.5);
        
        request.setItems(new ArrayList<>());
        request.getItems().add(item);
        
        return request;
    }
}