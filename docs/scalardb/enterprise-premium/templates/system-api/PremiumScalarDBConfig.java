package com.example.scalardb.systemapi.config;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.cluster.ScalarDBClusterManager;
import com.scalar.db.sql.SqlSessionFactory;
import com.scalar.db.vector.VectorSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import java.io.IOException;
import java.util.Properties;

@Configuration
@EnableJdbcRepositories(basePackages = "com.example.scalardb.systemapi.repository")
public class PremiumScalarDBConfig {

    @Value("${scalardb.cluster.contact_points:localhost}")
    private String clusterContactPoints;

    @Value("${scalardb.cluster.contact_port:60051}")
    private String clusterContactPort;

    @Value("${scalardb.auth.username:admin}")
    private String authUsername;

    @Value("${scalardb.auth.password:admin}")
    private String authPassword;

    @Value("${scalardb.vector.enabled:true}")
    private boolean vectorSearchEnabled;

    @Value("${scalardb.sql.enabled:true}")
    private boolean sqlEnabled;

    @Value("${scalardb.graphql.enabled:true}")
    private boolean graphqlEnabled;

    @Bean
    public Properties scalarDbProperties() {
        Properties properties = new Properties();
        
        // Cluster configuration
        properties.setProperty("scalar.db.cluster.contact_points", clusterContactPoints);
        properties.setProperty("scalar.db.cluster.contact_port", clusterContactPort);
        
        // Authentication
        properties.setProperty("scalar.db.auth.username", authUsername);
        properties.setProperty("scalar.db.auth.password", authPassword);
        
        // Premium features
        if (vectorSearchEnabled) {
            properties.setProperty("scalar.db.vector.enabled", "true");
        }
        
        if (sqlEnabled) {
            properties.setProperty("scalar.db.sql.enabled", "true");
        }
        
        if (graphqlEnabled) {
            properties.setProperty("scalar.db.graphql.enabled", "true");
        }
        
        // Performance tuning for premium
        properties.setProperty("scalar.db.consensus_commit.isolation_level", "SNAPSHOT");
        properties.setProperty("scalar.db.consensus_commit.serializable_strategy", "EXTRA_READ");
        properties.setProperty("scalar.db.cluster.grpc.max_inbound_message_size", "4194304");
        properties.setProperty("scalar.db.cluster.grpc.max_inbound_metadata_size", "8192");
        
        return properties;
    }

    @Bean
    public ScalarDBClusterManager clusterManager() throws IOException {
        return new ScalarDBClusterManager(scalarDbProperties());
    }

    @Bean
    public DistributedStorage distributedStorage() throws IOException {
        return clusterManager().getStorage();
    }

    @Bean
    public DistributedTransactionManager transactionManager() throws IOException {
        return clusterManager().getTransactionManager();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws IOException {
        if (sqlEnabled) {
            return clusterManager().getSqlSessionFactory();
        }
        return null;
    }

    @Bean
    public VectorSearchService vectorSearchService() throws IOException {
        if (vectorSearchEnabled) {
            return new VectorSearchService(scalarDbProperties());
        }
        return null;
    }
}