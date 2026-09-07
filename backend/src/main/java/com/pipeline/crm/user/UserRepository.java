package com.pipeline.crm.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.status = :status AND u.deletedAt IS NULL")
    long countByRoleAndStatus(@Param("role") Role role, @Param("status") UserStatus status);
}
