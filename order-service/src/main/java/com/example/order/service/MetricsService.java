package com.example.order.service;

import com.example.order.config.MetricsConfig;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for tracking business metrics and performance indicators
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final Counter orderCreatedCounter;
    private final Counter orderCancelledCounter;
    private final Counter orderProcessingErrorCounter;
    private final Timer orderProcessingTimer;
    private final Counter compensationExecutedCounter;
    private final Counter paymentProcessedCounter;
    private final MetricsConfig metricsConfig;
    private final CacheService cacheService;
    private final MeterRegistry meterRegistry;

    public MetricsService(
            Counter orderCreatedCounter,
            Counter orderCancelledCounter,
            Counter orderProcessingErrorCounter,
            Timer orderProcessingTimer,
            Counter compensationExecutedCounter,
            Counter paymentProcessedCounter,
            MetricsConfig metricsConfig,
            @Autowired(required = false) CacheService cacheService,
            MeterRegistry meterRegistry) {
        this.orderCreatedCounter = orderCreatedCounter;
        this.orderCancelledCounter = orderCancelledCounter;
        this.orderProcessingErrorCounter = orderProcessingErrorCounter;
        this.orderProcessingTimer = orderProcessingTimer;
        this.compensationExecutedCounter = compensationExecutedCounter;
        this.paymentProcessedCounter = paymentProcessedCounter;
        this.metricsConfig = metricsConfig;
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record order creation metrics
     */
    public void recordOrderCreation(OrderResponse order) {
        try {
            // Increment counters
            orderCreatedCounter.increment();
            metricsConfig.incrementOrdersCreatedToday();

            // Add revenue (convert to yen cents for precision)
            BigDecimal amount = order.getTotalAmount();
            if (amount != null) {
                long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
                metricsConfig.addRevenue(amountInCents);
            }

            // Track order by status
            Counter.builder("orders.created.by.status")
                .tag("status", order.getStatus().name())
                .register(meterRegistry).increment();

            // Track order by payment method
            Counter.builder("orders.created.by.payment")
                .tag("payment_method", order.getPaymentMethod())
                .register(meterRegistry).increment();

            // Cache metrics if available
            if (cacheService != null) {
                cacheService.cacheOrderMetrics(order.getCustomerId(), order.getOrderId());
            }

            logger.debug("Recorded order creation metrics for order: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("Failed to record order creation metrics", e);
        }
    }

    /**
     * Record order cancellation metrics
     */
    public void recordOrderCancellation(String orderId, String customerId, OrderStatus previousStatus) {
        try {
            orderCancelledCounter.increment();
            metricsConfig.incrementOrdersCancelledToday();

            // Track cancellation by previous status
            Counter.builder("orders.cancelled.by.status")
                .tag("previous_status", previousStatus.name())
                .register(meterRegistry).increment();

            logger.debug("Recorded order cancellation metrics for order: {}", orderId);

        } catch (Exception e) {
            logger.error("Failed to record order cancellation metrics", e);
        }
    }

    /**
     * Record order processing error metrics
     */
    public void recordOrderProcessingError(String orderId, String errorCode, String errorMessage) {
        try {
            orderProcessingErrorCounter.increment();
            metricsConfig.incrementOrderProcessingErrors();

            // Track errors by type
            Counter.builder("orders.processing.errors.by.type")
                .tag("error_code", errorCode)
                .register(meterRegistry).increment();

            logger.debug("Recorded order processing error metrics for order: {} with error: {}", orderId, errorCode);

        } catch (Exception e) {
            logger.error("Failed to record order processing error metrics", e);
        }
    }

    /**
     * Time order processing duration
     */
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start();
    }

    /**
     * Record order processing completion time
     */
    public void recordOrderProcessingTime(Timer.Sample sample, String orderId, boolean success) {
        try {
            long durationNanos = sample.stop(orderProcessingTimer);
            Duration duration = Duration.ofNanos(durationNanos);
            
            // Record processing time with tags
            Timer.builder("orders.processing.duration.detailed")
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry).record(duration);

            logger.debug("Recorded order processing time for order: {} - Duration: {}ms", 
                orderId, duration.toMillis());

        } catch (Exception e) {
            logger.error("Failed to record order processing time", e);
        }
    }

    /**
     * Record compensation execution metrics
     */
    public void recordCompensationExecution(String orderId, String compensationType, boolean success) {
        try {
            compensationExecutedCounter.increment();

            // Track compensation by type and result
            Counter.builder("orders.compensation.by.type")
                .tag("type", compensationType)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry).increment();

            logger.debug("Recorded compensation execution metrics for order: {} - Type: {}, Success: {}", 
                orderId, compensationType, success);

        } catch (Exception e) {
            logger.error("Failed to record compensation execution metrics", e);
        }
    }

    /**
     * Record payment processing metrics
     */
    public void recordPaymentProcessing(String orderId, String paymentMethod, BigDecimal amount, boolean success) {
        try {
            paymentProcessedCounter.increment();

            // Track payments by method and result
            Counter.builder("payments.processed.by.method")
                .tag("payment_method", paymentMethod)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry).increment();

            // Track payment amounts
            if (amount != null) {
                Counter.builder("payments.processed.by.amount")
                    .tag("amount_range", getAmountRange(amount))
                    .register(meterRegistry).increment();
            }

            logger.debug("Recorded payment processing metrics for order: {} - Method: {}, Amount: {}, Success: {}", 
                orderId, paymentMethod, amount, success);

        } catch (Exception e) {
            logger.error("Failed to record payment processing metrics", e);
        }
    }

    /**
     * Get current business metrics summary
     */
    public BusinessMetricsSummary getCurrentMetrics() {
        BusinessMetricsSummary summary = new BusinessMetricsSummary();
        summary.setOrdersCreatedToday(metricsConfig.getOrdersCreatedToday());
        summary.setOrdersCancelledToday(metricsConfig.getOrdersCancelledToday());
        summary.setOrderProcessingErrors(metricsConfig.getOrderProcessingErrors());
        summary.setTotalRevenue(new BigDecimal(metricsConfig.getTotalRevenue()).divide(new BigDecimal("100")));
        
        // Get additional metrics from cache if available
        if (cacheService != null) {
            summary.setGlobalOrdersToday(cacheService.getGlobalOrderCountToday());
        }
        
        return summary;
    }

    /**
     * Reset daily metrics (typically called at midnight)
     */
    public void resetDailyMetrics() {
        try {
            metricsConfig.resetDailyCounters();
            logger.info("Daily metrics have been reset");
        } catch (Exception e) {
            logger.error("Failed to reset daily metrics", e);
        }
    }

    /**
     * Categorize payment amounts for metrics
     */
    private String getAmountRange(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            return "small"; // < 1,000 yen
        } else if (amount.compareTo(new BigDecimal("10000")) < 0) {
            return "medium"; // 1,000 - 10,000 yen
        } else if (amount.compareTo(new BigDecimal("100000")) < 0) {
            return "large"; // 10,000 - 100,000 yen
        } else {
            return "extra_large"; // > 100,000 yen
        }
    }

    /**
     * Business metrics summary DTO
     */
    public static class BusinessMetricsSummary {
        private long ordersCreatedToday;
        private long ordersCancelledToday;
        private long orderProcessingErrors;
        private BigDecimal totalRevenue;
        private Long globalOrdersToday;

        // Getters and setters
        public long getOrdersCreatedToday() {
            return ordersCreatedToday;
        }

        public void setOrdersCreatedToday(long ordersCreatedToday) {
            this.ordersCreatedToday = ordersCreatedToday;
        }

        public long getOrdersCancelledToday() {
            return ordersCancelledToday;
        }

        public void setOrdersCancelledToday(long ordersCancelledToday) {
            this.ordersCancelledToday = ordersCancelledToday;
        }

        public long getOrderProcessingErrors() {
            return orderProcessingErrors;
        }

        public void setOrderProcessingErrors(long orderProcessingErrors) {
            this.orderProcessingErrors = orderProcessingErrors;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public Long getGlobalOrdersToday() {
            return globalOrdersToday;
        }

        public void setGlobalOrdersToday(Long globalOrdersToday) {
            this.globalOrdersToday = globalOrdersToday;
        }
    }
}