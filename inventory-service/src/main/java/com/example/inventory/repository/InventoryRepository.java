package com.example.inventory.repository;

import com.example.inventory.entity.InventoryItem;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepository {
    
    private static final String NAMESPACE = "inventory";
    private static final String TABLE_NAME = "inventory_items";
    
    public Optional<InventoryItem> findById(DistributedTransaction transaction, String productId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("product_id", productId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToInventoryItem);
    }
    
    public List<InventoryItem> findAll(DistributedTransaction transaction) throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<InventoryItem> items = new ArrayList<>();
        for (Result result : results) {
            items.add(mapResultToInventoryItem(result));
        }
        return items;
    }
    
    public List<InventoryItem> findByStatus(DistributedTransaction transaction, String status) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<InventoryItem> items = new ArrayList<>();
        for (Result result : results) {
            InventoryItem item = mapResultToInventoryItem(result);
            if (status.equals(item.getStatus())) {
                items.add(item);
            }
        }
        return items;
    }
    
    public void save(DistributedTransaction transaction, InventoryItem item) throws TransactionException {
        Put put = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("product_id", item.getProductId()))
            .textValue("product_name", item.getProductName())
            .intValue("available_quantity", item.getAvailableQuantity())
            .intValue("reserved_quantity", item.getReservedQuantity())
            .intValue("total_quantity", item.getTotalQuantity())
            .bigIntValue("unit_price", item.getUnitPrice())
            .textValue("currency", item.getCurrency())
            .textValue("status", item.getStatus())
            .bigIntValue("created_at", item.getCreatedAt())
            .bigIntValue("updated_at", item.getUpdatedAt())
            .intValue("version", item.getVersion())
            .build();
        
        transaction.put(put);
    }
    
    public boolean existsById(DistributedTransaction transaction, String productId) 
            throws TransactionException {
        return findById(transaction, productId).isPresent();
    }
    
    public void deleteById(DistributedTransaction transaction, String productId) 
            throws TransactionException {
        // ScalarDBでは明示的なdeleteはないので、statusを更新することで論理削除
        Optional<InventoryItem> itemOpt = findById(transaction, productId);
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            item.setStatus("DELETED");
            item.setUpdatedAt(System.currentTimeMillis());
            save(transaction, item);
        }
    }
    
    public long countLowStockItems(DistributedTransaction transaction, int threshold) 
            throws TransactionException {
        List<InventoryItem> allItems = findAll(transaction);
        return allItems.stream()
            .filter(item -> "ACTIVE".equals(item.getStatus()))
            .filter(item -> item.getAvailableQuantity() <= threshold)
            .count();
    }
    
    private InventoryItem mapResultToInventoryItem(Result result) {
        InventoryItem item = new InventoryItem();
        
        result.getValue("product_id").ifPresent(v -> item.setProductId(((TextValue) v).get()));
        result.getValue("product_name").ifPresent(v -> item.setProductName(((TextValue) v).get()));
        result.getValue("available_quantity").ifPresent(v -> item.setAvailableQuantity(v.getAsInt()));
        result.getValue("reserved_quantity").ifPresent(v -> item.setReservedQuantity(v.getAsInt()));
        result.getValue("total_quantity").ifPresent(v -> item.setTotalQuantity(v.getAsInt()));
        result.getValue("unit_price").ifPresent(v -> item.setUnitPrice(v.getAsLong()));
        result.getValue("currency").ifPresent(v -> item.setCurrency(((TextValue) v).get()));
        result.getValue("status").ifPresent(v -> item.setStatus(((TextValue) v).get()));
        result.getValue("created_at").ifPresent(v -> item.setCreatedAt(v.getAsLong()));
        result.getValue("updated_at").ifPresent(v -> item.setUpdatedAt(v.getAsLong()));
        result.getValue("version").ifPresent(v -> item.setVersion(v.getAsInt()));
        
        return item;
    }
}