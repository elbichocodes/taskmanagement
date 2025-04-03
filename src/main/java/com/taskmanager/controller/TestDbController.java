package com.taskmanager.controller;

import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testdb")
public class TestDbController {

    private final UserRepository userRepository;

    @Autowired // Add this annotation
    public TestDbController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @GetMapping("/insertuser")
    public ResponseEntity<String> insertTestUser() {
        try {
            User testUser = new User();
            testUser.setUsername("testdbuser");
            testUser.setPassword("testdbpassword"); // Password will be encoded by Spring Security later
            testUser.setEmail("testdbuser@example.com");

            userRepository.save(testUser);
            return ResponseEntity.ok("Test user inserted successfully!");
        } catch (Exception e) {
            System.err.println("Error inserting test user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to insert test user.");
        }
    }
}