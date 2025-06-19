# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

This repository is a **blueprint for ScalarDB OSS + Spring Boot microservice implementations** using SQLite as the database backend. It demonstrates distributed transaction patterns and best practices for building scalable APIs with ScalarDB's open-source universal transaction management.

**Current Status**: This is a planning/specification repository. The implementation has not yet been created - only the architectural documentation and project structure exist.

## Planned Project Structure

**Target Implementation**: Each microservice sample should be organized in its own directory under the root:

```
microservice-samples/
├── order-service/             # Order management API with ScalarDB
├── inventory-service/         # Inventory management API with ScalarDB
├── payment-service/           # Payment processing API with ScalarDB
├── customer-service/          # Customer management API with ScalarDB
├── shared/                    # Shared ScalarDB OSS utilities and configurations
│   ├── scalardb-config/       # ScalarDB OSS configuration files for SQLite
│   ├── sqlite-data/           # SQLite database files for local development
│   └── docker-compose/        # Docker Compose with ScalarDB OSS containers
└── docs/                      # ScalarDB architecture and transaction patterns
```

**Current State**: Only `docs/` directory and this CLAUDE.md file exist. Implementation is pending.

## Getting Started

Since this repository contains only specifications, you'll need to implement the microservices from scratch. Start by:

1. **Choose a service to implement first** (recommended: `order-service` as it's foundational)
2. **Create the service directory structure** following the conventions below
3. **Set up the Maven/Gradle build file** with required dependencies
4. **Configure ScalarDB properties** for SQLite integration
5. **Define the database schema** using ScalarDB format
6. **Implement the Spring Boot application** with ScalarDB transaction management

## Development Conventions

### Service Structure
Each Spring Boot + ScalarDB microservice should follow this structure:
- `src/main/java/` - Java source code with Spring Boot application
- `src/main/resources/` - Configuration files including ScalarDB properties
- `src/test/java/` - Unit and integration tests with ScalarDB test utilities
- `schema/` - ScalarDB schema definitions and table creation scripts
- `docker/` - Dockerfile and ScalarDB database setup
- `README.md` - Service-specific setup and ScalarDB configuration instructions
- `pom.xml` or `build.gradle` - Maven/Gradle dependencies including ScalarDB

### ScalarDB OSS + SQLite Patterns
- **Distributed Transactions**: Use ScalarDB OSS transaction manager for ACID properties across multiple SQLite databases
- **Schema Management**: Define tables using ScalarDB's schema format with SQLite-compatible column types
- **Transaction Boundaries**: Implement proper transaction scoping in service layer methods
- **Error Handling**: Handle ScalarDB-specific exceptions (TransactionException, ExecutionException)
- **SQLite Configuration**: File-based SQLite databases with proper isolation levels
- **Health Checks**: Include SQLite database connectivity in application health endpoints

### Testing Strategy
- **Unit Tests**: Business logic with mocked ScalarDB OSS transaction manager
- **Integration Tests**: Full ScalarDB OSS integration with in-memory SQLite databases
- **Transaction Tests**: Verify ACID properties and distributed transaction behavior with SQLite
- **Performance Tests**: ScalarDB OSS transaction throughput and latency testing with SQLite
- **Contract Tests**: API compatibility between services using ScalarDB OSS schemas

### Inter-Service Communication Patterns
- **Synchronous**: REST APIs with ScalarDB distributed transactions across services
- **Asynchronous**: Event-driven patterns using ScalarDB for reliable event storage
- **Saga Pattern**: Implement distributed transactions across multiple microservices
- **Two-Phase Commit**: Leverage ScalarDB's built-in distributed transaction coordination
- **Compensation**: Handle transaction rollbacks across service boundaries

## Spring Boot + ScalarDB Development Commands

**Project Setup:**
- `./mvnw clean install` - Install dependencies including ScalarDB libraries
- `./mvnw dependency:tree` - Verify ScalarDB dependency resolution
- `./mvnw spring-boot:run` - Start Spring Boot application with ScalarDB

**Development:**
- `./mvnw spring-boot:run -Dspring.profiles.active=dev` - Run with development profile
- `./mvnw test` - Run unit tests with ScalarDB test utilities
- `./mvnw verify` - Run integration tests with ScalarDB containers
- `./mvnw spotless:apply` - Format code according to project standards

**ScalarDB Schema Management:**
- `java -jar scalardb-schema-loader.jar --config scalardb.properties --schema-file schema.json` - Load schema
- `java -jar scalardb-schema-loader.jar --config scalardb.properties --schema-file schema.json --delete-all` - Reset schema

**SQLite Database Operations:**
- `ls -la shared/sqlite-data/` - View SQLite database files
- `sqlite3 shared/sqlite-data/service.db ".tables"` - List tables in SQLite database
- `sqlite3 shared/sqlite-data/service.db ".schema"` - View SQLite schema
- `docker-compose up -d` - Start ScalarDB OSS containers (if using containerized setup)
- `docker-compose down` - Stop ScalarDB OSS containers

## Docker and ScalarDB Orchestration

**Docker Configuration:**
- Multi-stage Dockerfile with OpenJDK base image for Spring Boot applications
- ScalarDB client libraries bundled in application image
- Health check endpoints for container readiness probes

**Docker Compose Setup (Optional):**
- Application containers with ScalarDB OSS embedded mode (no separate ScalarDB server needed)
- Volume mounts for SQLite database files and ScalarDB configuration
- Network configuration for inter-service communication
- Shared SQLite data directory for development

**Local Development (Recommended):**
- ScalarDB OSS in embedded mode within Spring Boot applications
- SQLite database files stored in `shared/sqlite-data/` directory
- No external database servers or containers required
- Simple file-based persistence for development and testing

## Required Dependencies

Each Spring Boot + ScalarDB service must include:

```xml
<dependency>
    <groupId>com.scalar-labs</groupId>
    <artifactId>scalardb</artifactId>
    <version>3.9.0</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.2.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Configuration Files Required

- `scalardb.properties` - ScalarDB OSS connection settings for SQLite
- `application.yml` - Spring Boot application configuration with ScalarDB OSS integration
- `schema.json` - ScalarDB table definitions compatible with SQLite column types
- `docker-compose.yml` - Optional containerized development environment

## ScalarDB OSS + SQLite Configuration Example

**scalardb.properties:**
```properties
scalar.db.storage=jdbc
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/service.db
scalar.db.username=
scalar.db.password=
scalar.db.isolation_level=SNAPSHOT
```

**application.yml:**
```yaml
spring:
  application:
    name: example-service
scalardb:
  properties: classpath:scalardb.properties
```

**schema.json:**
```json
{
  "example_service.orders": {
    "transaction": true,
    "partition-key": ["order_id"],
    "columns": {
      "order_id": "TEXT",
      "customer_id": "TEXT",
      "total_amount": "BIGINT",
      "status": "TEXT",
      "created_at": "BIGINT"
    }
  }
}
```

## Documentation Requirements

Each ScalarDB OSS microservice should include:
- **Service Domain**: Business context and data ownership boundaries
- **ScalarDB OSS Schema**: Table definitions with SQLite-compatible column types
- **API Specification**: REST endpoints with transaction semantics
- **SQLite Configuration**: Database file locations and ScalarDB OSS properties
- **Transaction Patterns**: How distributed transactions are implemented across SQLite databases
- **Development Setup**: Local SQLite database initialization and schema loading