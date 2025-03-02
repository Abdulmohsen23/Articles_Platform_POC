//package com.example.first.config;
//
//import com.example.first.models.UserEntity;
//import com.example.first.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DataLoader implements CommandLineRunner {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        // Check if we need to initialize data
//        if (userRepository.count() == 0) {
//            // Create admin user
//            UserEntity admin = new UserEntity();
//            admin.setName("Admin User");
//            admin.setEmail("admin@example.com");
//            admin.setPassword("admin123");
//            admin.setRole(UserEntity.Role.ADMIN);
//            userRepository.save(admin);
//
//            // Create regular user
//            UserEntity user = new UserEntity();
//            user.setName("Regular User");
//            user.setEmail("user@example.com");
//            user.setPassword("user123");
//            user.setRole(UserEntity.Role.USER);
//            userRepository.save(user);
//
//            System.out.println("Initial data loaded!");
//        }
//    }
//}