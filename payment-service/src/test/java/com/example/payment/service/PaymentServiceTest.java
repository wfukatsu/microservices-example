package com.example.payment.service;

import com.example.payment.dto.CreatePaymentRequest;
import com.example.payment.dto.ExecutePaymentRequest;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private PaymentProviderService paymentProviderService;
    
    @Mock
    private DistributedTransaction transaction;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        when(transactionManager.start()).thenReturn(transaction);
    }
    
    @Test
    void createPayment_Success() throws Exception {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "ORDER-001", "CUST-001", 15000L, "JPY", "PM-001", "stripe");
        
        // When
        Payment result = paymentService.createPayment(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getAmount()).isEqualTo(15000L);
        assertThat(result.getCurrency()).isEqualTo("JPY");
        assertThat(result.getPaymentStatusEnum()).isEqualTo(PaymentStatus.PENDING);
        
        verify(paymentRepository).save(transaction, any(Payment.class));
        verify(transaction).commit();
    }
    
    @Test
    void executePayment_Success() throws Exception {
        // Given
        String paymentId = "PAY-001";
        ExecutePaymentRequest request = new ExecutePaymentRequest(
            Map.of("payment_intent_id", "pi_test"), true);
        
        Payment payment = new Payment(paymentId, "ORDER-001", "CUST-001", 
            15000L, "JPY", "PM-001", "stripe");
        
        when(paymentRepository.findById(transaction, paymentId)).thenReturn(Optional.of(payment));
        
        PaymentProviderResponse providerResponse = PaymentProviderResponse.builder()
            .success(true)
            .transactionId("tx_12345")
            .build();
        
        when(paymentProviderService.executePayment(any())).thenReturn(providerResponse);
        
        // When
        Payment result = paymentService.executePayment(paymentId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatusEnum()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(result.getProviderTransactionId()).isEqualTo("tx_12345");
        
        verify(paymentRepository).save(transaction, any(Payment.class));
        verify(transaction).commit();
    }
    
    @Test
    void executePayment_PaymentNotFound_ThrowsException() throws Exception {
        // Given
        String paymentId = "PAY-999";
        ExecutePaymentRequest request = new ExecutePaymentRequest();
        
        when(paymentRepository.findById(transaction, paymentId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> paymentService.executePayment(paymentId, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to execute payment");
        
        verify(transaction).abort();
    }
    
    @Test
    void executePayment_ProviderFailure_UpdatesPaymentStatus() throws Exception {
        // Given
        String paymentId = "PAY-001";
        ExecutePaymentRequest request = new ExecutePaymentRequest();
        
        Payment payment = new Payment(paymentId, "ORDER-001", "CUST-001", 
            15000L, "JPY", "PM-001", "stripe");
        
        when(paymentRepository.findById(transaction, paymentId)).thenReturn(Optional.of(payment));
        
        PaymentProviderResponse providerResponse = PaymentProviderResponse.builder()
            .success(false)
            .failureReason("Insufficient funds")
            .build();
        
        when(paymentProviderService.executePayment(any())).thenReturn(providerResponse);
        
        // When
        Payment result = paymentService.executePayment(paymentId, request);
        
        // Then
        assertThat(result.getPaymentStatusEnum()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Insufficient funds");
        
        verify(paymentRepository).save(transaction, any(Payment.class));
        verify(transaction).commit();
    }
    
    @Test
    void getPayment_Success() throws Exception {
        // Given
        String paymentId = "PAY-001";
        Payment payment = new Payment(paymentId, "ORDER-001", "CUST-001", 
            15000L, "JPY", "PM-001", "stripe");
        
        when(paymentRepository.findById(transaction, paymentId)).thenReturn(Optional.of(payment));
        
        // When
        Optional<Payment> result = paymentService.getPayment(paymentId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo(paymentId);
        
        verify(transaction).commit();
    }
    
    @Test
    void cancelPayment_Success() throws Exception {
        // Given
        String paymentId = "PAY-001";
        Payment payment = new Payment(paymentId, "ORDER-001", "CUST-001", 
            15000L, "JPY", "PM-001", "stripe");
        payment.setPaymentStatusEnum(PaymentStatus.PENDING);
        
        when(paymentRepository.findById(transaction, paymentId)).thenReturn(Optional.of(payment));
        
        // When
        paymentService.cancelPayment(paymentId);
        
        // Then
        verify(paymentRepository).save(transaction, any(Payment.class));
        verify(transaction).commit();
    }
}