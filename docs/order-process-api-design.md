# 注文サービス API設計書

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Order Service |
| API種別 | Process API |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/order/v1 |
| 認証方式 | Bearer Token |
| データベース | SQLite + ScalarDB |

## ビジネスドメイン

### ドメイン概要
- **責務**: 複数のSystem APIを連携して注文プロセス全体を管理
- **境界**: 注文のライフサイクル全体（在庫確保→決済処理→配送手配）
- **データ所有**: 注文情報、注文ステータス、プロセス履歴

### ユースケース
1. 注文の作成と全プロセスの実行
2. 注文状況の照会と管理
3. 注文のキャンセルと補償処理
4. 注文履歴の管理

## 外部依存サービス

### System API依存関係
- **Inventory Service**: 在庫予約・確保・解放
- **Payment Service**: 決済処理・返金
- **Shipping Service**: 配送手配・追跡

## データモデル

### ScalarDBスキーマ定義

```json
{
  "order.orders": {
    "transaction": true,
    "partition-key": ["order_id"],
    "columns": {
      "order_id": "TEXT",
      "customer_id": "TEXT",
      "order_status": "TEXT",
      "total_amount": "BIGINT",
      "currency": "TEXT",
      "payment_method_id": "TEXT",
      "shipping_address_id": "TEXT",
      "inventory_reservation_id": "TEXT",
      "payment_id": "TEXT",
      "shipment_id": "TEXT",
      "order_notes": "TEXT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT",
      "version": "INT"
    }
  },
  "order.order_items": {
    "transaction": true,
    "partition-key": ["order_id"],
    "clustering-key": ["item_id"],
    "columns": {
      "order_id": "TEXT",
      "item_id": "TEXT",
      "product_id": "TEXT",
      "product_name": "TEXT",
      "quantity": "INT",
      "unit_price": "BIGINT",
      "total_price": "BIGINT",
      "currency": "TEXT"
    }
  },
  "order.order_process_events": {
    "transaction": true,
    "partition-key": ["event_id"],
    "clustering-key": ["order_id", "created_at"],
    "columns": {
      "event_id": "TEXT",
      "order_id": "TEXT",
      "process_step": "TEXT",
      "event_type": "TEXT",
      "event_status": "TEXT",
      "event_data": "TEXT",
      "error_message": "TEXT",
      "retry_count": "INT",
      "created_at": "BIGINT",
      "created_by": "TEXT"
    }
  },
  "order.order_compensation": {
    "transaction": true,
    "partition-key": ["compensation_id"],
    "clustering-key": ["order_id"],
    "columns": {
      "compensation_id": "TEXT",
      "order_id": "TEXT",
      "compensation_type": "TEXT",
      "compensation_status": "TEXT",
      "failed_step": "TEXT",
      "inventory_rollback_status": "TEXT",
      "payment_rollback_status": "TEXT",
      "shipping_rollback_status": "TEXT",
      "compensation_reason": "TEXT",
      "created_at": "BIGINT",
      "completed_at": "BIGINT"
    }
  }
}
```

### エンティティ定義

#### Order
```java
@Entity
@Table(name = "orders")
public class Order {
    @PartitionKey
    private String orderId;
    
    @Column
    private String customerId;
    
    @Column
    private OrderStatus orderStatus;
    
    @Column
    private Long totalAmount;
    
    @Column
    private String currency;
    
    @Column
    private String paymentMethodId;
    
    @Column
    private String shippingAddressId;
    
    @Column
    private String inventoryReservationId;
    
    @Column
    private String paymentId;
    
    @Column
    private String shipmentId;
    
    @Column
    private String orderNotes;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private Integer version;
}

public enum OrderStatus {
    DRAFT, PROCESSING, PAYMENT_PENDING, PAYMENT_FAILED, PAID, 
    FULFILLMENT_PENDING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
}
```

#### OrderItem
```java
@Entity
@Table(name = "order_items")
public class OrderItem {
    @PartitionKey
    private String orderId;
    
    @ClusteringColumn
    private String itemId;
    
    @Column
    private String productId;
    
    @Column
    private String productName;
    
    @Column
    private Integer quantity;
    
    @Column
    private Long unitPrice;
    
    @Column
    private Long totalPrice;
    
    @Column
    private String currency;
}
```

#### OrderProcessEvent
```java
@Entity
@Table(name = "order_process_events")
public class OrderProcessEvent {
    @PartitionKey
    private String eventId;
    
    @ClusteringColumn(0)
    private String orderId;
    
    @ClusteringColumn(1)
    private LocalDateTime createdAt;
    
    @Column
    private String processStep;
    
    @Column
    private String eventType;
    
    @Column
    private String eventStatus;
    
    @Column
    private String eventData;
    
    @Column
    private String errorMessage;
    
    @Column
    private Integer retryCount;
    
    @Column
    private String createdBy;
}
```

#### OrderCompensation
```java
@Entity
@Table(name = "order_compensation")
public class OrderCompensation {
    @PartitionKey
    private String compensationId;
    
    @ClusteringColumn
    private String orderId;
    
    @Column
    private CompensationType compensationType;
    
    @Column
    private CompensationStatus compensationStatus;
    
    @Column
    private String failedStep;
    
    @Column
    private String inventoryRollbackStatus;
    
    @Column
    private String paymentRollbackStatus;
    
    @Column
    private String shippingRollbackStatus;
    
    @Column
    private String compensationReason;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime completedAt;
}

public enum CompensationType {
    FULL_ROLLBACK, PARTIAL_ROLLBACK, MANUAL_INTERVENTION
}

public enum CompensationStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}
```

## API仕様

### Process API (複合操作)

#### 1. 注文作成・実行
```http
POST /orders
Content-Type: application/json
Authorization: Bearer {token}

{
  "customer_id": "CUST-001",
  "payment_method_id": "PM-001",
  "shipping_address": {
    "recipient_name": "田中太郎",
    "phone": "090-1234-5678",
    "address": "東京都渋谷区渋谷1-1-1",
    "city": "渋谷区",
    "state": "東京都",
    "postal_code": "150-0002",
    "country": "JP"
  },
  "items": [
    {
      "product_id": "PROD-001",
      "product_name": "Sample Product",
      "quantity": 2,
      "unit_price": 1500
    },
    {
      "product_id": "PROD-002", 
      "product_name": "Another Product",
      "quantity": 1,
      "unit_price": 3000
    }
  ],
  "shipping_method": "STANDARD",
  "order_notes": "午前中配達希望"
}
```

**レスポンス**:
```json
{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "order_status": "PROCESSING",
  "total_amount": 6000,
  "currency": "JPY",
  "payment_method_id": "PM-001",
  "process_status": {
    "inventory_reservation": "PENDING",
    "payment_processing": "PENDING",
    "shipping_arrangement": "PENDING"
  },
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "version": 1
}
```

#### 2. 注文取得
```http
GET /orders/{order_id}
Authorization: Bearer {token}
```

**レスポンス**:
```json
{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "order_status": "SHIPPED",
  "total_amount": 6000,
  "currency": "JPY",
  "items": [
    {
      "item_id": "ITEM-001",
      "product_id": "PROD-001",
      "product_name": "Sample Product",
      "quantity": 2,
      "unit_price": 1500,
      "total_price": 3000
    }
  ],
  "process_details": {
    "inventory_reservation_id": "RES-001",
    "payment_id": "PAY-001",
    "shipment_id": "SHIP-001"
  },
  "tracking_info": {
    "tracking_number": "1234567890123",
    "carrier": "YAMATO",
    "estimated_delivery": "2024-01-03T00:00:00Z"
  },
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T10:00:00Z",
  "version": 5
}
```

#### 3. 注文一覧取得
```http
GET /orders?customer_id=CUST-001&status=SHIPPED&page=0&size=20
Authorization: Bearer {token}
```

#### 4. 注文キャンセル
```http
POST /orders/{order_id}/cancel
Content-Type: application/json
Authorization: Bearer {token}

{
  "cancellation_reason": "Customer requested cancellation",
  "refund_required": true
}
```

#### 5. 注文プロセス履歴取得
```http
GET /orders/{order_id}/process-events
Authorization: Bearer {token}
```

**レスポンス**:
```json
{
  "order_id": "ORDER-001",
  "events": [
    {
      "event_id": "EVT-001",
      "process_step": "INVENTORY_RESERVATION",
      "event_type": "STEP_STARTED",
      "event_status": "SUCCESS",
      "created_at": "2024-01-01T00:00:01Z"
    },
    {
      "event_id": "EVT-002",
      "process_step": "PAYMENT_PROCESSING",
      "event_type": "STEP_STARTED",
      "event_status": "SUCCESS",
      "created_at": "2024-01-01T00:00:05Z"
    },
    {
      "event_id": "EVT-003",
      "process_step": "SHIPPING_ARRANGEMENT",
      "event_type": "STEP_STARTED",
      "event_status": "SUCCESS",
      "created_at": "2024-01-01T00:00:10Z"
    }
  ]
}
```

#### 6. 注文再試行
```http
POST /orders/{order_id}/retry
Content-Type: application/json
Authorization: Bearer {token}

{
  "retry_from_step": "PAYMENT_PROCESSING"
}
```

## ScalarDBトランザクション設計

### 注文プロセス実行
```java
@Service
public class OrderProcessService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private InventoryServiceClient inventoryService;
    
    @Autowired
    private PaymentServiceClient paymentService;
    
    @Autowired
    private ShippingServiceClient shippingService;
    
    public Order processOrder(CreateOrderRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 注文作成
            Order order = createOrder(transaction, request);
            
            // 2. 分散トランザクション実行
            executeOrderProcess(transaction, order, request);
            
            transaction.commit();
            return order;
        } catch (Exception e) {
            transaction.abort();
            // 補償処理を非同期で実行
            initiateCompensation(order.getOrderId(), e);
            throw new OrderProcessException("Order processing failed", e);
        }
    }
    
    private void executeOrderProcess(DistributedTransaction transaction, Order order, CreateOrderRequest request) {
        String orderId = order.getOrderId();
        
        try {
            // Step 1: 在庫予約
            recordProcessEvent(transaction, orderId, "INVENTORY_RESERVATION", "STEP_STARTED", "SUCCESS");
            
            InventoryReservationResponse inventoryResponse = inventoryService.reserveInventoryWithTransaction(
                transaction,
                InventoryReservationRequest.builder()
                    .customerId(order.getCustomerId())
                    .items(request.getItems().stream()
                        .map(item -> ReservationItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                        .collect(Collectors.toList()))
                    .reservationExpiry(LocalDateTime.now().plusHours(24))
                    .build()
            );
            
            order.setInventoryReservationId(inventoryResponse.getReservationId());
            recordProcessEvent(transaction, orderId, "INVENTORY_RESERVATION", "STEP_COMPLETED", "SUCCESS");
            
            // Step 2: 決済処理
            recordProcessEvent(transaction, orderId, "PAYMENT_PROCESSING", "STEP_STARTED", "SUCCESS");
            
            PaymentResponse paymentResponse = paymentService.processPaymentWithTransaction(
                transaction,
                PaymentRequest.builder()
                    .orderId(orderId)
                    .customerId(order.getCustomerId())
                    .amount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .paymentMethodId(order.getPaymentMethodId())
                    .build()
            );
            
            order.setPaymentId(paymentResponse.getPaymentId());
            recordProcessEvent(transaction, orderId, "PAYMENT_PROCESSING", "STEP_COMPLETED", "SUCCESS");
            
            // Step 3: 配送手配
            recordProcessEvent(transaction, orderId, "SHIPPING_ARRANGEMENT", "STEP_STARTED", "SUCCESS");
            
            ShippingResponse shippingResponse = shippingService.createShipmentWithTransaction(
                transaction,
                ShippingRequest.builder()
                    .orderId(orderId)
                    .customerId(order.getCustomerId())
                    .shippingAddress(request.getShippingAddress())
                    .items(request.getItems())
                    .shippingMethod(request.getShippingMethod())
                    .build()
            );
            
            order.setShipmentId(shippingResponse.getShipmentId());
            order.setOrderStatus(OrderStatus.PROCESSING);
            recordProcessEvent(transaction, orderId, "SHIPPING_ARRANGEMENT", "STEP_COMPLETED", "SUCCESS");
            
            // 4. 注文状態更新
            order.setUpdatedAt(LocalDateTime.now());
            order.setVersion(order.getVersion() + 1);
            orderRepository.save(transaction, order);
            
            recordProcessEvent(transaction, orderId, "ORDER_PROCESSING", "PROCESS_COMPLETED", "SUCCESS");
            
        } catch (InventoryServiceException e) {
            recordProcessEvent(transaction, orderId, "INVENTORY_RESERVATION", "STEP_FAILED", "FAILED", e.getMessage());
            throw new OrderProcessException("Inventory reservation failed", e);
        } catch (PaymentServiceException e) {
            recordProcessEvent(transaction, orderId, "PAYMENT_PROCESSING", "STEP_FAILED", "FAILED", e.getMessage());
            throw new OrderProcessException("Payment processing failed", e);
        } catch (ShippingServiceException e) {
            recordProcessEvent(transaction, orderId, "SHIPPING_ARRANGEMENT", "STEP_FAILED", "FAILED", e.getMessage());
            throw new OrderProcessException("Shipping arrangement failed", e);
        }
    }
    
    public void cancelOrder(String orderId, CancelOrderRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 注文取得
            Optional<Order> orderOpt = orderRepository.findById(transaction, orderId);
            Order order = orderOpt.orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            
            // 2. キャンセル可能状態チェック
            if (!isCancellable(order.getOrderStatus())) {
                throw new OrderNotCancellableException("Order cannot be cancelled in current status: " + order.getOrderStatus());
            }
            
            // 3. 各サービスでのキャンセル処理
            CompensationResult compensationResult = executeCompensation(transaction, order, CompensationType.FULL_ROLLBACK);
            
            // 4. 注文状態更新
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            order.setVersion(order.getVersion() + 1);
            orderRepository.save(transaction, order);
            
            // 5. 補償記録作成
            OrderCompensation compensation = OrderCompensation.builder()
                .compensationId(UUID.randomUUID().toString())
                .orderId(orderId)
                .compensationType(CompensationType.FULL_ROLLBACK)
                .compensationStatus(CompensationStatus.COMPLETED)
                .inventoryRollbackStatus(compensationResult.getInventoryRollbackStatus())
                .paymentRollbackStatus(compensationResult.getPaymentRollbackStatus())
                .shippingRollbackStatus(compensationResult.getShippingRollbackStatus())
                .compensationReason(request.getCancellationReason())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
            
            compensationRepository.save(transaction, compensation);
            
            recordProcessEvent(transaction, orderId, "ORDER_CANCELLATION", "PROCESS_COMPLETED", "SUCCESS");
            
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw new OrderProcessException("Order cancellation failed", e);
        }
    }
    
    private CompensationResult executeCompensation(DistributedTransaction transaction, Order order, CompensationType type) {
        CompensationResult.Builder resultBuilder = CompensationResult.builder();
        
        try {
            // 配送キャンセル
            if (order.getShipmentId() != null) {
                shippingService.cancelShipmentWithTransaction(transaction, order.getShipmentId());
                resultBuilder.shippingRollbackStatus("SUCCESS");
            }
            
            // 決済返金/キャンセル
            if (order.getPaymentId() != null) {
                if (order.getOrderStatus() == OrderStatus.PAID) {
                    paymentService.refundPaymentWithTransaction(transaction, order.getPaymentId(), order.getTotalAmount());
                } else {
                    paymentService.cancelPaymentWithTransaction(transaction, order.getPaymentId());
                }
                resultBuilder.paymentRollbackStatus("SUCCESS");
            }
            
            // 在庫解放
            if (order.getInventoryReservationId() != null) {
                inventoryService.releaseReservationWithTransaction(transaction, order.getInventoryReservationId());
                resultBuilder.inventoryRollbackStatus("SUCCESS");
            }
            
        } catch (Exception e) {
            log.error("Compensation execution failed for order: " + order.getOrderId(), e);
            resultBuilder.inventoryRollbackStatus("FAILED")
                        .paymentRollbackStatus("FAILED")
                        .shippingRollbackStatus("FAILED");
        }
        
        return resultBuilder.build();
    }
    
    private void recordProcessEvent(DistributedTransaction transaction, String orderId, String processStep, 
                                  String eventType, String eventStatus) {
        recordProcessEvent(transaction, orderId, processStep, eventType, eventStatus, null);
    }
    
    private void recordProcessEvent(DistributedTransaction transaction, String orderId, String processStep, 
                                  String eventType, String eventStatus, String errorMessage) {
        OrderProcessEvent event = OrderProcessEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .processStep(processStep)
            .eventType(eventType)
            .eventStatus(eventStatus)
            .errorMessage(errorMessage)
            .retryCount(0)
            .createdAt(LocalDateTime.now())
            .createdBy("SYSTEM")
            .build();
        
        processEventRepository.save(transaction, event);
    }
}
```

### サービス間通信インターフェース

#### InventoryServiceClient
```java
@FeignClient(name = "inventory-service", url = "${services.inventory.url}")
public interface InventoryServiceClient {
    
    @PostMapping("/inventory-items/reserve")
    InventoryReservationResponse reserveInventory(@RequestBody InventoryReservationRequest request);
    
    @PostMapping("/reservations/{reservationId}/consume")
    void consumeReservation(@PathVariable String reservationId);
    
    @DeleteMapping("/reservations/{reservationId}")
    void releaseReservation(@PathVariable String reservationId);
    
    // ScalarDBトランザクション対応版
    default InventoryReservationResponse reserveInventoryWithTransaction(
            DistributedTransaction transaction, InventoryReservationRequest request) {
        // ScalarDBトランザクションを使用した在庫予約処理の実装
        return reserveInventory(request);
    }
}
```

#### PaymentServiceClient
```java
@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentServiceClient {
    
    @PostMapping("/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
    
    @PostMapping("/payments/{paymentId}/execute")
    PaymentResponse executePayment(@PathVariable String paymentId, @RequestBody ExecutePaymentRequest request);
    
    @PostMapping("/payments/{paymentId}/cancel")
    void cancelPayment(@PathVariable String paymentId);
    
    @PostMapping("/payments/{paymentId}/refunds")
    RefundResponse refundPayment(@PathVariable String paymentId, @RequestBody RefundRequest request);
    
    // ScalarDBトランザクション対応版
    default PaymentResponse processPaymentWithTransaction(
            DistributedTransaction transaction, PaymentRequest request) {
        // ScalarDBトランザクションを使用した決済処理の実装
        PaymentResponse createResponse = createPayment(request);
        return executePayment(createResponse.getPaymentId(), 
            ExecutePaymentRequest.builder().build());
    }
}
```

#### ShippingServiceClient
```java
@FeignClient(name = "shipping-service", url = "${services.shipping.url}")
public interface ShippingServiceClient {
    
    @PostMapping("/shipments")
    ShippingResponse createShipment(@RequestBody ShippingRequest request);
    
    @PostMapping("/shipments/{shipmentId}/cancel")
    void cancelShipment(@PathVariable String shipmentId);
    
    @GetMapping("/shipments/{shipmentId}")
    ShippingResponse getShipment(@PathVariable String shipmentId);
    
    // ScalarDBトランザクション対応版
    default ShippingResponse createShipmentWithTransaction(
            DistributedTransaction transaction, ShippingRequest request) {
        // ScalarDBトランザクションを使用した配送手配処理の実装
        return createShipment(request);
    }
}
```

## 設定とデプロイ

### ScalarDB設定

#### scalardb.properties
```properties
# SQLite設定
scalar.db.storage=jdbc
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/order.db
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
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

server:
  port: ${SERVER_PORT:8080}

scalardb:
  properties: classpath:scalardb.properties

# 外部サービス設定
services:
  inventory:
    url: ${INVENTORY_SERVICE_URL:http://localhost:8081}
    timeout: 30s
    retry:
      max-attempts: 3
      delay: 1s
  payment:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8082}
    timeout: 60s
    retry:
      max-attempts: 3
      delay: 2s
  shipping:
    url: ${SHIPPING_SERVICE_URL:http://localhost:8083}
    timeout: 45s
    retry:
      max-attempts: 3
      delay: 1s

# 注文プロセス設定
order:
  process:
    timeout: 300s # 5分
    retry:
      max-attempts: 3
      backoff-multiplier: 2
    compensation:
      enabled: true
      async: true
      max-retry: 5
  
  inventory:
    reservation-expiry-hours: 24
  
  payment:
    auto-capture: true
    capture-timeout: 60s

# サーキットブレーカー設定
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
      payment-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
      shipping-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10

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
    com.example.order: DEBUG
    feign: DEBUG
```

## エラーハンドリング

### カスタム例外
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class OrderProcessException extends RuntimeException {
    public OrderProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String service, String message, Throwable cause) {
        super("External service [" + service + "] error: " + message, cause);
    }
}
```

### グローバル例外ハンドラー
```java
@ControllerAdvice
public class OrderExceptionHandler {
    
    @ExceptionHandler(OrderProcessException.class)
    public ResponseEntity<ErrorResponse> handleOrderProcessException(OrderProcessException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("ORDER_PROCESS_ERROR")
            .message("Order processing failed")
            .details(e.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(500).body(error);
    }
    
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        String serviceName = extractServiceName(e.request().url());
        ErrorResponse error = ErrorResponse.builder()
            .code("EXTERNAL_SERVICE_ERROR")
            .message("External service communication failed")
            .details("Service: " + serviceName + ", Status: " + e.status())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(502).body(error);
    }
}
```

## テスト戦略

### 統合テスト例
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "scalar.db.contact_points=jdbc:sqlite::memory:",
    "services.inventory.url=http://localhost:${wiremock.server.port}",
    "services.payment.url=http://localhost:${wiremock.server.port}",
    "services.shipping.url=http://localhost:${wiremock.server.port}"
})
class OrderProcessIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(0))
        .build();
    
    @Test
    void processOrder_SuccessfulFlow_ReturnsCompletedOrder() {
        // Given
        stubInventoryReservation();
        stubPaymentProcessing();
        stubShippingArrangement();
        
        CreateOrderRequest request = CreateOrderRequest.builder()
            .customerId("CUST-001")
            .paymentMethodId("PM-001")
            .items(Arrays.asList(
                CreateOrderItemRequest.builder()
                    .productId("PROD-001")
                    .quantity(2)
                    .unitPrice(1500L)
                    .build()
            ))
            .shippingAddress(createTestAddress())
            .build();
        
        // When
        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", request, Order.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order order = response.getBody();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getInventoryReservationId()).isNotNull();
        assertThat(order.getPaymentId()).isNotNull();
        assertThat(order.getShipmentId()).isNotNull();
    }
    
    @Test
    void processOrder_PaymentFailure_RollsBackInventory() {
        // Given
        stubInventoryReservation();
        stubPaymentFailure();
        stubInventoryRollback();
        
        CreateOrderRequest request = createTestOrderRequest();
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/orders", request, ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // 在庫ロールバックが呼ばれたことを確認
        verify(deleteRequestedFor(urlMatching("/reservations/.*")));
    }
    
    private void stubInventoryReservation() {
        wireMock.stubFor(post(urlEqualTo("/inventory-items/reserve"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "reservation_id": "RES-001",
                        "status": "ACTIVE",
                        "expires_at": "2024-01-02T00:00:00Z"
                    }
                    """)));
    }
    
    private void stubPaymentProcessing() {
        wireMock.stubFor(post(urlEqualTo("/payments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "payment_id": "PAY-001",
                        "status": "PENDING"
                    }
                    """)));
        
        wireMock.stubFor(post(urlMatching("/payments/.*/execute"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "payment_id": "PAY-001",
                        "status": "CAPTURED"
                    }
                    """)));
    }
}
```

## 監視とメトリクス

### カスタムメトリクス
```java
@Component
public class OrderProcessMetrics {
    
    private final Counter orderCounter;
    private final Timer orderProcessingTime;
    private final Counter compensationCounter;
    private final Gauge activeOrders;
    
    public OrderProcessMetrics(MeterRegistry meterRegistry) {
        this.orderCounter = Counter.builder("orders.total")
            .description("Total number of orders processed")
            .register(meterRegistry);
            
        this.orderProcessingTime = Timer.builder("orders.processing_time")
            .description("Time taken to process orders")
            .register(meterRegistry);
            
        this.compensationCounter = Counter.builder("orders.compensations.total")
            .description("Total number of compensations executed")
            .register(meterRegistry);
            
        this.activeOrders = Gauge.builder("orders.active")
            .description("Number of active orders")
            .register(meterRegistry, this, OrderProcessMetrics::getActiveOrderCount);
    }
    
    public void recordOrderSuccess() {
        orderCounter.increment(Tags.of("status", "success"));
    }
    
    public void recordOrderFailure(String failureReason) {
        orderCounter.increment(Tags.of("status", "failure", "reason", failureReason));
    }
    
    public void recordCompensation(CompensationType type, CompensationStatus status) {
        compensationCounter.increment(Tags.of("type", type.name(), "status", status.name()));
    }
    
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    private double getActiveOrderCount() {
        return orderRepository.countActiveOrders();
    }
}
```

## 実装チェックリスト

### Process API実装
- [ ] Order, OrderItem, OrderProcessEvent, OrderCompensation エンティティ定義
- [ ] ScalarDBリポジトリ実装（Order, OrderItem, ProcessEvent, Compensation）
- [ ] OrderProcessService実装（注文プロセス実行・補償処理）
- [ ] OrderController実装（CRUD + プロセス操作）
- [ ] 外部サービスクライアント実装（Feign）
- [ ] サーキットブレーカー設定（Resilience4j）
- [ ] 分散トランザクション実装（ScalarDB）
- [ ] 補償トランザクション実装（Saga Pattern）
- [ ] エラーハンドリング・リトライ実装
- [ ] ユニットテスト実装
- [ ] 統合テスト実装（WireMock使用）

### 運用準備
- [ ] プロセス監視ダッシュボード設定
- [ ] 分散トランザクション失敗アラート設定
- [ ] 補償処理監視設定
- [ ] 外部サービス依存関係監視設定