# ScalarDB Community Edition - Code Generation Rules

## Core Principles

### 1. Transaction Management
- Always define clear transaction boundaries
- Use try-with-resources for proper resource management
- Implement proper rollback mechanisms
- Handle concurrent access patterns

### 2. Database Abstraction
- Use ScalarDB's abstract storage interface
- Avoid database-specific SQL or NoSQL queries
- Leverage ScalarDB's cross-database capabilities
- Implement vendor-neutral data access patterns

### 3. Error Handling
```java
// Correct format
throw new IllegalArgumentException("Invalid argument provided");

// Incorrect format
throw new IllegalArgumentException("Invalid argument provided.");
```

### 4. Configuration Management
- Use configuration files for database connections
- Implement environment-specific configurations
- Separate configuration from business logic
- Support multiple storage backends

## Code Templates

### Basic Transaction Template
```java
public class ScalarDBTransaction {
    private final DistributedTransactionManager manager;
    
    public void executeTransaction() throws Exception {
        DistributedTransaction transaction = manager.start();
        try {
            // Transaction logic here
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw e;
        }
    }
}
```

### Storage Configuration Template
```java
public class StorageConfig {
    public static StorageService createStorage() {
        return StorageFactory.create(loadProperties());
    }
    
    private static Properties loadProperties() {
        Properties props = new Properties();
        // Load configuration
        return props;
    }
}
```

### CRUD Operations Template
```java
public class DataAccessLayer {
    private final DistributedStorage storage;
    
    public Optional<Result> get(Get get) throws ExecutionException {
        return storage.get(get);
    }
    
    public void put(Put put) throws ExecutionException {
        storage.put(put);
    }
    
    public void delete(Delete delete) throws ExecutionException {
        storage.delete(delete);
    }
}
```

## Best Practices

### Performance Optimization
- Use batch operations when possible
- Implement proper indexing strategies
- Monitor transaction performance
- Optimize query patterns

### Testing Guidelines
- Write integration tests for cross-database scenarios
- Mock external dependencies appropriately
- Test transaction rollback scenarios
- Validate data consistency across databases

### Documentation Standards
- Document transaction boundaries clearly
- Provide configuration examples
- Include error handling documentation
- Explain cross-database implications