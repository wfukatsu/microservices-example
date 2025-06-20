package com.example.scalardb.systemapi.config;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.cluster.ScalarDBClusterManager;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.TransactionFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class EnterpriseScalarDBConfig {

    @Value("${scalardb.cluster.enabled:true}")
    private boolean clusterEnabled;

    @Value("${scalardb.cluster.contact_points:localhost}")
    private String clusterContactPoints;

    @Value("${scalardb.cluster.contact_port:60051}")
    private String clusterContactPort;

    @Value("${scalardb.storage.contact_points:localhost}")
    private String storageContactPoints;

    @Value("${scalardb.storage.contact_port:9042}")
    private String storageContactPort;

    @Value("${scalardb.storage.storage:cassandra}")
    private String storage;

    @Value("${scalardb.storage.username:}")
    private String username;

    @Value("${scalardb.storage.password:}")
    private String password;

    @Value("${scalardb.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${scalardb.auth.username:admin}")
    private String authUsername;

    @Value("${scalardb.auth.password:admin}")
    private String authPassword;

    @Bean
    public Properties scalarDbProperties() {
        Properties properties = new Properties();
        
        if (clusterEnabled) {
            // Cluster configuration
            properties.setProperty("scalar.db.cluster.contact_points", clusterContactPoints);
            properties.setProperty("scalar.db.cluster.contact_port", clusterContactPort);
            
            if (authEnabled) {
                properties.setProperty("scalar.db.auth.username", authUsername);
                properties.setProperty("scalar.db.auth.password", authPassword);
            }
        } else {
            // Direct storage configuration
            properties.setProperty("scalar.db.storage.contact_points", storageContactPoints);
            properties.setProperty("scalar.db.storage.contact_port", storageContactPort);
            properties.setProperty("scalar.db.storage.storage", storage);
            
            if (!username.isEmpty()) {
                properties.setProperty("scalar.db.storage.username", username);
            }
            if (!password.isEmpty()) {
                properties.setProperty("scalar.db.storage.password", password);
            }
        }
        
        // Performance tuning
        properties.setProperty("scalar.db.consensus_commit.isolation_level", "SNAPSHOT");
        properties.setProperty("scalar.db.consensus_commit.serializable_strategy", "EXTRA_READ");
        
        return properties;
    }

    @Bean
    @Profile("cluster")
    public ScalarDBClusterManager clusterManager() throws IOException {
        return new ScalarDBClusterManager(scalarDbProperties());
    }

    @Bean
    public DistributedStorage distributedStorage() throws IOException {
        if (clusterEnabled) {
            return clusterManager().getStorage();
        }
        return StorageFactory.create(scalarDbProperties());
    }

    @Bean
    public DistributedTransactionManager transactionManager(MeterRegistry meterRegistry) throws IOException {
        DistributedTransactionManager manager;
        
        if (clusterEnabled) {
            manager = clusterManager().getTransactionManager();
        } else {
            manager = TransactionFactory.create(scalarDbProperties());
        }
        
        // Add metrics instrumentation
        return new MetricsInstrumentedTransactionManager(manager, meterRegistry);
    }
}