# 在庫サービス API設計書

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Inventory Service |
| API種別 | System API |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/inventory/v1 |
| 認証方式 | Bearer Token |
| データベース | SQLite + ScalarDB |

## ビジネスドメイン

### ドメイン概要
- **責務**: 商品在庫の管理と在庫操作
- **境界**: 商品の在庫数量、在庫状態、在庫予約の管理
- **データ所有**: 商品在庫情報、在庫履歴、在庫予約情報

### ユースケース
1. 商品在庫の登録・更新・削除
2. 在庫数量の照会
3. 在庫の予約・確保
4. 在庫の解放・消費

## データモデル

### ScalarDBスキーマ定義

```json
{
  "inventory.inventory_items": {
    "transaction": true,
    "partition-key": ["product_id"],
    "columns": {
      "product_id": "TEXT",
      "product_name": "TEXT",
      "available_quantity": "INT",
      "reserved_quantity": "INT",
      "total_quantity": "INT",
      "unit_price": "BIGINT",
      "currency": "TEXT",
      "status": "TEXT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT",
      "version": "INT"
    }
  },
  "inventory.inventory_reservations": {
    "transaction": true,
    "partition-key": ["reservation_id"],
    "clustering-key": ["product_id"],
    "columns": {
      "reservation_id": "TEXT",
      "product_id": "TEXT",
      "customer_id": "TEXT",
      "reserved_quantity": "INT",
      "reservation_status": "TEXT",
      "expires_at": "BIGINT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT"
    }
  },
  "inventory.inventory_transactions": {
    "transaction": true,
    "partition-key": ["transaction_id"],
    "clustering-key": ["created_at"],
    "columns": {
      "transaction_id": "TEXT",
      "product_id": "TEXT",
      "transaction_type": "TEXT",
      "quantity_change": "INT",
      "reason": "TEXT",
      "reference_id": "TEXT",
      "created_at": "BIGINT",
      "created_by": "TEXT"
    }
  }
}
```

### エンティティ定義

#### InventoryItem
```java
@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @PartitionKey
    private String productId;
    
    @Column
    private String productName;
    
    @Column
    private Integer availableQuantity;
    
    @Column
    private Integer reservedQuantity;
    
    @Column
    private Integer totalQuantity;
    
    @Column
    private Long unitPrice;
    
    @Column
    private String currency;
    
    @Column
    private InventoryStatus status;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private Integer version;
}

public enum InventoryStatus {
    ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED
}
```

#### InventoryReservation
```java
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
    @PartitionKey
    private String reservationId;
    
    @ClusteringColumn
    private String productId;
    
    @Column
    private String customerId;
    
    @Column
    private Integer reservedQuantity;
    
    @Column
    private ReservationStatus reservationStatus;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}

public enum ReservationStatus {
    ACTIVE, EXPIRED, CONSUMED, CANCELLED
}
```

## API仕様

### System API (CRUD操作)

#### 1. 在庫アイテム作成
```http
POST /inventory-items
Content-Type: application/json
Authorization: Bearer {token}

{
  "product_id": "PROD-001",
  "product_name": "Sample Product",
  "total_quantity": 100,
  "unit_price": 1500,
  "currency": "JPY",
  "status": "ACTIVE"
}
```

**レスポンス**:
```json
{
  "product_id": "PROD-001",
  "product_name": "Sample Product",
  "available_quantity": 100,
  "reserved_quantity": 0,
  "total_quantity": 100,
  "unit_price": 1500,
  "currency": "JPY",
  "status": "ACTIVE",
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "version": 1
}
```

#### 2. 在庫アイテム取得
```http
GET /inventory-items/{product_id}
Authorization: Bearer {token}
```

#### 3. 在庫アイテム一覧取得
```http
GET /inventory-items?page=0&size=20&status=ACTIVE
Authorization: Bearer {token}
```

#### 4. 在庫アイテム更新
```http
PUT /inventory-items/{product_id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "product_name": "Updated Product",
  "total_quantity": 150,
  "unit_price": 1800,
  "status": "ACTIVE",
  "version": 1
}
```

#### 5. 在庫アイテム削除
```http
DELETE /inventory-items/{product_id}
Authorization: Bearer {token}
```

#### 6. 在庫予約作成
```http
POST /inventory-items/{product_id}/reservations
Content-Type: application/json
Authorization: Bearer {token}

{
  "customer_id": "CUST-001",
  "reserved_quantity": 5,
  "expires_at": "2024-01-02T00:00:00Z"
}
```

#### 7. 在庫予約取得
```http
GET /reservations/{reservation_id}
Authorization: Bearer {token}
```

#### 8. 在庫予約キャンセル
```http
DELETE /reservations/{reservation_id}
Authorization: Bearer {token}
```

#### 9. 在庫履歴取得
```http
GET /inventory-items/{product_id}/transactions?page=0&size=50
Authorization: Bearer {token}
```

## ScalarDBトランザクション設計

### 在庫予約処理
```java
@Service
@Transactional
public class InventoryService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    public InventoryReservation reserveInventory(String productId, ReserveInventoryRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 在庫アイテム取得
            Optional<InventoryItem> itemOpt = inventoryRepository.findById(transaction, productId);
            if (itemOpt.isEmpty()) {
                throw new InventoryNotFoundException("Product not found: " + productId);
            }
            
            InventoryItem item = itemOpt.get();
            
            // 2. 在庫数量チェック
            if (item.getAvailableQuantity() < request.getReservedQuantity()) {
                throw new InsufficientInventoryException("Insufficient inventory");
            }
            
            // 3. 在庫予約作成
            InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(UUID.randomUUID().toString())
                .productId(productId)
                .customerId(request.getCustomerId())
                .reservedQuantity(request.getReservedQuantity())
                .reservationStatus(ReservationStatus.ACTIVE)
                .expiresAt(request.getExpiresAt())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            reservationRepository.save(transaction, reservation);
            
            // 4. 在庫数量更新
            item.setAvailableQuantity(item.getAvailableQuantity() - request.getReservedQuantity());
            item.setReservedQuantity(item.getReservedQuantity() + request.getReservedQuantity());
            item.setUpdatedAt(LocalDateTime.now());
            item.setVersion(item.getVersion() + 1);
            
            inventoryRepository.save(transaction, item);
            
            // 5. 在庫履歴記録
            InventoryTransaction historyTransaction = InventoryTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .productId(productId)
                .transactionType("RESERVE")
                .quantityChange(-request.getReservedQuantity())
                .reason("Inventory reservation")
                .referenceId(reservation.getReservationId())
                .createdAt(LocalDateTime.now())
                .createdBy(request.getCustomerId())
                .build();
            
            transactionRepository.save(transaction, historyTransaction);
            
            transaction.commit();
            return reservation;
        } catch (Exception e) {
            transaction.abort();
            throw new InventoryServiceException("Failed to reserve inventory", e);
        }
    }
    
    public void consumeReservation(String reservationId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 予約情報取得
            Optional<InventoryReservation> reservationOpt = 
                reservationRepository.findById(transaction, reservationId);
            if (reservationOpt.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found: " + reservationId);
            }
            
            InventoryReservation reservation = reservationOpt.get();
            if (reservation.getReservationStatus() != ReservationStatus.ACTIVE) {
                throw new InvalidReservationStatusException("Reservation is not active");
            }
            
            // 2. 在庫アイテム取得・更新
            Optional<InventoryItem> itemOpt = 
                inventoryRepository.findById(transaction, reservation.getProductId());
            InventoryItem item = itemOpt.orElseThrow();
            
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQuantity());
            item.setTotalQuantity(item.getTotalQuantity() - reservation.getReservedQuantity());
            item.setUpdatedAt(LocalDateTime.now());
            item.setVersion(item.getVersion() + 1);
            
            inventoryRepository.save(transaction, item);
            
            // 3. 予約状態更新
            reservation.setReservationStatus(ReservationStatus.CONSUMED);
            reservation.setUpdatedAt(LocalDateTime.now());
            
            reservationRepository.save(transaction, reservation);
            
            // 4. 在庫履歴記録
            InventoryTransaction historyTransaction = InventoryTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .productId(reservation.getProductId())
                .transactionType("CONSUME")
                .quantityChange(-reservation.getReservedQuantity())
                .reason("Inventory consumption")
                .referenceId(reservationId)
                .createdAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();
            
            transactionRepository.save(transaction, historyTransaction);
            
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw new InventoryServiceException("Failed to consume reservation", e);
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
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/inventory.db
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
    name: inventory-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

server:
  port: ${SERVER_PORT:8081}

scalardb:
  properties: classpath:scalardb.properties

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

# 在庫サービス固有設定
inventory:
  reservation:
    default-expiry-hours: 24
    cleanup-interval-minutes: 60
  low-stock:
    threshold: 10
    notification-enabled: true

logging:
  level:
    com.scalar.db: DEBUG
    com.example.inventory: DEBUG
```

## エラーハンドリング

### カスタム例外
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidReservationStatusException extends RuntimeException {
    public InvalidReservationStatusException(String message) {
        super(message);
    }
}
```

## テスト戦略

### ユニットテスト例
```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @InjectMocks
    private InventoryService inventoryService;
    
    @Test
    void reserveInventory_Success() {
        // Given
        DistributedTransaction transaction = mock(DistributedTransaction.class);
        when(transactionManager.start()).thenReturn(transaction);
        
        InventoryItem item = InventoryItem.builder()
            .productId("PROD-001")
            .availableQuantity(100)
            .reservedQuantity(0)
            .totalQuantity(100)
            .build();
        
        when(inventoryRepository.findById(transaction, "PROD-001"))
            .thenReturn(Optional.of(item));
        
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
            .customerId("CUST-001")
            .reservedQuantity(5)
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
        
        // When
        InventoryReservation result = inventoryService.reserveInventory("PROD-001", request);
        
        // Then
        assertThat(result.getReservedQuantity()).isEqualTo(5);
        assertThat(result.getReservationStatus()).isEqualTo(ReservationStatus.ACTIVE);
        verify(transaction).commit();
    }
    
    @Test
    void reserveInventory_InsufficientInventory_ThrowsException() {
        // Given
        DistributedTransaction transaction = mock(DistributedTransaction.class);
        when(transactionManager.start()).thenReturn(transaction);
        
        InventoryItem item = InventoryItem.builder()
            .productId("PROD-001")
            .availableQuantity(3)
            .reservedQuantity(0)
            .totalQuantity(3)
            .build();
        
        when(inventoryRepository.findById(transaction, "PROD-001"))
            .thenReturn(Optional.of(item));
        
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
            .customerId("CUST-001")
            .reservedQuantity(5)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory("PROD-001", request))
            .isInstanceOf(InsufficientInventoryException.class);
        
        verify(transaction).abort();
    }
}
```

## 監視とメトリクス

### カスタムメトリクス
```java
@Component
public class InventoryMetrics {
    
    private final Counter reservationCounter;
    private final Gauge lowStockGauge;
    private final Timer reservationProcessingTime;
    
    public InventoryMetrics(MeterRegistry meterRegistry) {
        this.reservationCounter = Counter.builder("inventory.reservations.total")
            .description("Total number of inventory reservations")
            .register(meterRegistry);
            
        this.lowStockGauge = Gauge.builder("inventory.low_stock_items")
            .description("Number of items with low stock")
            .register(meterRegistry, this, InventoryMetrics::getLowStockItemCount);
            
        this.reservationProcessingTime = Timer.builder("inventory.reservation.processing_time")
            .description("Time taken to process inventory reservations")
            .register(meterRegistry);
    }
    
    public void recordReservationSuccess() {
        reservationCounter.increment(Tags.of("status", "success"));
    }
    
    public void recordReservationFailure(String reason) {
        reservationCounter.increment(Tags.of("status", "failure", "reason", reason));
    }
    
    private double getLowStockItemCount() {
        // 低在庫アイテム数を取得するロジック
        return inventoryRepository.countLowStockItems();
    }
}
```

## 実装チェックリスト

### System API実装
- [ ] InventoryItem, InventoryReservation, InventoryTransaction エンティティ定義
- [ ] ScalarDBリポジトリ実装（Inventory, Reservation, Transaction）
- [ ] InventoryService実装（予約・消費・解放・在庫更新）
- [ ] InventoryController実装（CRUD + 予約管理）
- [ ] バリデーション実装（在庫数量、予約期限など）
- [ ] カスタム例外ハンドリング実装
- [ ] ユニットテスト実装
- [ ] 統合テスト実装

### 運用準備
- [ ] 在庫低下アラート設定
- [ ] 期限切れ予約の自動クリーンアップ
- [ ] 在庫メトリクス収集
- [ ] 在庫履歴の保持期間設定