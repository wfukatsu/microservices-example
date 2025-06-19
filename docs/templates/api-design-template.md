# マイクロサービスAPI設計書テンプレート

## 概要

このテンプレートは、ScalarDBを使用したマイクロサービスアーキテクチャにおけるAPI設計のためのドキュメントです。System API（CRUD操作）とProcess API（複数サービス連携）の設計パターンを定義します。

## アーキテクチャパターン

### System API
- **目的**: 単一ドメインの基本的なCRUD操作を提供
- **スコープ**: 1つのビジネスドメイン内でのデータ操作
- **トランザクション**: 単一サービス内での単純なトランザクション
- **依存関係**: 外部サービスに依存しない自己完結型

### Process API
- **目的**: 複数のSystem APIを協調させてビジネスプロセスを実現
- **スコープ**: 複数ドメインにまたがる複雑なビジネスロジック
- **トランザクション**: ScalarDBによる分散トランザクション
- **依存関係**: 複数のSystem APIを呼び出し

---

## [サービス名] API設計書

### 基本情報

| 項目 | 内容 |
|------|------|
| サービス名 | [サービス名] |
| API種別 | System API / Process API |
| バージョン | v1.0.0 |
| ベースURL | https://api.example.com/[service-name]/v1 |
| 認証方式 | Bearer Token / API Key |
| データベース | SQLite + ScalarDB |

### ビジネスドメイン

#### ドメイン概要
- **責務**: [このサービスが担当するビジネス領域]
- **境界**: [他サービスとの境界定義]
- **データ所有**: [管理するデータの種類]

#### ユースケース
1. [主要なユースケース1]
2. [主要なユースケース2]
3. [主要なユースケース3]

---

## データモデル

### ScalarDBスキーマ定義

```json
{
  "[namespace].[table_name]": {
    "transaction": true,
    "partition-key": ["[primary_key]"],
    "clustering-key": ["[optional_clustering_key]"],
    "columns": {
      "[column1]": "TEXT",
      "[column2]": "BIGINT", 
      "[column3]": "BOOLEAN",
      "created_at": "BIGINT",
      "updated_at": "BIGINT",
      "version": "INT"
    }
  }
}
```

### エンティティ定義

#### [Entity名]
```java
@Entity
@Table(name = "[table_name]")
public class [EntityName] {
    @PartitionKey
    private String [primaryKey];
    
    @Column
    private String [field1];
    
    @Column  
    private Long [field2];
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private Integer version;
}
```

---

## API仕様

### System API (CRUD操作)

#### 1. [リソース]作成
```http
POST /[resources]
Content-Type: application/json
Authorization: Bearer {token}

{
  "[field1]": "string",
  "[field2]": 123,
  "[field3]": true
}
```

**レスポンス**:
```json
{
  "id": "string",
  "[field1]": "string", 
  "[field2]": 123,
  "[field3]": true,
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "version": 1
}
```

#### 2. [リソース]取得
```http
GET /[resources]/{id}
Authorization: Bearer {token}
```

#### 3. [リソース]一覧取得
```http
GET /[resources]?page=0&size=20&sort=created_at,desc
Authorization: Bearer {token}
```

#### 4. [リソース]更新
```http
PUT /[resources]/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "[field1]": "updated_string",
  "[field2]": 456,
  "version": 1
}
```

#### 5. [リソース]削除
```http
DELETE /[resources]/{id}
Authorization: Bearer {token}
```

### Process API (複合操作)

#### [ビジネスプロセス名]
```http
POST /processes/[process-name]
Content-Type: application/json
Authorization: Bearer {token}

{
  "[input1]": "string",
  "[input2]": 123,
  "[related_resources]": [
    {
      "resource_id": "string",
      "quantity": 10
    }
  ]
}
```

**処理フロー**:
1. 入力データの検証
2. [System API A]への呼び出し（データ作成/更新）
3. [System API B]への呼び出し（関連データ処理）
4. [System API C]への呼び出し（状態更新）
5. すべて成功時にコミット、失敗時にロールバック

---

## ScalarDBトランザクション設計

### トランザクション境界

#### System API
```java
@Service
@Transactional
public class [Service]Service {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    public [Entity] create([CreateRequest] request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // 単一テーブルへの操作
            [Entity] entity = buildEntity(request);
            repository.save(transaction, entity);
            transaction.commit();
            return entity;
        } catch (Exception e) {
            transaction.abort();
            throw new ServiceException("Creation failed", e);
        }
    }
}
```

#### Process API
```java
@Service
public class [Process]Service {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private [SystemA]Service systemAService;
    
    @Autowired 
    private [SystemB]Service systemBService;
    
    public [ProcessResult] execute[Process]([ProcessRequest] request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Step 1: System A操作
            [ResultA] resultA = systemAService.processWithTransaction(transaction, request.getDataA());
            
            // Step 2: System B操作
            [ResultB] resultB = systemBService.processWithTransaction(transaction, request.getDataB());
            
            // Step 3: Process固有の処理
            [ProcessResult] result = buildProcessResult(resultA, resultB);
            
            transaction.commit();
            return result;
        } catch (Exception e) {
            transaction.abort();
            throw new ProcessException("Process execution failed", e);
        }
    }
}
```

### エラーハンドリング

#### ScalarDB例外処理
```java
@ControllerAdvice
public class ScalarDBExceptionHandler {
    
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("TRANSACTION_ERROR")
            .message("Transaction processing failed")
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(500).body(error);
    }
    
    @ExceptionHandler(CrudException.class)
    public ResponseEntity<ErrorResponse> handleCrudException(CrudException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("DATA_ACCESS_ERROR")
            .message("Data access operation failed")
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(500).body(error);
    }
}
```

---

## 設定とデプロイ

### ScalarDB設定

#### scalardb.properties
```properties
# SQLite設定
scalar.db.storage=jdbc
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/[service-name].db
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
    name: [service-name]
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

server:
  port: ${SERVER_PORT:8080}

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

logging:
  level:
    com.scalar.db: DEBUG
    com.[company].[service]: DEBUG
```

### Docker設定

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/[service-name]-*.jar app.jar
COPY src/main/resources/scalardb.properties scalardb.properties

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## テスト戦略

### ユニットテスト

#### Service層テスト
```java
@ExtendWith(MockitoExtension.class)
class [Service]ServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private [Entity]Repository repository;
    
    @InjectMocks
    private [Service]Service service;
    
    @Test
    void create_Success() {
        // Given
        DistributedTransaction transaction = mock(DistributedTransaction.class);
        when(transactionManager.start()).thenReturn(transaction);
        
        [CreateRequest] request = [CreateRequest].builder()
            .[field1]("test")
            .[field2](123)
            .build();
        
        // When
        [Entity] result = service.create(request);
        
        // Then
        assertThat(result.[getField1]()).isEqualTo("test");
        verify(transaction).commit();
    }
}
```

### 統合テスト

#### API統合テスト
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "scalar.db.contact_points=jdbc:sqlite::memory:"
})
class [Service]ApiIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void createAndRetrieve_Success() {
        // Given
        [CreateRequest] request = [CreateRequest].builder()
            .[field1]("integration-test")
            .[field2](999)
            .build();
        
        // When - Create
        ResponseEntity<[Entity]> createResponse = restTemplate.postForEntity(
            "/[resources]", request, [Entity].class);
        
        // Then - Create
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String createdId = createResponse.getBody().getId();
        
        // When - Retrieve
        ResponseEntity<[Entity]> getResponse = restTemplate.getForEntity(
            "/[resources]/" + createdId, [Entity].class);
        
        // Then - Retrieve
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().[getField1]()).isEqualTo("integration-test");
    }
}
```

### パフォーマンステスト

#### トランザクション性能テスト
```java
@Test
void processApi_ConcurrentExecution_Performance() {
    int numberOfThreads = 10;
    int numberOfExecutionsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Void>> futures = new ArrayList<>();
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < numberOfThreads; i++) {
        Future<Void> future = executor.submit(() -> {
            for (int j = 0; j < numberOfExecutionsPerThread; j++) {
                service.execute[Process](createTestRequest());
            }
            return null;
        });
        futures.add(future);
    }
    
    // すべてのスレッドの完了を待機
    futures.forEach(future -> {
        try {
            future.get();
        } catch (Exception e) {
            fail("Concurrent execution failed", e);
        }
    });
    
    long endTime = System.currentTimeMillis();
    long totalExecutions = numberOfThreads * numberOfExecutionsPerThread;
    double throughput = totalExecutions / ((endTime - startTime) / 1000.0);
    
    assertThat(throughput).isGreaterThan(100); // 100 TPS以上
}
```

---

## 監視とメトリクス

### ヘルスチェック

#### カスタムヘルスインジケーター
```java
@Component
public class ScalarDBHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Override
    public Health health() {
        try {
            DistributedTransaction transaction = transactionManager.start();
            transaction.abort(); // テスト用トランザクション
            
            return Health.up()
                .withDetail("scalardb", "Available")
                .withDetail("timestamp", Instant.now())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("scalardb", "Unavailable")
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }
}
```

### メトリクス収集
```java
@Component
public class TransactionMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter transactionCounter;
    private final Timer transactionTimer;
    
    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionCounter = Counter.builder("transactions.total")
            .description("Total number of transactions")
            .register(meterRegistry);
        this.transactionTimer = Timer.builder("transactions.duration")
            .description("Transaction execution time")
            .register(meterRegistry);
    }
    
    public void recordTransactionSuccess() {
        transactionCounter.increment(Tags.of("status", "success"));
    }
    
    public void recordTransactionFailure() {
        transactionCounter.increment(Tags.of("status", "failure"));
    }
    
    public Timer.Sample startTransactionTimer() {
        return Timer.start(meterRegistry);
    }
}
```

---

## セキュリティ考慮事項

### API認証・認可
- Bearer Tokenによる認証
- ロールベースアクセス制御（RBAC）
- API呼び出し制限（Rate Limiting）

### データ保護
- 機密データの暗号化
- SQLインジェクション対策
- ScalarDBのトランザクション分離による一貫性保証

### 監査ログ
- API呼び出しログ
- トランザクション操作ログ  
- エラー発生時の詳細ログ

---

## 実装チェックリスト

### System API実装
- [ ] エンティティクラス定義
- [ ] ScalarDBリポジトリ実装
- [ ] サービス層実装（トランザクション処理含む）
- [ ] コントローラー実装
- [ ] バリデーション実装
- [ ] 例外ハンドリング実装
- [ ] ユニットテスト実装
- [ ] 統合テスト実装

### Process API実装
- [ ] Process用のリクエスト/レスポンスDTO定義
- [ ] 複数System API呼び出しサービス実装
- [ ] 分散トランザクション制御実装
- [ ] 補償トランザクション実装（必要に応じて）
- [ ] プロセスレベルのテスト実装
- [ ] パフォーマンステスト実装

### 運用準備
- [ ] ScalarDB設定ファイル作成
- [ ] Docker設定作成
- [ ] ヘルスチェック実装
- [ ] メトリクス収集実装
- [ ] ログ設定実装
- [ ] セキュリティ設定実装

---

## 参考資料

- [ScalarDB Documentation](https://scalar-labs.com/docs/scalardb/latest/)
- [Spring Boot Reference Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [SQLite Documentation](https://www.sqlite.org/docs.html)