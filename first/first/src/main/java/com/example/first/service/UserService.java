package com.example.first.service;

import com.example.first.models.UserEntity;
import com.example.first.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService
{
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserEntity createUser(UserEntity user) {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // If no role is selected, default to USER
        if (user.getRole() == null) {
            user.setRole(UserEntity.Role.USER);
        }

        return userRepository.save(user);
    }

    // Get user by ID
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get user by email
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Update user information
    @Transactional
    public UserEntity updateUser(UserEntity user) {
        Optional<UserEntity> existingUser = userRepository.findById(user.getId());

        if (existingUser.isPresent()) {
            // Check if email is being changed and if new email already exists
            if (!existingUser.get().getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email already in use");
            }

            return userRepository.save(user);
        }

        throw new RuntimeException("User not found");
    }

    // Update user role (admin only)
    @Transactional
    public UserEntity updateUserRole(Long userId, UserEntity.Role newRole, UserEntity admin) {
        if (admin.getRole() != UserEntity.Role.ADMIN) {
            throw new RuntimeException("Only admins can change user roles");
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setRole(newRole);
            return userRepository.save(user);
        }

        throw new RuntimeException("User not found");
    }

    // Get all users
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    // Get all admins
    public List<UserEntity> getAllAdmins() {
        return userRepository.findByRole(UserEntity.Role.ADMIN);
    }

    // Get users by role
    public List<UserEntity> getUsersByRole(UserEntity.Role role) {
        return userRepository.findByRole(role);
    }

    // Check if user is admin
    public boolean isAdmin(UserEntity user) {
        return user != null && user.getRole() == UserEntity.Role.ADMIN;
    }

    // Delete user
    @Transactional
    public void deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new RuntimeException("User not found");
        }
    }
}
