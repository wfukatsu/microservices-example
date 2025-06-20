# ScalarDB マイクロサービス API テンプレート概要

## 概要
ScalarDBの各エディション向けに、マイクロサービス化されたAPIテンプレートを提供します。System APIとProcess APIの2層アーキテクチャで構成され、ScalarDBのトランザクション機能を活用します。

## アーキテクチャ

### System API
- **目的**: 単一のドメインエンティティに対するCRUD処理
- **責任**: データの永続化、基本的なビジネスルール適用
- **トランザクション**: 単一エンティティのトランザクション管理

### Process API  
- **目的**: 複数のSystem APIを組み合わせた複合処理
- **責任**: ビジネスプロセス、ワークフロー管理
- **トランザクション**: 分散トランザクション管理

## エディション別テンプレート

### Community Edition
**場所**: `community/templates/`

**特徴**:
- SpringBootベースの基本的なREST API
- ScalarDB Community APIの直接使用
- 基本的なCRUD操作とトランザクション管理

**技術スタック**:
- Spring Boot 3.1
- ScalarDB Community 3.15.2
- Spring Web MVC
- WebClient (Process API用)

### Enterprise Standard Edition
**場所**: `enterprise-standard/templates/`

**特徴**:
- クラスタ機能とセキュリティ統合
- 認証・認可機能
- メトリクス監視機能
- 高可用性対応

**技術スタック**:
- Spring Boot 3.1
- ScalarDB Enterprise 3.15.2
- Spring Security
- Micrometer + Prometheus

### Enterprise Premium Edition
**場所**: `enterprise-premium/templates/`

**特徴**:
- Spring Data JDBC for ScalarDB使用
- GraphQL API対応
- ベクター検索機能
- SQL インターフェース対応

**技術スタック**:
- Spring Boot 3.1
- ScalarDB Premium 3.15.2
- Spring Data JDBC for ScalarDB
- Spring GraphQL
- Vector Search API

## 共通機能

### トランザクション管理
全エディションで以下のトランザクションパターンを実装:

1. **System API**: 単一エンティティトランザクション
2. **Process API**: 複数System API呼び出しでの分散トランザクション
3. **エラー処理**: 適切なロールバック機能

### REST API エンドポイント
標準的なCRUD操作:
- `POST /api/v1/users` - ユーザー作成
- `GET /api/v1/users/{id}` - ユーザー取得
- `GET /api/v1/users` - ユーザー一覧取得
- `PUT /api/v1/users/{id}` - ユーザー更新
- `DELETE /api/v1/users/{id}` - ユーザー削除

### Process API エンドポイント
ビジネスプロセス:
- `POST /api/v1/process/user-registration` - ユーザー登録プロセス
- `POST /api/v1/process/user-onboarding` - ユーザーオンボーディング

## 使用方法

### 1. エディション選択
要件に応じて適切なエディションのテンプレートを選択

### 2. 設定カスタマイズ
- `application.yml`の環境設定
- データベース接続設定
- セキュリティ設定（Enterprise以上）

### 3. ビジネスロジック実装
- エンティティモデルのカスタマイズ
- ビジネスルールの追加
- プロセスフローの定義

### 4. デプロイメント
- Dockerコンテナ化
- Kubernetes対応（Enterprise以上）
- 監視・ログ設定

## ベストプラクティス

### トランザクション設計
1. System APIでは単純なトランザクション境界を維持
2. Process APIで複合トランザクションを管理
3. 適切なエラーハンドリングとロールバック

### セキュリティ
1. API認証の実装（Enterprise以上）
2. ロールベースアクセス制御
3. 監査ログ記録

### 監視・運用
1. メトリクス収集と監視
2. 分散トレーシング
3. ヘルスチェック実装

## カスタマイズポイント

### エンティティ拡張
- `User`エンティティを他のドメインエンティティに変更
- フィールド追加・変更
- バリデーションルール追加

### プロセス追加
- 新しいビジネスプロセスの追加
- 複数System API連携パターン
- 外部サービス統合

### 技術統合
- 他のSpringBootライブラリ統合
- 外部監視システム連携
- CI/CDパイプライン統合