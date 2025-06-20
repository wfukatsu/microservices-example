package com.example.scalardb.systemapi.service;

import com.example.scalardb.systemapi.entity.User;
import com.example.scalardb.systemapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUser(String name, String email) {
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with email already exists: " + email);
        }

        String id = UUID.randomUUID().toString();
        User user = new User(id, name, email, "ACTIVE");
        return userRepository.save(user);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }

    public List<User> getUsersByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    public List<User> searchUsersByName(String namePattern) {
        return userRepository.findByNameContaining("%" + namePattern + "%");
    }

    public List<User> getRecentUsers(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return userRepository.findRecentUsers(fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Transactional
    public User updateUser(String id, String name, String email, String status) {
        Optional<User> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }

        User user = existingUser.get();
        if (name != null) {
            user.setName(name);
        }
        if (email != null) {
            // Check if email is already used by another user
            Optional<User> userWithEmail = userRepository.findByEmail(email);
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Email already used by another user: " + email);
            }
            user.setEmail(email);
        }
        if (status != null) {
            user.setStatus(status);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String id) {
        Optional<User> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User activateUser(String id) {
        return updateUser(id, null, null, "ACTIVE");
    }

    @Transactional
    public User deactivateUser(String id) {
        return updateUser(id, null, null, "INACTIVE");
    }

    public long getActiveUserCount() {
        return userRepository.countByStatus("ACTIVE");
    }

    public long getInactiveUserCount() {
        return userRepository.countByStatus("INACTIVE");
    }
}