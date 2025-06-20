# ScalarDB Community Edition - API テンプレート

## 概要
ScalarDB Community Edition用のマイクロサービスAPIテンプレートです。Spring Bootベースで、System APIとProcess APIの2層構成を提供します。

## 構成

### System API
**ディレクトリ**: `system-api/`

**機能**:
- ユーザーエンティティのCRUD操作
- ScalarDBトランザクション管理
- REST API提供

**主要ファイル**:
- `SystemApiApplication.java` - Spring Bootメインクラス
- `ScalarDBConfig.java` - ScalarDB設定
- `UserEntity.java` - ユーザーエンティティ
- `UserRepository.java` - データアクセス層
- `UserService.java` - ビジネスロジック
- `UserController.java` - REST API
- `application.yml` - アプリケーション設定

### Process API
**ディレクトリ**: `process-api/`

**機能**:
- ユーザー登録プロセス
- ユーザーオンボーディングプロセス
- System API連携
- 分散トランザクション管理

**主要ファイル**:
- `ProcessApiApplication.java` - Spring Bootメインクラス
- `SystemApiClient.java` - System API呼び出し
- `UserRegistrationService.java` - 登録プロセス
- `ProcessController.java` - プロセスAPI
- `application.yml` - アプリケーション設定

## セットアップ

### 前提条件
- Java 11以上
- Maven 3.6以上
- ScalarDB対応データベース（Cassandra、DynamoDB等）

### 1. データベース準備
```bash
# Cassandraの場合
docker run -p 9042:9042 -d cassandra:3.11
```

### 2. スキーマ作成
```sql
CREATE KEYSPACE user_service WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE KEYSPACE process_service WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE user_service.users (
    id text PRIMARY KEY,
    name text,
    email text,
    status text,
    created_at text,
    updated_at text
);

CREATE TABLE process_service.user_registrations (
    registration_id text PRIMARY KEY,
    name text,
    email text,
    status text,
    user_id text,
    created_at text,
    updated_at text
);

CREATE TABLE process_service.user_onboarding (
    onboarding_id text PRIMARY KEY,
    user_id text,
    user_name text,
    preferences text,
    status text,
    created_at text
);
```

### 3. アプリケーション起動

#### System API
```bash
cd system-api/
mvn spring-boot:run
```

#### Process API
```bash
cd process-api/
mvn spring-boot:run
```

## API使用例

### System API (ポート: 8080)

#### ユーザー作成
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

#### ユーザー取得
```bash
curl http://localhost:8080/api/v1/users/{user-id}
```

### Process API (ポート: 8081)

#### ユーザー登録プロセス
```bash
curl -X POST http://localhost:8081/api/v1/process/user-registration \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane@example.com"}'
```

#### ユーザーオンボーディング
```bash
curl -X POST http://localhost:8081/api/v1/process/user-onboarding \
  -H "Content-Type: application/json" \
  -d '{"userId": "{user-id}", "preferences": "theme=dark,lang=en"}'
```

## 設定カスタマイズ

### データベース設定
`application.yml`を編集:
```yaml
scalardb:
  storage:
    contact_points: your-database-host
    contact_port: your-database-port
    storage: cassandra  # or dynamodb, cosmos, etc.
    username: your-username
    password: your-password
```

### System API URL設定 (Process API)
```yaml
system-api:
  base-url: http://your-system-api-host:8080
```

## 監視

### ヘルスチェック
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

### メトリクス
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8081/actuator/metrics
```

## 開発ガイド

### エンティティ追加
1. 新しいエンティティクラス作成
2. リポジトリインターフェース実装
3. サービスクラス作成
4. コントローラー追加

### プロセス追加
1. プロセスサービスクラス作成
2. 必要なSystem API呼び出し実装
3. トランザクション境界定義
4. コントローラーエンドポイント追加

### テスト
```bash
mvn test
```

## トラブルシューティング

### よくある問題

#### データベース接続エラー
- 接続設定確認
- データベースサービス起動確認
- 認証情報確認

#### トランザクションエラー
- スキーマ設定確認
- 分散トランザクション設定確認

#### API呼び出しエラー
- System API起動確認
- ネットワーク接続確認
- URL設定確認