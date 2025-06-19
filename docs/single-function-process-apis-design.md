# 単機能Process API設計書

## 概要

各System APIに対応する単機能のProcess APIを設計します。これらのAPIは単一のSystem APIを呼び出すシンプルなProcess APIとして、System APIとの間にトランザクション境界を提供し、外部からの呼び出しを簡素化します。

## アーキテクチャ概要

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│   External Client   │    │ Single-Function      │    │    System API       │
│                     │───▶│   Process API        │───▶│                     │
│                     │    │                      │    │                     │
└─────────────────────┘    └──────────────────────┘    └─────────────────────┘
                                     │
                                     ▼
                           ┌──────────────────────┐
                           │    ScalarDB          │
                           │  Transaction Layer   │
                           └──────────────────────┘
```

---

# 在庫管理Process API

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Inventory Process Service |
| API種別 | Process API (単機能) |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/inventory-process/v1 |
| 認証方式 | Bearer Token |
| 依存System API | Inventory Service |

## API仕様

### 1. 在庫予約プロセス
```http
POST /processes/reserve-inventory
Content-Type: application/json
Authorization: Bearer {token}

{
  "customer_id": "CUST-001",
  "items": [
    {
      "product_id": "PROD-001",
      "quantity": 5
    },
    {
      "product_id": "PROD-002", 
      "quantity": 2
    }
  ],
  "reservation_expiry_hours": 24,
  "reference_id": "ORDER-001"
}
```

**レスポンス**:
```json
{
  "process_id": "IP-001",
  "status": "COMPLETED",
  "reservation_id": "RES-001",
  "reserved_items": [
    {
      "product_id": "PROD-001",
      "requested_quantity": 5,
      "reserved_quantity": 5,
      "status": "RESERVED"
    },
    {
      "product_id": "PROD-002",
      "requested_quantity": 2,
      "reserved_quantity": 2,
      "status": "RESERVED"
    }
  ],
  "expires_at": "2024-01-02T00:00:00Z",
  "created_at": "2024-01-01T00:00:00Z"
}
```

### 2. 在庫確保プロセス
```http
POST /processes/consume-inventory
Content-Type: application/json
Authorization: Bearer {token}

{
  "reservation_id": "RES-001",
  "reference_id": "ORDER-001"
}
```

### 3. 在庫解放プロセス
```http
POST /processes/release-inventory
Content-Type: application/json
Authorization: Bearer {token}

{
  "reservation_id": "RES-001",
  "reason": "Order cancelled",
  "reference_id": "ORDER-001"
}
```

## 実装例

```java
@RestController
@RequestMapping("/processes")
public class InventoryProcessController {
    
    @Autowired
    private InventoryProcessService inventoryProcessService;
    
    @PostMapping("/reserve-inventory")
    public ResponseEntity<InventoryReservationProcessResponse> reserveInventory(
            @RequestBody ReserveInventoryProcessRequest request) {
        InventoryReservationProcessResponse response = inventoryProcessService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/consume-inventory")
    public ResponseEntity<Void> consumeInventory(@RequestBody ConsumeInventoryProcessRequest request) {
        inventoryProcessService.consumeInventory(request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/release-inventory")
    public ResponseEntity<Void> releaseInventory(@RequestBody ReleaseInventoryProcessRequest request) {
        inventoryProcessService.releaseInventory(request);
        return ResponseEntity.ok().build();
    }
}

@Service
public class InventoryProcessService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private InventoryService inventoryService;
    
    public InventoryReservationProcessResponse reserveInventory(ReserveInventoryProcessRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // プロセス記録作成
            String processId = UUID.randomUUID().toString();
            recordProcessStart(transaction, processId, "RESERVE_INVENTORY", request);
            
            // 在庫予約実行
            InventoryReservation reservation = inventoryService.reserveInventoryBatch(
                transaction,
                InventoryReservationRequest.builder()
                    .customerId(request.getCustomerId())
                    .items(request.getItems())
                    .expiryHours(request.getReservationExpiryHours())
                    .referenceId(request.getReferenceId())
                    .build()
            );
            
            // プロセス完了記録
            recordProcessCompletion(transaction, processId, "COMPLETED", reservation);
            
            transaction.commit();
            
            return buildReservationResponse(processId, reservation);
        } catch (Exception e) {
            transaction.abort();
            throw new InventoryProcessException("Inventory reservation process failed", e);
        }
    }
}
```

---

# 決済Process API

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Payment Process Service |
| API種別 | Process API (単機能) |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/payment-process/v1 |
| 認証方式 | Bearer Token |
| 依存System API | Payment Service |

## API仕様

### 1. 決済実行プロセス
```http
POST /processes/execute-payment
Content-Type: application/json
Authorization: Bearer {token}

{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "amount": 15000,
  "currency": "JPY",
  "payment_method_id": "PM-001",
  "payment_provider": "stripe",
  "auto_capture": true,
  "reference_data": {
    "order_reference": "ORDER-001",
    "customer_email": "customer@example.com"
  }
}
```

**レスポンス**:
```json
{
  "process_id": "PP-001",
  "status": "COMPLETED",
  "payment_id": "PAY-001",
  "payment_status": "CAPTURED",
  "amount": 15000,
  "currency": "JPY",
  "payment_method_type": "CREDIT_CARD",
  "provider_transaction_id": "pi_stripe_12345",
  "processed_at": "2024-01-01T00:00:05Z",
  "created_at": "2024-01-01T00:00:00Z"
}
```

### 2. 決済キャンセルプロセス
```http
POST /processes/cancel-payment
Content-Type: application/json
Authorization: Bearer {token}

{
  "payment_id": "PAY-001",
  "cancellation_reason": "Customer requested cancellation",
  "reference_id": "ORDER-001"
}
```

### 3. 返金プロセス
```http
POST /processes/refund-payment
Content-Type: application/json
Authorization: Bearer {token}

{
  "payment_id": "PAY-001",
  "refund_amount": 5000,
  "currency": "JPY",
  "refund_reason": "Partial refund for cancelled items",
  "reference_id": "ORDER-001"
}
```

## 実装例

```java
@Service
public class PaymentProcessService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private PaymentService paymentService;
    
    public PaymentExecutionProcessResponse executePayment(ExecutePaymentProcessRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            String processId = UUID.randomUUID().toString();
            recordProcessStart(transaction, processId, "EXECUTE_PAYMENT", request);
            
            // 決済作成
            Payment payment = paymentService.createPayment(
                transaction,
                CreatePaymentRequest.builder()
                    .orderId(request.getOrderId())
                    .customerId(request.getCustomerId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .paymentMethodId(request.getPaymentMethodId())
                    .build()
            );
            
            // 決済実行
            Payment executedPayment = paymentService.executePayment(
                transaction,
                payment.getPaymentId(),
                ExecutePaymentRequest.builder()
                    .autoCapture(request.getAutoCapture())
                    .paymentProviderData(request.getProviderData())
                    .build()
            );
            
            recordProcessCompletion(transaction, processId, "COMPLETED", executedPayment);
            
            transaction.commit();
            
            return buildPaymentResponse(processId, executedPayment);
        } catch (Exception e) {
            transaction.abort();
            throw new PaymentProcessException("Payment execution process failed", e);
        }
    }
}
```

---

# 配送Process API

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Shipping Process Service |
| API種別 | Process API (単機能) |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/shipping-process/v1 |
| 認証方式 | Bearer Token |
| 依存System API | Shipping Service |

## API仕様

### 1. 配送手配プロセス
```http
POST /processes/arrange-shipping
Content-Type: application/json
Authorization: Bearer {token}

{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "shipping_method": "STANDARD",
  "carrier": "YAMATO",
  "recipient_info": {
    "name": "田中太郎",
    "phone": "090-1234-5678",
    "address": "東京都渋谷区渋谷1-1-1",
    "city": "渋谷区",
    "state": "東京都",
    "postal_code": "150-0002",
    "country": "JP"
  },
  "package_info": {
    "weight": 1.5,
    "dimensions": "30x20x10",
    "special_instructions": "午前中配達希望"
  },
  "items": [
    {
      "product_id": "PROD-001",
      "product_name": "Sample Product",
      "quantity": 2,
      "weight": 0.5,
      "dimensions": "15x10x5"
    }
  ]
}
```

**レスポンス**:
```json
{
  "process_id": "SP-001",
  "status": "COMPLETED",
  "shipment_id": "SHIP-001",
  "tracking_number": "1234567890123",
  "shipping_status": "PROCESSING",
  "carrier": "YAMATO",
  "estimated_delivery_date": "2024-01-03T00:00:00Z",
  "shipping_cost": 800,
  "currency": "JPY",
  "created_at": "2024-01-01T00:00:00Z"
}
```

### 2. 配送キャンセルプロセス
```http
POST /processes/cancel-shipping
Content-Type: application/json
Authorization: Bearer {token}

{
  "shipment_id": "SHIP-001",
  "cancellation_reason": "Order cancelled",
  "reference_id": "ORDER-001"
}
```

### 3. 配送追跡プロセス
```http
POST /processes/track-shipping
Content-Type: application/json
Authorization: Bearer {token}

{
  "shipment_id": "SHIP-001",
  "sync_with_carrier": true
}
```

## 実装例

```java
@Service
public class ShippingProcessService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private ShippingService shippingService;
    
    public ShippingArrangementProcessResponse arrangeShipping(ArrangeShippingProcessRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            String processId = UUID.randomUUID().toString();
            recordProcessStart(transaction, processId, "ARRANGE_SHIPPING", request);
            
            // 配送作成
            Shipment shipment = shippingService.createShipment(
                transaction,
                CreateShipmentRequest.builder()
                    .orderId(request.getOrderId())
                    .customerId(request.getCustomerId())
                    .shippingMethod(request.getShippingMethod())
                    .carrier(request.getCarrier())
                    .recipientInfo(request.getRecipientInfo())
                    .packageInfo(request.getPackageInfo())
                    .items(request.getItems())
                    .build()
            );
            
            recordProcessCompletion(transaction, processId, "COMPLETED", shipment);
            
            transaction.commit();
            
            return buildShippingResponse(processId, shipment);
        } catch (Exception e) {
            transaction.abort();
            throw new ShippingProcessException("Shipping arrangement process failed", e);
        }
    }
}
```

---

## 共通設計パターン

### プロセス記録エンティティ

```java
@Entity
@Table(name = "process_executions")
public class ProcessExecution {
    @PartitionKey
    private String processId;
    
    @Column
    private String processType;
    
    @Column
    private String referenceId;
    
    @Column
    private ProcessStatus status;
    
    @Column
    private String requestData;
    
    @Column
    private String responseData;
    
    @Column
    private String errorMessage;
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column
    private Duration executionTime;
}

public enum ProcessStatus {
    STARTED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}
```

### 共通インターフェース

```java
public interface ProcessService<TRequest, TResponse> {
    TResponse executeProcess(TRequest request);
    ProcessStatus getProcessStatus(String processId);
    void cancelProcess(String processId);
}

@Component
public abstract class BaseProcessService<TRequest, TResponse> implements ProcessService<TRequest, TResponse> {
    
    @Autowired
    protected DistributedTransactionManager transactionManager;
    
    @Autowired
    protected ProcessExecutionRepository processRepository;
    
    protected void recordProcessStart(DistributedTransaction transaction, String processId, 
                                    String processType, TRequest request) {
        ProcessExecution process = ProcessExecution.builder()
            .processId(processId)
            .processType(processType)
            .status(ProcessStatus.STARTED)
            .requestData(serializeRequest(request))
            .startedAt(LocalDateTime.now())
            .build();
        
        processRepository.save(transaction, process);
    }
    
    protected void recordProcessCompletion(DistributedTransaction transaction, String processId, 
                                         String status, Object response) {
        ProcessExecution process = processRepository.findById(transaction, processId)
            .orElseThrow();
        
        process.setStatus(ProcessStatus.valueOf(status));
        process.setResponseData(serializeResponse(response));
        process.setCompletedAt(LocalDateTime.now());
        process.setExecutionTime(Duration.between(process.getStartedAt(), process.getCompletedAt()));
        
        processRepository.save(transaction, process);
    }
    
    protected abstract String serializeRequest(TRequest request);
    protected abstract String serializeResponse(Object response);
}
```

### 共通例外ハンドリング

```java
@ControllerAdvice
public class ProcessApiExceptionHandler {
    
    @ExceptionHandler(InventoryProcessException.class)
    public ResponseEntity<ProcessErrorResponse> handleInventoryProcessException(InventoryProcessException e) {
        return buildErrorResponse("INVENTORY_PROCESS_ERROR", e.getMessage(), 500);
    }
    
    @ExceptionHandler(PaymentProcessException.class)
    public ResponseEntity<ProcessErrorResponse> handlePaymentProcessException(PaymentProcessException e) {
        return buildErrorResponse("PAYMENT_PROCESS_ERROR", e.getMessage(), 500);
    }
    
    @ExceptionHandler(ShippingProcessException.class)
    public ResponseEntity<ProcessErrorResponse> handleShippingProcessException(ShippingProcessException e) {
        return buildErrorResponse("SHIPPING_PROCESS_ERROR", e.getMessage(), 500);
    }
    
    private ResponseEntity<ProcessErrorResponse> buildErrorResponse(String code, String message, int status) {
        ProcessErrorResponse error = ProcessErrorResponse.builder()
            .errorCode(code)
            .errorMessage(message)
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(status).body(error);
    }
}
```

## 設定とデプロイ

### 共通設定

```yaml
# application.yml (各Process APIサービス共通)
spring:
  application:
    name: ${SERVICE_NAME}-process-service

scalardb:
  properties: classpath:scalardb.properties

# System API連携設定
system-apis:
  inventory:
    url: ${INVENTORY_SERVICE_URL:http://localhost:8081}
    timeout: 30s
  payment:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8082}
    timeout: 60s
  shipping:
    url: ${SHIPPING_SERVICE_URL:http://localhost:8083}
    timeout: 45s

# プロセス実行設定
process:
  execution:
    timeout: 120s
    retry:
      max-attempts: 3
      delay: 2s
  recording:
    enabled: true
    retention-days: 90

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## テスト戦略

### Process API統合テスト

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "scalar.db.contact_points=jdbc:sqlite::memory:",
    "system-apis.inventory.url=http://localhost:${wiremock.server.port}"
})
class InventoryProcessIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();
    
    @Test
    void reserveInventoryProcess_Success() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/inventory-items/reserve"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(createInventoryReservationResponse())));
        
        ReserveInventoryProcessRequest request = ReserveInventoryProcessRequest.builder()
            .customerId("CUST-001")
            .items(Arrays.asList(
                ReservationItemRequest.builder()
                    .productId("PROD-001")
                    .quantity(5)
                    .build()
            ))
            .reservationExpiryHours(24)
            .referenceId("ORDER-001")
            .build();
        
        // When
        ResponseEntity<InventoryReservationProcessResponse> response = 
            restTemplate.postForEntity("/processes/reserve-inventory", request, 
                InventoryReservationProcessResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InventoryReservationProcessResponse processResponse = response.getBody();
        assertThat(processResponse.getStatus()).isEqualTo("COMPLETED");
        assertThat(processResponse.getReservationId()).isNotNull();
        assertThat(processResponse.getProcessId()).isNotNull();
    }
}
```

## 実装チェックリスト

### 単機能Process API実装
- [ ] **在庫管理Process API**
  - [ ] InventoryProcessService実装
  - [ ] 在庫予約プロセス実装
  - [ ] 在庫確保プロセス実装  
  - [ ] 在庫解放プロセス実装
  - [ ] プロセス記録・追跡機能実装

- [ ] **決済Process API** 
  - [ ] PaymentProcessService実装
  - [ ] 決済実行プロセス実装
  - [ ] 決済キャンセルプロセス実装
  - [ ] 返金プロセス実装
  - [ ] プロセス記録・追跡機能実装

- [ ] **配送Process API**
  - [ ] ShippingProcessService実装
  - [ ] 配送手配プロセス実装
  - [ ] 配送キャンセルプロセス実装
  - [ ] 配送追跡プロセス実装
  - [ ] プロセス記録・追跡機能実装

### 共通機能実装
- [ ] BaseProcessService抽象クラス実装
- [ ] ProcessExecution共通エンティティ実装
- [ ] 共通例外ハンドリング実装
- [ ] プロセス実行メトリクス実装
- [ ] 統合テスト実装

### 運用準備
- [ ] プロセス実行監視設定
- [ ] System API連携障害監視
- [ ] プロセス実行時間アラート設定
- [ ] ログ集約・分析設定