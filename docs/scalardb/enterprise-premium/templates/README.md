# ScalarDB Enterprise Premium Edition - API テンプレート

## 概要
ScalarDB Enterprise Premium Edition用の高機能マイクロサービスAPIテンプレートです。Spring Data JDBC for ScalarDB、GraphQL、ベクター検索機能を統合しています。

## 主要機能

### Advanced Features
- **Spring Data JDBC for ScalarDB**: JPA風のデータアクセス
- **GraphQL API**: 柔軟なクエリ機能
- **Vector Search**: AI/ML統合用ベクター検索
- **SQL Interface**: 標準SQL対応
- **Multi-Cloud Support**: 複数クラウド対応

### Enterprise Features
- **高可用性クラスタ**: 自動フェイルオーバー
- **認証・認可**: エンタープライズ認証統合
- **監視・メトリクス**: Prometheusメトリクス
- **分散トレーシング**: Zipkin/Jaeger対応

## 構成

### System API
**ディレクトリ**: `system-api/`

**機能**:
- Spring Data JDBC リポジトリパターン
- REST + GraphQL双方対応
- トランザクション管理
- ベクター検索統合

**主要ファイル**:
- `PremiumScalarDBConfig.java` - Premium機能設定
- `User.java` - Spring Data JDBCエンティティ
- `UserRepository.java` - Spring Data JDBCリポジトリ
- `UserService.java` - ビジネスロジック
- `UserController.java` - REST API
- `UserGraphQLController.java` - GraphQL API
- `schema.graphqls` - GraphQLスキーマ

## セットアップ

### 前提条件
- Java 17以上
- Maven 3.8以上
- ScalarDB Enterprise Premium License
- クラスタ環境（本格運用時）

### 1. Premium機能有効化
```yaml
scalardb:
  vector:
    enabled: true
  sql:
    enabled: true
  graphql:
    enabled: true
```

### 2. クラスタ設定
```yaml
scalardb:
  cluster:
    contact_points: node1,node2,node3
    contact_port: 60051
  auth:
    username: admin
    password: premium-password
```

### 3. アプリケーション起動
```bash
mvn spring-boot:run -Dspring.profiles.active=premium
```

## API使用例

### REST API

#### ユーザー作成
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -u api-user:api-password \
  -d '{"name": "Premium User", "email": "premium@example.com"}'
```

### GraphQL API

#### GraphiQL Playground
ブラウザで `http://localhost:8080/graphiql` にアクセス

#### ユーザー取得クエリ
```graphql
query {
  userById(id: "user-id") {
    id
    name
    email
    status
    createdAt
    updatedAt
  }
}
```

#### ユーザー検索クエリ
```graphql
query {
  searchUsers(namePattern: "John") {
    id
    name
    email
    status
  }
}
```

#### ユーザー作成ミューテーション
```graphql
mutation {
  createUser(input: {
    name: "GraphQL User"
    email: "graphql@example.com"
  }) {
    id
    name
    email
    status
  }
}
```

#### 統計クエリ
```graphql
query {
  activeUserCount
  inactiveUserCount
  recentUsers(days: 7) {
    id
    name
    createdAt
  }
}
```

## Premium機能

### Vector Search統合
```java
@Service
public class UserVectorSearchService {
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    public List<User> findSimilarUsers(String description) {
        // ベクター検索実装
        float[] queryVector = embeddingService.generateEmbedding(description);
        return vectorSearchService.searchSimilar(queryVector, 10);
    }
}
```

### SQL Interface使用
```java
@Service
public class UserSqlService {
    
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    
    public List<User> complexQuery() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(
                "SELECT * FROM users WHERE created_at > ? AND status = ?",
                LocalDate.now().minusDays(30), "ACTIVE"
            );
        }
    }
}
```

### 監視・メトリクス

#### Prometheus メトリクス
```bash
curl http://localhost:8080/actuator/prometheus
```

#### GraphQL メトリクス
```bash
curl http://localhost:8080/actuator/metrics/graphql.request.duration
```

#### カスタムメトリクス
```java
@Component
public class CustomMetrics {
    private final Counter userRegistrations;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.userRegistrations = Counter.builder("user.registrations")
            .description("Number of user registrations")
            .register(meterRegistry);
    }
}
```

## 高度な設定

### Multi-Cloud設定
```yaml
scalardb:
  cluster:
    contact_points: 
      - region1.cluster.example.com
      - region2.cluster.example.com
    global_distribution:
      enabled: true
      conflict_resolution: LAST_WRITER_WINS
```

### セキュリティ強化
```yaml
app:
  security:
    jwt:
      enabled: true
      secret: ${JWT_SECRET}
    oauth2:
      enabled: true
      provider: ${OAUTH2_PROVIDER}
```

### パフォーマンス調整
```yaml
scalardb:
  cluster:
    grpc:
      max_inbound_message_size: 4194304
      max_inbound_metadata_size: 8192
    connection_pool:
      max_connections: 100
      min_connections: 10
```

## 開発ガイド

### Spring Data JDBC活用
```java
@Repository
public interface CustomUserRepository extends CrudRepository<User, String> {
    
    @Query("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Modifying
    @Query("UPDATE users SET status = :status WHERE id = :id")
    void updateStatus(@Param("id") String id, @Param("status") String status);
}
```

### GraphQLスキーマ拡張
```graphql
extend type Query {
    # カスタムクエリ追加
    userAnalytics(period: String!): UserAnalytics!
}

type UserAnalytics {
    totalUsers: Long!
    activeUsers: Long!
    newUsersThisMonth: Long!
    userGrowthRate: Float!
}
```

### ベクター検索実装
```java
@Service
public class DocumentVectorService {
    
    public void indexDocument(String content, Map<String, Object> metadata) {
        float[] vector = embeddingService.embed(content);
        VectorDocument doc = VectorDocument.builder()
            .content(content)
            .vector(vector)
            .metadata(metadata)
            .build();
        vectorSearchService.index(doc);
    }
}
```

## 運用・監視

### ヘルスチェック強化
```bash
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health/liveness
```

### アプリケーションメトリクス
- トランザクション成功/失敗率
- GraphQLクエリ実行時間
- ベクター検索パフォーマンス
- API レスポンス時間

### ログ分析
```yaml
logging:
  level:
    com.scalar.db.vector: DEBUG
    org.springframework.graphql: DEBUG
  pattern:
    console: "%d [%X{traceId},%X{spanId}] %-5level %logger - %msg%n"
```

## トラブルシューティング

### Premium機能関連
- ライセンス設定確認
- 機能有効化設定確認
- クラスタ接続確認

### GraphQL関連
- スキーマ検証
- Resolver実装確認
- セキュリティ設定確認

### ベクター検索関連
- インデックス作成確認
- 埋め込みモデル設定確認
- 検索精度調整