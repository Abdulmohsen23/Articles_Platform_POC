package com.example.first.repository;
import com.example.first.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>
{
    // Find user by email (for login functionality)
    Optional<UserEntity> findByEmail(String email);

    // Check if email exists (for registration validation)
    boolean existsByEmail(String email);

    // Find users by role
    List<UserEntity> findByRole(UserEntity.Role role);

    // Find user by name containing keyword (for search functionality)
    List<UserEntity> findByNameContainingIgnoreCase(String keyword);

    // Find users by role and name containing
    List<UserEntity> findByRoleAndNameContainingIgnoreCase(UserEntity.Role role, String name);

    List<UserEntity> findByRoleOrderByNameAsc(UserEntity.Role role);
}

