package com.example.order.config;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.service.StorageFactory;
import com.scalar.db.service.TransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class ScalarDBConfig {

    @Value("${scalardb.properties:classpath:scalardb.properties}")
    private Resource scalardbPropertiesResource;

    @Bean
    public Properties scalarDbProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = scalardbPropertiesResource.getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

    @Bean
    public DistributedStorage distributedStorage() throws IOException {
        StorageFactory factory = StorageFactory.create(scalarDbProperties());
        return factory.getStorage();
    }

    @Bean
    public DistributedTransactionManager transactionManager() throws IOException {
        TransactionFactory factory = TransactionFactory.create(scalarDbProperties());
        return factory.getTransactionManager();
    }
}