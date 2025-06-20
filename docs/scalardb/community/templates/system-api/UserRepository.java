package com.example.scalardb.systemapi.repository;

import com.example.scalardb.systemapi.entity.UserEntity;
import com.scalar.db.api.*;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final String NAMESPACE = "user_service";
    private static final String TABLE_NAME = "users";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private DistributedTransactionManager transactionManager;

    public UserEntity create(UserEntity user) throws ExecutionException {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Put put = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("id", user.getId()))
                    .textValue("name", user.getName())
                    .textValue("email", user.getEmail())
                    .textValue("status", user.getStatus())
                    .textValue("created_at", user.getCreatedAt().format(FORMATTER))
                    .textValue("updated_at", user.getUpdatedAt().format(FORMATTER))
                    .build();

            transaction.put(put);
            transaction.commit();
            return user;
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Failed to create user", e);
        }
    }

    public Optional<UserEntity> findById(String id) throws ExecutionException {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Get get = Get.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("id", id))
                    .build();

            Optional<Result> result = transaction.get(get);
            transaction.commit();

            if (result.isPresent()) {
                return Optional.of(mapResultToEntity(result.get()));
            }
            return Optional.empty();
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Failed to find user by id", e);
        }
    }

    public List<UserEntity> findAll() throws ExecutionException {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Scan scan = Scan.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .build();

            List<Result> results = transaction.scan(scan);
            transaction.commit();

            List<UserEntity> users = new ArrayList<>();
            for (Result result : results) {
                users.add(mapResultToEntity(result));
            }
            return users;
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Failed to find all users", e);
        }
    }

    public UserEntity update(UserEntity user) throws ExecutionException {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // First check if user exists
            Get get = Get.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("id", user.getId()))
                    .build();

            Optional<Result> existing = transaction.get(get);
            if (!existing.isPresent()) {
                transaction.abort();
                throw new ExecutionException("User not found for update");
            }

            Put put = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("id", user.getId()))
                    .textValue("name", user.getName())
                    .textValue("email", user.getEmail())
                    .textValue("status", user.getStatus())
                    .textValue("created_at", existing.get().getValue("created_at").get().getAsString().get())
                    .textValue("updated_at", LocalDateTime.now().format(FORMATTER))
                    .build();

            transaction.put(put);
            transaction.commit();
            return user;
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Failed to update user", e);
        }
    }

    public void deleteById(String id) throws ExecutionException {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Delete delete = Delete.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("id", id))
                    .build();

            transaction.delete(delete);
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw new ExecutionException("Failed to delete user", e);
        }
    }

    private UserEntity mapResultToEntity(Result result) {
        UserEntity user = new UserEntity();
        user.setId(result.getValue("id").get().getAsString().get());
        user.setName(result.getValue("name").get().getAsString().get());
        user.setEmail(result.getValue("email").get().getAsString().get());
        user.setStatus(result.getValue("status").get().getAsString().get());
        user.setCreatedAt(LocalDateTime.parse(result.getValue("created_at").get().getAsString().get(), FORMATTER));
        user.setUpdatedAt(LocalDateTime.parse(result.getValue("updated_at").get().getAsString().get(), FORMATTER));
        return user;
    }
}