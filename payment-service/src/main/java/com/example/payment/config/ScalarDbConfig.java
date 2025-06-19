package com.example.payment.config;

import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.service.TransactionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class ScalarDbConfig {
    
    @Bean
    public DistributedTransactionManager transactionManager() throws IOException {
        Properties properties = new Properties();
        
        // Load ScalarDB properties from classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("scalardb.properties")) {
            if (input == null) {
                throw new IOException("Unable to find scalardb.properties");
            }
            properties.load(input);
        }
        
        TransactionFactory factory = TransactionFactory.create(properties);
        return factory.getTransactionManager();
    }
}