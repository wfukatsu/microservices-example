# ScalarDB Community Edition Documentation

## Overview
ScalarDB Community Edition is an open-source universal HTAP (Hybrid Transactional/Analytical Processing) engine that enables ACID transactions and real-time analytics across diverse databases.

## License
- Apache 2.0 License
- Available as open-source software
- No commercial license required

## Key Features
- Cross-database ACID transactions
- Multi-database transaction management
- Java-based application development
- Real-time analytics capabilities
- Support for diverse database backends

## Installation

### Maven Dependency
```xml
<dependency>
    <groupId>com.scalar-labs</groupId>
    <artifactId>scalardb</artifactId>
    <version>3.15.2</version>
</dependency>
```

### Gradle Dependency
```gradle
dependencies {
    implementation 'com.scalar-labs:scalardb:3.15.2'
}
```

## Architecture
- Universal HTAP Engine
- Cross-database transaction layer
- Supports multiple storage backends
- Distributed transaction coordinator

## Code Generation Rules

### Exception Handling
- Capitalize first character of exception messages
- No punctuation at the end of messages
- Use descriptive error messages

### Logging Standards
- Follow the same capitalization rules as exceptions
- Provide clear, actionable log messages
- Include relevant context information

### Development Guidelines
- Use pre-commit hooks for code formatting
- Follow Java coding standards
- Implement proper transaction boundaries
- Handle database connection pooling appropriately

## Sample Applications
Reference the ScalarDB samples repository for:
- Microservice transaction examples
- Multi-storage transaction patterns
- Analytics implementation samples
- JDBC integration examples
- Spring Data integration

## Documentation Reference
- Main Documentation: https://scalardb.scalar-labs.com/docs/latest/
- GitHub Repository: https://github.com/scalar-labs/scalardb
- Sample Applications: https://github.com/scalar-labs/scalardb-samples

## Community Support
- Use Stack Overflow with `scalardb` tag for questions
- File issues on GitHub repository
- Contribute to open-source development