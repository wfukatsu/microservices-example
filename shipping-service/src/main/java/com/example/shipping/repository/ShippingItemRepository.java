package com.example.shipping.repository;

import com.example.shipping.entity.ShippingItem;
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
public class ShippingItemRepository {
    
    private static final String NAMESPACE = "shipping";
    private static final String TABLE_NAME = "shipping_items";
    
    public Optional<ShippingItem> findById(DistributedTransaction transaction, String shipmentId, String itemId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.of("shipment_id", shipmentId, "item_id", itemId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToShippingItem);
    }
    
    public List<ShippingItem> findByShipmentId(DistributedTransaction transaction, String shipmentId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<ShippingItem> items = new ArrayList<>();
        for (Result result : results) {
            ShippingItem item = mapResultToShippingItem(result);
            if (shipmentId.equals(item.getShipmentId())) {
                items.add(item);
            }
        }
        return items;
    }
    
    public void save(DistributedTransaction transaction, ShippingItem item) throws TransactionException {
        Put.Builder putBuilder = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.of("shipment_id", item.getShipmentId(), "item_id", item.getItemId()))
            .textValue("product_id", item.getProductId())
            .textValue("product_name", item.getProductName())
            .intValue("quantity", item.getQuantity());
        
        if (item.getWeight() != null) {
            putBuilder.doubleValue("weight", item.getWeight());
        }
        if (item.getDimensions() != null) {
            putBuilder.textValue("dimensions", item.getDimensions());
        }
        if (item.getIsFragile() != null) {
            putBuilder.booleanValue("is_fragile", item.getIsFragile());
        }
        if (item.getIsHazardous() != null) {
            putBuilder.booleanValue("is_hazardous", item.getIsHazardous());
        }
        
        transaction.put(putBuilder.build());
    }
    
    public void saveAll(DistributedTransaction transaction, List<ShippingItem> items) throws TransactionException {
        for (ShippingItem item : items) {
            save(transaction, item);
        }
    }
    
    public boolean existsById(DistributedTransaction transaction, String shipmentId, String itemId) 
            throws TransactionException {
        return findById(transaction, shipmentId, itemId).isPresent();
    }
    
    private ShippingItem mapResultToShippingItem(Result result) {
        ShippingItem item = new ShippingItem();
        
        result.getValue("shipment_id").ifPresent(v -> item.setShipmentId(((TextValue) v).get()));
        result.getValue("item_id").ifPresent(v -> item.setItemId(((TextValue) v).get()));
        result.getValue("product_id").ifPresent(v -> item.setProductId(((TextValue) v).get()));
        result.getValue("product_name").ifPresent(v -> item.setProductName(((TextValue) v).get()));
        result.getValue("quantity").ifPresent(v -> item.setQuantity(v.getAsInt()));
        result.getValue("weight").ifPresent(v -> item.setWeight(v.getAsDouble()));
        result.getValue("dimensions").ifPresent(v -> item.setDimensions(((TextValue) v).get()));
        result.getValue("is_fragile").ifPresent(v -> item.setIsFragile(v.getAsBoolean()));
        result.getValue("is_hazardous").ifPresent(v -> item.setIsHazardous(v.getAsBoolean()));
        
        return item;
    }
}