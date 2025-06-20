package com.example.scalardb.systemapi.repository;

import com.example.scalardb.systemapi.entity.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

    @Query("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT * FROM users WHERE status = :status")
    List<User> findByStatus(@Param("status") String status);

    @Query("SELECT * FROM users WHERE name LIKE :namePattern")
    List<User> findByNameContaining(@Param("namePattern") String namePattern);

    @Query("SELECT COUNT(*) FROM users WHERE status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT * FROM users WHERE created_at >= :fromDate ORDER BY created_at DESC")
    List<User> findRecentUsers(@Param("fromDate") String fromDate);
}