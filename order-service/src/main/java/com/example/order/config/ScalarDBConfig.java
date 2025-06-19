package com.example.order.config;

import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.service.TransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class ScalarDBConfig {

    @Value("${scalardb.properties}")
    private String scalarDBPropertiesFile;

    @Bean
    public DistributedTransactionManager transactionManager() throws IOException {
        TransactionFactory factory = TransactionFactory.create(scalarDBPropertiesFile);
        return factory.getTransactionManager();
    }
}