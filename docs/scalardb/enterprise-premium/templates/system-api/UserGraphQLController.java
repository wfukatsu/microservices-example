package com.example.scalardb.systemapi.controller;

import com.example.scalardb.systemapi.dto.CreateUserRequest;
import com.example.scalardb.systemapi.dto.UpdateUserRequest;
import com.example.scalardb.systemapi.dto.UserResponse;
import com.example.scalardb.systemapi.entity.User;
import com.example.scalardb.systemapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UserGraphQLController {

    @Autowired
    private UserService userService;

    @QueryMapping
    public UserResponse userById(@Argument String id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            return mapToResponse(user.get());
        }
        return null;
    }

    @QueryMapping
    public UserResponse userByEmail(@Argument String email) {
        Optional<User> user = userService.getUserByEmail(email);
        if (user.isPresent()) {
            return mapToResponse(user.get());
        }
        return null;
    }

    @QueryMapping
    public List<UserResponse> allUsers() {
        return userService.getAllUsers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<UserResponse> usersByStatus(@Argument String status) {
        return userService.getUsersByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<UserResponse> searchUsers(@Argument String namePattern) {
        return userService.searchUsersByName(namePattern).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<UserResponse> recentUsers(@Argument Integer days) {
        int daysToSearch = days != null ? days : 7;
        return userService.getRecentUsers(daysToSearch).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public Long activeUserCount() {
        return userService.getActiveUserCount();
    }

    @QueryMapping
    public Long inactiveUserCount() {
        return userService.getInactiveUserCount();
    }

    @MutationMapping
    public UserResponse createUser(@Argument CreateUserRequest input) {
        User user = userService.createUser(input.getName(), input.getEmail());
        return mapToResponse(user);
    }

    @MutationMapping
    public UserResponse updateUser(@Argument String id, @Argument UpdateUserRequest input) {
        User user = userService.updateUser(id, input.getName(), input.getEmail(), input.getStatus());
        return mapToResponse(user);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument String id) {
        try {
            userService.deleteUser(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @MutationMapping
    public UserResponse activateUser(@Argument String id) {
        User user = userService.activateUser(id);
        return mapToResponse(user);
    }

    @MutationMapping
    public UserResponse deactivateUser(@Argument String id) {
        User user = userService.deactivateUser(id);
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}