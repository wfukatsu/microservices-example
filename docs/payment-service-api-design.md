# 決済サービス API設計書

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Payment Service |
| API種別 | System API |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/payment/v1 |
| 認証方式 | Bearer Token |
| データベース | SQLite + ScalarDB |

## ビジネスドメイン

### ドメイン概要
- **責務**: 決済処理と決済状態の管理
- **境界**: 決済要求、決済実行、返金処理、決済履歴の管理
- **データ所有**: 決済情報、決済履歴、返金記録

### ユースケース
1. 決済の作成・実行・キャンセル
2. 決済状態の照会・更新
3. 返金処理
4. 決済履歴の管理

## データモデル

### ScalarDBスキーマ定義

```json
{
  "payment.payments": {
    "transaction": true,
    "partition-key": ["payment_id"],
    "columns": {
      "payment_id": "TEXT",
      "order_id": "TEXT",
      "customer_id": "TEXT",
      "amount": "BIGINT",
      "currency": "TEXT",
      "payment_method_type": "TEXT",
      "payment_method_id": "TEXT",
      "payment_status": "TEXT",
      "payment_provider": "TEXT",
      "provider_transaction_id": "TEXT",
      "failure_reason": "TEXT",
      "processed_at": "BIGINT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT",
      "version": "INT"
    }
  },
  "payment.payment_methods": {
    "transaction": true,
    "partition-key": ["payment_method_id"],
    "columns": {
      "payment_method_id": "TEXT",
      "customer_id": "TEXT",
      "method_type": "TEXT",
      "card_last_four": "TEXT",
      "card_brand": "TEXT",
      "expiry_month": "INT",
      "expiry_year": "INT",
      "is_default": "BOOLEAN",
      "is_active": "BOOLEAN",
      "provider_method_id": "TEXT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT"
    }
  },
  "payment.refunds": {
    "transaction": true,
    "partition-key": ["refund_id"],
    "clustering-key": ["payment_id"],
    "columns": {
      "refund_id": "TEXT",
      "payment_id": "TEXT",
      "order_id": "TEXT",
      "refund_amount": "BIGINT",
      "currency": "TEXT",
      "refund_reason": "TEXT",
      "refund_status": "TEXT",
      "provider_refund_id": "TEXT",
      "processed_at": "BIGINT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT"
    }
  },
  "payment.payment_events": {
    "transaction": true,
    "partition-key": ["event_id"],
    "clustering-key": ["payment_id", "created_at"],
    "columns": {
      "event_id": "TEXT",
      "payment_id": "TEXT",
      "event_type": "TEXT",
      "previous_status": "TEXT",
      "new_status": "TEXT",
      "event_data": "TEXT",
      "created_at": "BIGINT",
      "created_by": "TEXT"
    }
  }
}
```

### エンティティ定義

#### Payment
```java
@Entity
@Table(name = "payments")
public class Payment {
    @PartitionKey
    private String paymentId;
    
    @Column
    private String orderId;
    
    @Column
    private String customerId;
    
    @Column
    private Long amount;
    
    @Column
    private String currency;
    
    @Column
    private PaymentMethodType paymentMethodType;
    
    @Column
    private String paymentMethodId;
    
    @Column
    private PaymentStatus paymentStatus;
    
    @Column
    private String paymentProvider;
    
    @Column
    private String providerTransactionId;
    
    @Column
    private String failureReason;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private Integer version;
}

public enum PaymentStatus {
    PENDING, AUTHORIZED, CAPTURED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
}

public enum PaymentMethodType {
    CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, DIGITAL_WALLET, CRYPTOCURRENCY
}
```

#### PaymentMethod
```java
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {
    @PartitionKey
    private String paymentMethodId;
    
    @Column
    private String customerId;
    
    @Column
    private PaymentMethodType methodType;
    
    @Column
    private String cardLastFour;
    
    @Column
    private String cardBrand;
    
    @Column
    private Integer expiryMonth;
    
    @Column
    private Integer expiryYear;
    
    @Column
    private Boolean isDefault;
    
    @Column
    private Boolean isActive;
    
    @Column
    private String providerMethodId;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}
```

#### Refund
```java
@Entity
@Table(name = "refunds")
public class Refund {
    @PartitionKey
    private String refundId;
    
    @ClusteringColumn
    private String paymentId;
    
    @Column
    private String orderId;
    
    @Column
    private Long refundAmount;
    
    @Column
    private String currency;
    
    @Column
    private String refundReason;
    
    @Column
    private RefundStatus refundStatus;
    
    @Column
    private String providerRefundId;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}

public enum RefundStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}
```

## API仕様

### System API (CRUD操作)

#### 1. 決済作成
```http
POST /payments
Content-Type: application/json
Authorization: Bearer {token}

{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "amount": 15000,
  "currency": "JPY",
  "payment_method_id": "PM-001",
  "payment_provider": "stripe"
}
```

**レスポンス**:
```json
{
  "payment_id": "PAY-001",
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "amount": 15000,
  "currency": "JPY",
  "payment_method_type": "CREDIT_CARD",
  "payment_method_id": "PM-001",
  "payment_status": "PENDING",
  "payment_provider": "stripe",
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "version": 1
}
```

#### 2. 決済取得
```http
GET /payments/{payment_id}
Authorization: Bearer {token}
```

#### 3. 決済一覧取得
```http
GET /payments?customer_id=CUST-001&status=COMPLETED&page=0&size=20
Authorization: Bearer {token}
```

#### 4. 決済実行
```http
POST /payments/{payment_id}/execute
Content-Type: application/json
Authorization: Bearer {token}

{
  "payment_provider_data": {
    "payment_intent_id": "pi_stripe_12345",
    "confirmation_method": "automatic"
  }
}
```

#### 5. 決済キャンセル
```http
POST /payments/{payment_id}/cancel
Content-Type: application/json
Authorization: Bearer {token}

{
  "cancellation_reason": "Customer requested cancellation"
}
```

#### 6. 返金作成
```http
POST /payments/{payment_id}/refunds
Content-Type: application/json
Authorization: Bearer {token}

{
  "refund_amount": 5000,
  "currency": "JPY",
  "refund_reason": "Partial refund for cancelled items"
}
```

#### 7. 返金取得
```http
GET /refunds/{refund_id}
Authorization: Bearer {token}
```

#### 8. 決済方法登録
```http
POST /payment-methods
Content-Type: application/json
Authorization: Bearer {token}

{
  "customer_id": "CUST-001",
  "method_type": "CREDIT_CARD",
  "card_token": "tok_stripe_12345",
  "is_default": true
}
```

#### 9. 決済方法一覧取得
```http
GET /customers/{customer_id}/payment-methods
Authorization: Bearer {token}
```

## ScalarDBトランザクション設計

### 決済実行処理
```java
@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private PaymentProviderService paymentProviderService;
    
    public Payment executePayment(String paymentId, ExecutePaymentRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 決済情報取得
            Optional<Payment> paymentOpt = paymentRepository.findById(transaction, paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            }
            
            Payment payment = paymentOpt.get();
            
            // 2. 決済状態チェック
            if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
                throw new InvalidPaymentStatusException("Payment is not in pending status");
            }
            
            // 3. 決済方法取得
            Optional<PaymentMethod> paymentMethodOpt = 
                paymentMethodRepository.findById(transaction, payment.getPaymentMethodId());
            PaymentMethod paymentMethod = paymentMethodOpt.orElseThrow();
            
            // 4. 決済プロバイダーで実行
            PaymentProviderResponse providerResponse = paymentProviderService.executePayment(
                PaymentProviderRequest.builder()
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .paymentMethodId(paymentMethod.getProviderMethodId())
                    .orderId(payment.getOrderId())
                    .customerId(payment.getCustomerId())
                    .providerData(request.getPaymentProviderData())
                    .build()
            );
            
            // 5. 決済状態更新
            PaymentStatus newStatus = mapProviderStatusToPaymentStatus(providerResponse.getStatus());
            payment.setPaymentStatus(newStatus);
            payment.setProviderTransactionId(providerResponse.getTransactionId());
            
            if (newStatus == PaymentStatus.FAILED) {
                payment.setFailureReason(providerResponse.getFailureReason());
            } else if (newStatus == PaymentStatus.CAPTURED) {
                payment.setProcessedAt(LocalDateTime.now());
            }
            
            payment.setUpdatedAt(LocalDateTime.now());
            payment.setVersion(payment.getVersion() + 1);
            
            paymentRepository.save(transaction, payment);
            
            // 6. 決済イベント記録
            PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .eventType("PAYMENT_EXECUTED")
                .previousStatus(PaymentStatus.PENDING.name())
                .newStatus(newStatus.name())
                .eventData(objectMapper.writeValueAsString(providerResponse))
                .createdAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();
            
            paymentEventRepository.save(transaction, event);
            
            transaction.commit();
            return payment;
        } catch (Exception e) {
            transaction.abort();
            throw new PaymentServiceException("Failed to execute payment", e);
        }
    }
    
    public Refund processRefund(String paymentId, CreateRefundRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 決済情報取得・検証
            Optional<Payment> paymentOpt = paymentRepository.findById(transaction, paymentId);
            Payment payment = paymentOpt.orElseThrow(() -> 
                new PaymentNotFoundException("Payment not found: " + paymentId));
            
            if (payment.getPaymentStatus() != PaymentStatus.CAPTURED) {
                throw new InvalidPaymentStatusException("Payment cannot be refunded");
            }
            
            // 2. 返金可能金額チェック
            Long totalRefunded = refundRepository.getTotalRefundedAmount(transaction, paymentId);
            Long availableAmount = payment.getAmount() - totalRefunded;
            
            if (request.getRefundAmount() > availableAmount) {
                throw new InvalidRefundAmountException("Refund amount exceeds available amount");
            }
            
            // 3. 返金作成
            Refund refund = Refund.builder()
                .refundId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .orderId(payment.getOrderId())
                .refundAmount(request.getRefundAmount())
                .currency(request.getCurrency())
                .refundReason(request.getRefundReason())
                .refundStatus(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            refundRepository.save(transaction, refund);
            
            // 4. 決済プロバイダーで返金実行
            RefundProviderResponse providerResponse = paymentProviderService.processRefund(
                RefundProviderRequest.builder()
                    .originalTransactionId(payment.getProviderTransactionId())
                    .refundAmount(request.getRefundAmount())
                    .currency(request.getCurrency())
                    .reason(request.getRefundReason())
                    .build()
            );
            
            // 5. 返金状態更新
            RefundStatus newRefundStatus = mapProviderStatusToRefundStatus(providerResponse.getStatus());
            refund.setRefundStatus(newRefundStatus);
            refund.setProviderRefundId(providerResponse.getRefundId());
            
            if (newRefundStatus == RefundStatus.COMPLETED) {
                refund.setProcessedAt(LocalDateTime.now());
            }
            
            refund.setUpdatedAt(LocalDateTime.now());
            refundRepository.save(transaction, refund);
            
            // 6. 決済状態更新（必要に応じて）
            if (newRefundStatus == RefundStatus.COMPLETED) {
                Long newTotalRefunded = totalRefunded + request.getRefundAmount();
                if (newTotalRefunded.equals(payment.getAmount())) {
                    payment.setPaymentStatus(PaymentStatus.REFUNDED);
                } else {
                    payment.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }
                payment.setUpdatedAt(LocalDateTime.now());
                payment.setVersion(payment.getVersion() + 1);
                paymentRepository.save(transaction, payment);
            }
            
            // 7. 決済イベント記録
            PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .eventType("REFUND_PROCESSED")
                .eventData(objectMapper.writeValueAsString(refund))
                .createdAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();
            
            paymentEventRepository.save(transaction, event);
            
            transaction.commit();
            return refund;
        } catch (Exception e) {
            transaction.abort();
            throw new PaymentServiceException("Failed to process refund", e);
        }
    }
}
```

## 設定とデプロイ

### ScalarDB設定

#### scalardb.properties
```properties
# SQLite設定
scalar.db.storage=jdbc
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/payment.db
scalar.db.username=
scalar.db.password=
scalar.db.isolation_level=SNAPSHOT

# トランザクション設定
scalar.db.transaction_manager=consensus-commit
scalar.db.consensus_commit.isolation_level=SNAPSHOT
scalar.db.consensus_commit.serializable_strategy=EXTRA_READ
```

#### application.yml
```yaml
spring:
  application:
    name: payment-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

server:
  port: ${SERVER_PORT:8082}

scalardb:
  properties: classpath:scalardb.properties

# 決済プロバイダー設定
payment:
  providers:
    stripe:
      api-key: ${STRIPE_API_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      api-version: "2023-10-16"
    paypal:
      client-id: ${PAYPAL_CLIENT_ID}
      client-secret: ${PAYPAL_CLIENT_SECRET}
      environment: ${PAYPAL_ENVIRONMENT:sandbox}
  
  default-currency: JPY
  refund:
    max-days: 30
    auto-process: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.scalar.db: DEBUG
    com.example.payment: DEBUG
```

## エラーハンドリング

### カスタム例外
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaymentStatusException extends RuntimeException {
    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRefundAmountException extends RuntimeException {
    public InvalidRefundAmountException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 外部連携

### 決済プロバイダーインターフェース
```java
public interface PaymentProviderService {
    PaymentProviderResponse executePayment(PaymentProviderRequest request);
    RefundProviderResponse processRefund(RefundProviderRequest request);
    void handleWebhook(String providerId, String payload, String signature);
}

@Service
public class StripePaymentProviderService implements PaymentProviderService {
    
    @Autowired
    private StripeService stripe;
    
    @Override
    public PaymentProviderResponse executePayment(PaymentProviderRequest request) {
        try {
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(request.getPaymentMethodId())
                .build();
            
            PaymentIntent intent = PaymentIntent.retrieve(
                request.getProviderData().get("payment_intent_id").toString()
            );
            
            PaymentIntent confirmedIntent = intent.confirm(params);
            
            return PaymentProviderResponse.builder()
                .status(mapStripeStatusToPaymentStatus(confirmedIntent.getStatus()))
                .transactionId(confirmedIntent.getId())
                .build();
        } catch (StripeException e) {
            throw new PaymentProviderException("Stripe payment execution failed", e);
        }
    }
    
    @Override
    public RefundProviderResponse processRefund(RefundProviderRequest request) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(request.getOriginalTransactionId())
                .setAmount(request.getRefundAmount())
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();
            
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(params);
            
            return RefundProviderResponse.builder()
                .status(mapStripeRefundStatusToRefundStatus(refund.getStatus()))
                .refundId(refund.getId())
                .build();
        } catch (StripeException e) {
            throw new PaymentProviderException("Stripe refund processing failed", e);
        }
    }
}
```

## テスト戦略

### ユニットテスト例
```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private PaymentProviderService paymentProviderService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    void executePayment_Success() {
        // Given
        DistributedTransaction transaction = mock(DistributedTransaction.class);
        when(transactionManager.start()).thenReturn(transaction);
        
        Payment payment = Payment.builder()
            .paymentId("PAY-001")
            .paymentStatus(PaymentStatus.PENDING)
            .amount(15000L)
            .currency("JPY")
            .build();
        
        when(paymentRepository.findById(transaction, "PAY-001"))
            .thenReturn(Optional.of(payment));
        
        PaymentProviderResponse providerResponse = PaymentProviderResponse.builder()
            .status("succeeded")
            .transactionId("pi_stripe_12345")
            .build();
        
        when(paymentProviderService.executePayment(any()))
            .thenReturn(providerResponse);
        
        ExecutePaymentRequest request = ExecutePaymentRequest.builder()
            .paymentProviderData(Map.of("payment_intent_id", "pi_stripe_12345"))
            .build();
        
        // When
        Payment result = paymentService.executePayment("PAY-001", request);
        
        // Then
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(result.getProviderTransactionId()).isEqualTo("pi_stripe_12345");
        verify(transaction).commit();
    }
}
```

## 監視とメトリクス

### カスタムメトリクス
```java
@Component
public class PaymentMetrics {
    
    private final Counter paymentCounter;
    private final Counter refundCounter;
    private final Timer paymentProcessingTime;
    private final Gauge failedPaymentRate;
    
    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.paymentCounter = Counter.builder("payments.total")
            .description("Total number of payments")
            .register(meterRegistry);
            
        this.refundCounter = Counter.builder("refunds.total")
            .description("Total number of refunds")
            .register(meterRegistry);
            
        this.paymentProcessingTime = Timer.builder("payments.processing_time")
            .description("Time taken to process payments")
            .register(meterRegistry);
            
        this.failedPaymentRate = Gauge.builder("payments.failure_rate")
            .description("Payment failure rate")
            .register(meterRegistry, this, PaymentMetrics::calculateFailureRate);
    }
    
    public void recordPaymentSuccess(String provider) {
        paymentCounter.increment(Tags.of("status", "success", "provider", provider));
    }
    
    public void recordPaymentFailure(String provider, String reason) {
        paymentCounter.increment(Tags.of("status", "failure", "provider", provider, "reason", reason));
    }
    
    private double calculateFailureRate() {
        // 失敗率を計算するロジック
        return paymentRepository.calculateFailureRate();
    }
}
```

## 実装チェックリスト

### System API実装
- [ ] Payment, PaymentMethod, Refund, PaymentEvent エンティティ定義
- [ ] ScalarDBリポジトリ実装（Payment, PaymentMethod, Refund, Event）
- [ ] PaymentService実装（決済実行・キャンセル・返金処理）
- [ ] PaymentController実装（CRUD + 決済操作）
- [ ] 決済プロバイダー連携実装（Stripe, PayPal等）
- [ ] Webhook処理実装
- [ ] バリデーション実装（金額、通貨、決済方法等）
- [ ] カスタム例外ハンドリング実装
- [ ] ユニットテスト実装
- [ ] 統合テスト実装

### セキュリティ実装
- [ ] 決済情報の暗号化
- [ ] PCI DSS準拠設定
- [ ] Webhookシグネチャ検証
- [ ] レート制限設定

### 運用準備
- [ ] 決済失敗アラート設定
- [ ] 決済メトリクス収集
- [ ] 監査ログ設定
- [ ] 返金処理の自動化設定