package com.example.order.repository;

import com.example.order.entity.OrderItem;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextColumn;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderItemRepository {
    
    private static final String NAMESPACE = "order_service";
    private static final String TABLE_NAME = "order_items";

    public void save(DistributedTransaction transaction, OrderItem orderItem) {
        Put put = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(orderItem.getPartitionKey())
            .build();
        
        orderItem.getColumns().forEach((name, column) -> put.withValue(column));
        
        try {
            transaction.put(put);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save order item: " + orderItem.getOrderId() + "-" + orderItem.getProductId(), e);
        }
    }

    public void saveAll(DistributedTransaction transaction, List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            save(transaction, orderItem);
        }
    }

    public Optional<OrderItem> findById(DistributedTransaction transaction, String orderId, String productId) {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.of(
                TextColumn.of("order_id", orderId),
                TextColumn.of("product_id", productId)
            ))
            .build();
        
        try {
            Optional<Result> result = transaction.get(get);
            return result.map(this::mapToOrderItem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find order item: " + orderId + "-" + productId, e);
        }
    }

    public List<OrderItem> findByOrderId(DistributedTransaction transaction, String orderId) {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("order_id", orderId))
            .build();
        
        List<OrderItem> orderItems = new ArrayList<>();
        try {
            List<Result> results = transaction.scan(scan);
            for (Result result : results) {
                orderItems.add(mapToOrderItem(result));
            }
            return orderItems;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find order items for order: " + orderId, e);
        }
    }

    private OrderItem mapToOrderItem(Result result) {
        OrderItem orderItem = new OrderItem();
        
        result.getValue("order_id").ifPresent(value -> 
            orderItem.setOrderId(value.getAsString().orElse(null)));
        result.getValue("product_id").ifPresent(value -> 
            orderItem.setProductId(value.getAsString().orElse(null)));
        result.getValue("product_name").ifPresent(value -> 
            orderItem.setProductName(value.getAsString().orElse(null)));
        result.getValue("quantity").ifPresent(value -> 
            orderItem.setQuantity(value.getAsInt().orElse(0)));
        result.getValue("unit_price").ifPresent(value -> 
            orderItem.setUnitPrice(value.getAsLong().orElse(0L)));
        result.getValue("total_price").ifPresent(value -> 
            orderItem.setTotalPrice(value.getAsLong().orElse(0L)));
        result.getValue("currency").ifPresent(value -> 
            orderItem.setCurrency(value.getAsString().orElse(null)));
        result.getValue("weight").ifPresent(value -> 
            orderItem.setWeight(value.getAsLong().orElse(0L)));
        result.getValue("created_at").ifPresent(value -> 
            orderItem.setCreatedAt(value.getAsLong().orElse(0L)));
        
        return orderItem;
    }
}