package com.example.order.service;

import com.example.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache service for improving performance with Redis
 */
@Service
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cache key patterns
    private static final String ORDER_KEY_PREFIX = "order:";
    private static final String CUSTOMER_ORDERS_KEY_PREFIX = "customer_orders:";
    private static final String ORDER_ITEMS_KEY_PREFIX = "order_items:";
    
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Cache order by ID
     */
    @Cacheable(value = "orders", key = "#orderId", unless = "#result == null")
    public OrderResponse getOrderFromCache(String orderId) {
        // This method is used for cache annotation only
        return null;
    }

    /**
     * Update order cache
     */
    @CachePut(value = "orders", key = "#orderResponse.orderId")
    public OrderResponse putOrderToCache(OrderResponse orderResponse) {
        logger.debug("Caching order: {}", orderResponse.getOrderId());
        return orderResponse;
    }

    /**
     * Cache customer orders
     */
    @Cacheable(value = "customers", key = "#customerId", unless = "#result == null or #result.isEmpty()")
    public List<OrderResponse> getCustomerOrdersFromCache(String customerId) {
        // This method is used for cache annotation only
        return null;
    }

    /**
     * Update customer orders cache
     */
    @CachePut(value = "customers", key = "#customerId")
    public List<OrderResponse> putCustomerOrdersToCache(String customerId, List<OrderResponse> orders) {
        logger.debug("Caching {} orders for customer: {}", orders.size(), customerId);
        return orders;
    }

    /**
     * Evict order from cache when updated or deleted
     */
    @CacheEvict(value = "orders", key = "#orderId")
    public void evictOrderFromCache(String orderId) {
        logger.debug("Evicting order from cache: {}", orderId);
    }

    /**
     * Evict customer orders from cache when orders change
     */
    @CacheEvict(value = "customers", key = "#customerId")
    public void evictCustomerOrdersFromCache(String customerId) {
        logger.debug("Evicting customer orders from cache: {}", customerId);
    }

    /**
     * Evict all caches for a specific customer
     */
    public void evictAllCustomerCaches(String customerId) {
        evictCustomerOrdersFromCache(customerId);
        logger.debug("Evicted all customer caches for: {}", customerId);
    }

    /**
     * Cache order creation metrics
     */
    public void cacheOrderMetrics(String customerId, String orderId) {
        try {
            String key = "metrics:orders_today:" + customerId;
            redisTemplate.opsForSet().add(key, orderId);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            
            // Global metrics
            String globalKey = "metrics:orders_today:global";
            redisTemplate.opsForValue().increment(globalKey);
            redisTemplate.expire(globalKey, 24, TimeUnit.HOURS);
            
            logger.debug("Cached order metrics for customer: {} and order: {}", customerId, orderId);
        } catch (Exception e) {
            logger.warn("Failed to cache order metrics", e);
        }
    }

    /**
     * Get customer order count for today
     */
    public Long getCustomerOrderCountToday(String customerId) {
        try {
            String key = "metrics:orders_today:" + customerId;
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            logger.warn("Failed to get customer order count from cache", e);
            return 0L;
        }
    }

    /**
     * Get global order count for today
     */
    public Long getGlobalOrderCountToday() {
        try {
            String globalKey = "metrics:orders_today:global";
            Object count = redisTemplate.opsForValue().get(globalKey);
            return count != null ? Long.valueOf(count.toString()) : 0L;
        } catch (Exception e) {
            logger.warn("Failed to get global order count from cache", e);
            return 0L;
        }
    }

    /**
     * Cache frequently accessed data with custom TTL
     */
    public void cacheWithTtl(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            logger.debug("Cached data with key: {} and TTL: {} {}", key, timeout, timeUnit);
        } catch (Exception e) {
            logger.warn("Failed to cache data with key: {}", key, e);
        }
    }

    /**
     * Get cached data
     */
    public Object getCachedData(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.warn("Failed to get cached data with key: {}", key, e);
            return null;
        }
    }

    /**
     * Check cache health
     */
    public boolean isHealthy() {
        try {
            redisTemplate.opsForValue().set("health_check", "ok", 10, TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get("health_check");
            return "ok".equals(result);
        } catch (Exception e) {
            logger.error("Redis cache health check failed", e);
            return false;
        }
    }
}