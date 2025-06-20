package com.example.scalardb.systemapi.config;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.TransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class ScalarDBConfig {

    @Value("${scalardb.storage.contact_points:localhost}")
    private String contactPoints;

    @Value("${scalardb.storage.contact_port:9042}")
    private String contactPort;

    @Value("${scalardb.storage.storage:cassandra}")
    private String storage;

    @Value("${scalardb.storage.username:}")
    private String username;

    @Value("${scalardb.storage.password:}")
    private String password;

    @Bean
    public Properties scalarDbProperties() {
        Properties properties = new Properties();
        properties.setProperty("scalar.db.storage.contact_points", contactPoints);
        properties.setProperty("scalar.db.storage.contact_port", contactPort);
        properties.setProperty("scalar.db.storage.storage", storage);
        
        if (!username.isEmpty()) {
            properties.setProperty("scalar.db.storage.username", username);
        }
        if (!password.isEmpty()) {
            properties.setProperty("scalar.db.storage.password", password);
        }
        
        return properties;
    }

    @Bean
    public DistributedStorage distributedStorage() throws IOException {
        return StorageFactory.create(scalarDbProperties());
    }

    @Bean
    public DistributedTransactionManager transactionManager() throws IOException {
        return TransactionFactory.create(scalarDbProperties());
    }
}