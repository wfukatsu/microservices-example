package com.example.scalardb.systemapi.service;

import com.example.scalardb.systemapi.entity.UserEntity;
import com.example.scalardb.systemapi.repository.UserRepository;
import com.scalar.db.exception.storage.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserEntity createUser(String name, String email) throws ExecutionException {
        String id = UUID.randomUUID().toString();
        UserEntity user = new UserEntity(id, name, email, "ACTIVE");
        return userRepository.create(user);
    }

    public Optional<UserEntity> getUserById(String id) throws ExecutionException {
        return userRepository.findById(id);
    }

    public List<UserEntity> getAllUsers() throws ExecutionException {
        return userRepository.findAll();
    }

    public UserEntity updateUser(String id, String name, String email, String status) throws ExecutionException {
        Optional<UserEntity> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            throw new ExecutionException("User not found with id: " + id);
        }

        UserEntity user = existingUser.get();
        if (name != null) {
            user.setName(name);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (status != null) {
            user.setStatus(status);
        }

        return userRepository.update(user);
    }

    public void deleteUser(String id) throws ExecutionException {
        Optional<UserEntity> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            throw new ExecutionException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserEntity activateUser(String id) throws ExecutionException {
        return updateUser(id, null, null, "ACTIVE");
    }

    public UserEntity deactivateUser(String id) throws ExecutionException {
        return updateUser(id, null, null, "INACTIVE");
    }
}