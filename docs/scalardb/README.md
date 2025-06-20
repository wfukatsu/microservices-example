# ScalarDB Documentation Summary

This directory contains comprehensive documentation for ScalarDB across all editions, organized for code generation and development guidance.

## Directory Structure

```
scalardb/
├── community/                  # Community Edition (Open Source)
│   ├── README.md              # Community features and setup
│   └── code-generation-rules.md  # Community-specific coding guidelines
├── enterprise-standard/        # Enterprise Standard Edition
│   ├── README.md              # Enterprise Standard features
│   └── code-generation-rules.md  # Enterprise Standard coding guidelines
├── enterprise-premium/         # Enterprise Premium Edition
│   ├── README.md              # Premium features and capabilities
│   └── code-generation-rules.md  # Premium-specific coding guidelines
├── architecture-guidelines.md  # Overall architecture guidance
└── README.md                   # This file
```

## Edition Comparison

### Community Edition
- **License**: Apache 2.0 (Open Source)
- **Features**: Basic HTAP engine, cross-database transactions
- **Use Cases**: Development, small-scale applications, POCs
- **Support**: Community support via Stack Overflow

### Enterprise Standard Edition
- **License**: Commercial license required
- **Features**: Community + Clustering, Authentication, Monitoring
- **Use Cases**: Production deployments, high availability
- **Support**: Professional support included

### Enterprise Premium Edition
- **License**: Premium commercial license required
- **Features**: Standard + Vector Search, GraphQL/SQL, Global Distribution
- **Use Cases**: Large-scale, AI/ML applications, multi-cloud
- **Support**: 24/7 priority support + professional services

## Key Architecture Concepts

### Universal HTAP Engine
ScalarDB provides a unified layer for both transactional (OLTP) and analytical (OLAP) workloads across multiple database systems.

### Cross-Database Transactions
Enables ACID transactions that span multiple heterogeneous databases (MySQL, PostgreSQL, Cassandra, DynamoDB, etc.).

### Storage Abstraction
Provides a unified API that abstracts away database-specific implementations, enabling database-agnostic application development.

## Code Generation Guidelines

### Common Patterns Across All Editions
1. **Transaction Boundary Management**: Always define clear transaction scopes
2. **Storage Abstraction Usage**: Use ScalarDB APIs rather than database-specific code
3. **Configuration Management**: Externalize configuration for different environments
4. **Error Handling**: Implement proper exception handling and retry logic
5. **Resource Management**: Use proper connection pooling and resource cleanup

### Edition-Specific Considerations
- **Community**: Focus on basic transaction patterns and single-node deployments
- **Enterprise Standard**: Include cluster-aware code, authentication, and monitoring
- **Enterprise Premium**: Add vector search, multi-interface support, and global distribution patterns

## Getting Started

1. **Choose Your Edition**: Determine which edition fits your requirements
2. **Review Architecture Guidelines**: Understand the overall architecture patterns
3. **Study Edition-Specific Documentation**: Focus on your chosen edition's features
4. **Follow Code Generation Rules**: Implement according to edition-specific guidelines
5. **Reference Sample Applications**: Use ScalarDB samples repository for examples

## Documentation Sources

- **Official Documentation**: https://scalardb.scalar-labs.com/docs/latest/
- **GitHub Repository**: https://github.com/scalar-labs/scalardb
- **Sample Applications**: https://github.com/scalar-labs/scalardb-samples
- **Helm Charts**: https://github.com/scalar-labs/helm-charts

## Support and Community

- **Community Support**: Stack Overflow with `scalardb` tag
- **Enterprise Support**: Through Scalar Inc. license agreement
- **Issues and Bugs**: GitHub repository issues
- **Professional Services**: Available for Enterprise customers