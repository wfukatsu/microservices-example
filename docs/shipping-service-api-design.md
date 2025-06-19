# 配送サービス API設計書

## 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | Shipping Service |
| API種別 | System API |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/shipping/v1 |
| 認証方式 | Bearer Token |
| データベース | SQLite + ScalarDB |

## ビジネスドメイン

### ドメイン概要
- **責務**: 配送手配と配送状況の管理
- **境界**: 配送先情報、配送業者連携、配送状況追跡の管理
- **データ所有**: 配送情報、配送履歴、配送業者情報

### ユースケース
1. 配送の作成・更新・キャンセル
2. 配送状況の照会・追跡
3. 配送業者との連携
4. 配送履歴の管理

## データモデル

### ScalarDBスキーマ定義

```json
{
  "shipping.shipments": {
    "transaction": true,
    "partition-key": ["shipment_id"],
    "columns": {
      "shipment_id": "TEXT",
      "order_id": "TEXT",
      "customer_id": "TEXT",
      "shipping_method": "TEXT",
      "carrier": "TEXT",
      "tracking_number": "TEXT",
      "shipping_status": "TEXT",
      "recipient_name": "TEXT",
      "recipient_phone": "TEXT",
      "shipping_address": "TEXT",
      "shipping_city": "TEXT",
      "shipping_state": "TEXT",
      "shipping_postal_code": "TEXT",
      "shipping_country": "TEXT",
      "estimated_delivery_date": "BIGINT",
      "actual_delivery_date": "BIGINT",
      "shipping_cost": "BIGINT",
      "currency": "TEXT",
      "weight": "DOUBLE",
      "dimensions": "TEXT",
      "special_instructions": "TEXT",
      "created_at": "BIGINT",
      "updated_at": "BIGINT",
      "version": "INT"
    }
  },
  "shipping.shipping_items": {
    "transaction": true,
    "partition-key": ["shipment_id"],
    "clustering-key": ["item_id"],
    "columns": {
      "shipment_id": "TEXT",
      "item_id": "TEXT",
      "product_id": "TEXT",
      "product_name": "TEXT",
      "quantity": "INT",
      "weight": "DOUBLE",
      "dimensions": "TEXT",
      "is_fragile": "BOOLEAN",
      "is_hazardous": "BOOLEAN"
    }
  },
  "shipping.shipping_events": {
    "transaction": true,
    "partition-key": ["event_id"],
    "clustering-key": ["shipment_id", "event_time"],
    "columns": {
      "event_id": "TEXT",
      "shipment_id": "TEXT",
      "event_type": "TEXT",
      "event_status": "TEXT",
      "event_description": "TEXT",
      "event_location": "TEXT",
      "event_time": "BIGINT",
      "carrier_event_id": "TEXT",
      "created_at": "BIGINT"
    }
  },
  "shipping.carriers": {
    "transaction": true,
    "partition-key": ["carrier_id"],
    "columns": {
      "carrier_id": "TEXT",
      "carrier_name": "TEXT",
      "carrier_code": "TEXT",
      "api_endpoint": "TEXT",
      "api_key": "TEXT",
      "supported_countries": "TEXT",
      "service_types": "TEXT",
      "tracking_url_template": "TEXT",
      "is_active": "BOOLEAN",
      "created_at": "BIGINT",
      "updated_at": "BIGINT"
    }
  },
  "shipping.shipping_rates": {
    "transaction": true,
    "partition-key": ["rate_id"],
    "clustering-key": ["carrier_id", "service_type"],
    "columns": {
      "rate_id": "TEXT",
      "carrier_id": "TEXT",
      "service_type": "TEXT",
      "origin_country": "TEXT",
      "destination_country": "TEXT",
      "weight_from": "DOUBLE",
      "weight_to": "DOUBLE",
      "base_rate": "BIGINT",
      "currency": "TEXT",
      "delivery_days_min": "INT",
      "delivery_days_max": "INT",
      "is_active": "BOOLEAN",
      "created_at": "BIGINT",
      "updated_at": "BIGINT"
    }
  }
}
```

### エンティティ定義

#### Shipment
```java
@Entity
@Table(name = "shipments")
public class Shipment {
    @PartitionKey
    private String shipmentId;
    
    @Column
    private String orderId;
    
    @Column
    private String customerId;
    
    @Column
    private String shippingMethod;
    
    @Column
    private String carrier;
    
    @Column
    private String trackingNumber;
    
    @Column
    private ShippingStatus shippingStatus;
    
    @Column
    private String recipientName;
    
    @Column
    private String recipientPhone;
    
    @Column
    private String shippingAddress;
    
    @Column
    private String shippingCity;
    
    @Column
    private String shippingState;
    
    @Column
    private String shippingPostalCode;
    
    @Column
    private String shippingCountry;
    
    @Column
    private LocalDateTime estimatedDeliveryDate;
    
    @Column
    private LocalDateTime actualDeliveryDate;
    
    @Column
    private Long shippingCost;
    
    @Column
    private String currency;
    
    @Column
    private Double weight;
    
    @Column
    private String dimensions;
    
    @Column
    private String specialInstructions;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private Integer version;
}

public enum ShippingStatus {
    PENDING, PROCESSING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, 
    EXCEPTION, CANCELLED, RETURNED, LOST
}
```

#### ShippingItem
```java
@Entity
@Table(name = "shipping_items")
public class ShippingItem {
    @PartitionKey
    private String shipmentId;
    
    @ClusteringColumn
    private String itemId;
    
    @Column
    private String productId;
    
    @Column
    private String productName;
    
    @Column
    private Integer quantity;
    
    @Column
    private Double weight;
    
    @Column
    private String dimensions;
    
    @Column
    private Boolean isFragile;
    
    @Column
    private Boolean isHazardous;
}
```

#### ShippingEvent
```java
@Entity
@Table(name = "shipping_events")
public class ShippingEvent {
    @PartitionKey
    private String eventId;
    
    @ClusteringColumn(0)
    private String shipmentId;
    
    @ClusteringColumn(1)
    private LocalDateTime eventTime;
    
    @Column
    private String eventType;
    
    @Column
    private String eventStatus;
    
    @Column
    private String eventDescription;
    
    @Column
    private String eventLocation;
    
    @Column
    private String carrierEventId;
    
    @Column
    private LocalDateTime createdAt;
}
```

#### Carrier
```java
@Entity
@Table(name = "carriers")
public class Carrier {
    @PartitionKey
    private String carrierId;
    
    @Column
    private String carrierName;
    
    @Column
    private String carrierCode;
    
    @Column
    private String apiEndpoint;
    
    @Column
    private String apiKey;
    
    @Column
    private String supportedCountries;
    
    @Column
    private String serviceTypes;
    
    @Column
    private String trackingUrlTemplate;
    
    @Column
    private Boolean isActive;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}
```

## API仕様

### System API (CRUD操作)

#### 1. 配送作成
```http
POST /shipments
Content-Type: application/json
Authorization: Bearer {token}

{
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "shipping_method": "STANDARD",
  "carrier": "YAMATO",
  "recipient_name": "田中太郎",
  "recipient_phone": "090-1234-5678",
  "shipping_address": "東京都渋谷区渋谷1-1-1",
  "shipping_city": "渋谷区",
  "shipping_state": "東京都",
  "shipping_postal_code": "150-0002",
  "shipping_country": "JP",
  "weight": 1.5,
  "dimensions": "30x20x10",
  "special_instructions": "午前中配達希望",
  "items": [
    {
      "product_id": "PROD-001",
      "product_name": "Sample Product",
      "quantity": 2,
      "weight": 0.5,
      "dimensions": "15x10x5",
      "is_fragile": false,
      "is_hazardous": false
    }
  ]
}
```

**レスポンス**:
```json
{
  "shipment_id": "SHIP-001",
  "order_id": "ORDER-001",
  "customer_id": "CUST-001",
  "shipping_method": "STANDARD",
  "carrier": "YAMATO",
  "tracking_number": "1234567890123",
  "shipping_status": "PENDING",
  "recipient_name": "田中太郎",
  "recipient_phone": "090-1234-5678",
  "shipping_address": "東京都渋谷区渋谷1-1-1",
  "shipping_city": "渋谷区",
  "shipping_state": "東京都",
  "shipping_postal_code": "150-0002",
  "shipping_country": "JP",
  "estimated_delivery_date": "2024-01-03T00:00:00Z",
  "shipping_cost": 800,
  "currency": "JPY",
  "weight": 1.5,
  "dimensions": "30x20x10",
  "special_instructions": "午前中配達希望",
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "version": 1
}
```

#### 2. 配送取得
```http
GET /shipments/{shipment_id}
Authorization: Bearer {token}
```

#### 3. 配送一覧取得
```http
GET /shipments?customer_id=CUST-001&status=IN_TRANSIT&page=0&size=20
Authorization: Bearer {token}
```

#### 4. 配送状況更新
```http
PUT /shipments/{shipment_id}/status
Content-Type: application/json
Authorization: Bearer {token}

{
  "shipping_status": "SHIPPED",
  "tracking_number": "1234567890123",
  "estimated_delivery_date": "2024-01-03T00:00:00Z"
}
```

#### 5. 配送キャンセル
```http
POST /shipments/{shipment_id}/cancel
Content-Type: application/json
Authorization: Bearer {token}

{
  "cancellation_reason": "Customer requested cancellation"
}
```

#### 6. 配送履歴取得
```http
GET /shipments/{shipment_id}/events
Authorization: Bearer {token}
```

#### 7. 配送料金計算
```http
POST /shipping-rates/calculate
Content-Type: application/json
Authorization: Bearer {token}

{
  "origin_country": "JP",
  "destination_country": "JP",
  "weight": 1.5,
  "dimensions": "30x20x10",
  "shipping_method": "STANDARD",
  "carrier": "YAMATO"
}
```

#### 8. 配送業者一覧取得
```http
GET /carriers?country=JP&is_active=true
Authorization: Bearer {token}
```

#### 9. 配送追跡
```http
GET /shipments/{shipment_id}/tracking
Authorization: Bearer {token}
```

## ScalarDBトランザクション設計

### 配送作成処理
```java
@Service
@Transactional
public class ShippingService {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private CarrierIntegrationService carrierIntegrationService;
    
    public Shipment createShipment(CreateShipmentRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 配送料金計算
            ShippingRate rate = calculateShippingRate(request);
            
            // 2. 配送情報作成
            Shipment shipment = Shipment.builder()
                .shipmentId(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .shippingMethod(request.getShippingMethod())
                .carrier(request.getCarrier())
                .shippingStatus(ShippingStatus.PENDING)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingPostalCode(request.getShippingPostalCode())
                .shippingCountry(request.getShippingCountry())
                .shippingCost(rate.getBaseRate())
                .currency(rate.getCurrency())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .specialInstructions(request.getSpecialInstructions())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .build();
            
            shipmentRepository.save(transaction, shipment);
            
            // 3. 配送アイテム作成
            for (CreateShippingItemRequest itemRequest : request.getItems()) {
                ShippingItem item = ShippingItem.builder()
                    .shipmentId(shipment.getShipmentId())
                    .itemId(UUID.randomUUID().toString())
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .weight(itemRequest.getWeight())
                    .dimensions(itemRequest.getDimensions())
                    .isFragile(itemRequest.getIsFragile())
                    .isHazardous(itemRequest.getIsHazardous())
                    .build();
                
                shippingItemRepository.save(transaction, item);
            }
            
            // 4. 配送業者へのラベル作成要求
            CarrierShipmentRequest carrierRequest = buildCarrierShipmentRequest(shipment, request.getItems());
            CarrierShipmentResponse carrierResponse = carrierIntegrationService.createShipment(carrierRequest);
            
            // 5. 追跡番号と配送業者情報更新
            shipment.setTrackingNumber(carrierResponse.getTrackingNumber());
            shipment.setEstimatedDeliveryDate(carrierResponse.getEstimatedDeliveryDate());
            shipment.setShippingStatus(ShippingStatus.PROCESSING);
            shipment.setUpdatedAt(LocalDateTime.now());
            shipment.setVersion(shipment.getVersion() + 1);
            
            shipmentRepository.save(transaction, shipment);
            
            // 6. 配送イベント記録
            ShippingEvent event = ShippingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .shipmentId(shipment.getShipmentId())
                .eventType("SHIPMENT_CREATED")
                .eventStatus("PROCESSING")
                .eventDescription("Shipment created and sent to carrier")
                .eventTime(LocalDateTime.now())
                .carrierEventId(carrierResponse.getCarrierEventId())
                .createdAt(LocalDateTime.now())
                .build();
            
            shippingEventRepository.save(transaction, event);
            
            transaction.commit();
            return shipment;
        } catch (Exception e) {
            transaction.abort();
            throw new ShippingServiceException("Failed to create shipment", e);
        }
    }
    
    public void updateShippingStatus(String shipmentId, UpdateShippingStatusRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 1. 配送情報取得
            Optional<Shipment> shipmentOpt = shipmentRepository.findById(transaction, shipmentId);
            if (shipmentOpt.isEmpty()) {
                throw new ShipmentNotFoundException("Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            ShippingStatus previousStatus = shipment.getShippingStatus();
            
            // 2. 状態遷移チェック
            if (!isValidStatusTransition(previousStatus, request.getShippingStatus())) {
                throw new InvalidShippingStatusTransitionException(
                    "Invalid status transition from " + previousStatus + " to " + request.getShippingStatus());
            }
            
            // 3. 配送情報更新
            shipment.setShippingStatus(request.getShippingStatus());
            if (request.getTrackingNumber() != null) {
                shipment.setTrackingNumber(request.getTrackingNumber());
            }
            if (request.getEstimatedDeliveryDate() != null) {
                shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
            }
            if (request.getShippingStatus() == ShippingStatus.DELIVERED && request.getActualDeliveryDate() != null) {
                shipment.setActualDeliveryDate(request.getActualDeliveryDate());
            }
            
            shipment.setUpdatedAt(LocalDateTime.now());
            shipment.setVersion(shipment.getVersion() + 1);
            
            shipmentRepository.save(transaction, shipment);
            
            // 4. 配送イベント記録
            ShippingEvent event = ShippingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .shipmentId(shipmentId)
                .eventType("STATUS_UPDATED")
                .eventStatus(request.getShippingStatus().name())
                .eventDescription("Shipping status updated to " + request.getShippingStatus())
                .eventLocation(request.getEventLocation())
                .eventTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
            
            shippingEventRepository.save(transaction, event);
            
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw new ShippingServiceException("Failed to update shipping status", e);
        }
    }
    
    @Scheduled(fixedRate = 300000) // 5分ごと
    public void syncTrackingInfo() {
        List<Shipment> activeShipments = shipmentRepository.findActiveShipments();
        
        for (Shipment shipment : activeShipments) {
            try {
                TrackingInfo trackingInfo = carrierIntegrationService.getTrackingInfo(
                    shipment.getCarrier(), shipment.getTrackingNumber());
                
                if (trackingInfo.hasUpdates()) {
                    updateShippingStatusFromTracking(shipment, trackingInfo);
                }
            } catch (Exception e) {
                log.error("Failed to sync tracking info for shipment: " + shipment.getShipmentId(), e);
            }
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
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/shipping.db
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
    name: shipping-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  task:
    scheduling:
      enabled: true

server:
  port: ${SERVER_PORT:8083}

scalardb:
  properties: classpath:scalardb.properties

# 配送業者設定
shipping:
  carriers:
    yamato:
      api-url: ${YAMATO_API_URL}
      api-key: ${YAMATO_API_KEY}
      tracking-url: "https://toi.kuronekoyamato.co.jp/cgi-bin/tneko"
    sagawa:
      api-url: ${SAGAWA_API_URL}
      api-key: ${SAGAWA_API_KEY}
      tracking-url: "https://k2k.sagawa-exp.co.jp/p/sagawa/web/okurijoinput.jsp"
    jpost:
      api-url: ${JPOST_API_URL}
      api-key: ${JPOST_API_KEY}
      tracking-url: "https://trackings.post.japanpost.jp/services/srv/search/"
  
  tracking:
    sync-interval: 300000 # 5分
    max-retries: 3
    retry-delay: 60000 # 1分
  
  default-currency: JPY
  weight-unit: kg
  dimension-unit: cm

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
    com.example.shipping: DEBUG
```

## エラーハンドリング

### カスタム例外
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidShippingStatusTransitionException extends RuntimeException {
    public InvalidShippingStatusTransitionException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidShippingAddressException extends RuntimeException {
    public InvalidShippingAddressException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CarrierIntegrationException extends RuntimeException {
    public CarrierIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 外部連携

### 配送業者インターフェース
```java
public interface CarrierIntegrationService {
    CarrierShipmentResponse createShipment(CarrierShipmentRequest request);
    TrackingInfo getTrackingInfo(String carrier, String trackingNumber);
    void cancelShipment(String carrier, String trackingNumber);
    List<ShippingRate> getShippingRates(ShippingRateRequest request);
}

@Service
public class YamatoCarrierService implements CarrierIntegrationService {
    
    @Autowired
    private YamatoApiClient yamatoClient;
    
    @Override
    public CarrierShipmentResponse createShipment(CarrierShipmentRequest request) {
        try {
            YamatoShipmentRequest yamatoRequest = YamatoShipmentRequest.builder()
                .recipientName(request.getRecipientName())
                .recipientAddress(request.getShippingAddress())
                .recipientPhone(request.getRecipientPhone())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .serviceType(mapServiceType(request.getShippingMethod()))
                .build();
            
            YamatoShipmentResponse yamatoResponse = yamatoClient.createShipment(yamatoRequest);
            
            return CarrierShipmentResponse.builder()
                .trackingNumber(yamatoResponse.getTrackingNumber())
                .estimatedDeliveryDate(yamatoResponse.getEstimatedDeliveryDate())
                .carrierEventId(yamatoResponse.getShipmentId())
                .build();
        } catch (YamatoApiException e) {
            throw new CarrierIntegrationException("Yamato shipment creation failed", e);
        }
    }
    
    @Override
    public TrackingInfo getTrackingInfo(String carrier, String trackingNumber) {
        try {
            YamatoTrackingResponse response = yamatoClient.getTrackingInfo(trackingNumber);
            
            return TrackingInfo.builder()
                .trackingNumber(trackingNumber)
                .status(mapYamatoStatusToShippingStatus(response.getStatus()))
                .events(response.getEvents().stream()
                    .map(this::mapYamatoEventToShippingEvent)
                    .collect(Collectors.toList()))
                .lastUpdated(response.getLastUpdated())
                .build();
        } catch (YamatoApiException e) {
            throw new CarrierIntegrationException("Yamato tracking info retrieval failed", e);
        }
    }
}
```

## テスト戦略

### ユニットテスト例
```java
@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private ShipmentRepository shipmentRepository;
    
    @Mock
    private CarrierIntegrationService carrierIntegrationService;
    
    @InjectMocks
    private ShippingService shippingService;
    
    @Test
    void createShipment_Success() {
        // Given
        DistributedTransaction transaction = mock(DistributedTransaction.class);
        when(transactionManager.start()).thenReturn(transaction);
        
        CreateShipmentRequest request = CreateShipmentRequest.builder()
            .orderId("ORDER-001")
            .customerId("CUST-001")
            .carrier("YAMATO")
            .shippingMethod("STANDARD")
            .recipientName("田中太郎")
            .weight(1.5)
            .items(Arrays.asList(
                CreateShippingItemRequest.builder()
                    .productId("PROD-001")
                    .quantity(1)
                    .weight(1.5)
                    .build()
            ))
            .build();
        
        CarrierShipmentResponse carrierResponse = CarrierShipmentResponse.builder()
            .trackingNumber("1234567890123")
            .estimatedDeliveryDate(LocalDateTime.now().plusDays(2))
            .build();
        
        when(carrierIntegrationService.createShipment(any()))
            .thenReturn(carrierResponse);
        
        // When
        Shipment result = shippingService.createShipment(request);
        
        // Then
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getTrackingNumber()).isEqualTo("1234567890123");
        assertThat(result.getShippingStatus()).isEqualTo(ShippingStatus.PROCESSING);
        verify(transaction).commit();
    }
}
```

## 監視とメトリクス

### カスタムメトリクス
```java
@Component
public class ShippingMetrics {
    
    private final Counter shipmentCounter;
    private final Timer deliveryTime;
    private final Gauge delayedShipments;
    
    public ShippingMetrics(MeterRegistry meterRegistry) {
        this.shipmentCounter = Counter.builder("shipments.total")
            .description("Total number of shipments")
            .register(meterRegistry);
            
        this.deliveryTime = Timer.builder("shipments.delivery_time")
            .description("Time from shipment to delivery")
            .register(meterRegistry);
            
        this.delayedShipments = Gauge.builder("shipments.delayed")
            .description("Number of delayed shipments")
            .register(meterRegistry, this, ShippingMetrics::getDelayedShipmentCount);
    }
    
    public void recordShipmentCreated(String carrier) {
        shipmentCounter.increment(Tags.of("status", "created", "carrier", carrier));
    }
    
    public void recordShipmentDelivered(String carrier, Duration deliveryDuration) {
        shipmentCounter.increment(Tags.of("status", "delivered", "carrier", carrier));
        deliveryTime.record(deliveryDuration);
    }
    
    private double getDelayedShipmentCount() {
        return shipmentRepository.countDelayedShipments();
    }
}
```

## 実装チェックリスト

### System API実装
- [ ] Shipment, ShippingItem, ShippingEvent, Carrier, ShippingRate エンティティ定義
- [ ] ScalarDBリポジトリ実装（Shipment, ShippingItem, Event, Carrier, Rate）
- [ ] ShippingService実装（配送作成・状況更新・キャンセル処理）
- [ ] ShippingController実装（CRUD + 配送操作）
- [ ] 配送業者連携実装（Yamato, Sagawa, Japan Post等）
- [ ] 配送料金計算ロジック実装
- [ ] 配送追跡同期処理実装（スケジューラー）
- [ ] バリデーション実装（住所、重量、サイズ等）
- [ ] カスタム例外ハンドリング実装
- [ ] ユニットテスト実装
- [ ] 統合テスト実装

### 外部連携実装
- [ ] 配送業者API連携（各キャリア）
- [ ] 配送状況同期バッチ処理
- [ ] 住所検証サービス連携
- [ ] 配送料金計算サービス連携

### 運用準備
- [ ] 配送遅延アラート設定
- [ ] 配送メトリクス収集
- [ ] 配送業者API障害対応
- [ ] 配送データの保持期間設定