package com.taskmanager.controller;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.taskmanager.entity.User;
import com.taskmanager.payload.request.LoginRequest;
import com.taskmanager.payload.request.RegisterRequest;
import com.taskmanager.payload.response.ErrorResponse;
import com.taskmanager.payload.response.JwtResponse;
import com.taskmanager.repository.RoleRepository;
import com.taskmanager.security.jwt.JwtUtil;
import com.taskmanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // In-memory storage for failed login attempts: <email, failedAttemptsCount>
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    // Maximum number of allowed failed login attempts before prompting password reset
    private static final int MAX_FAILED_ATTEMPTS = 6;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                          JwtUtil jwtUtil, UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, BindingResult result, HttpServletRequest request) {
        // Check for validation errors in the login request
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body(new ErrorResponse("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input")));
        }

        String email = loginRequest.getEmail();

        // Check if the user has exceeded the maximum number of failed login attempts
        if (failedLoginAttempts.containsKey(email) && failedLoginAttempts.get(email) >= MAX_FAILED_ATTEMPTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many failed login attempts. Please use 'Forgot your password' functionality."));
        }

        try {
            // Attempt to authenticate the user using the provided email and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            // If authentication is successful, load user details and generate a JWT token
            final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            final String token = jwtUtil.generateToken(userDetails);

            // Clear failed login attempts upon successful login
            failedLoginAttempts.remove(email);

            // Return the JWT token in the response
            return ResponseEntity.ok(new JwtResponse(token));

        } catch (BadCredentialsException e) {
            // If authentication fails due to incorrect credentials, increment the failed attempts count
            failedLoginAttempts.put(email, failedLoginAttempts.getOrDefault(email, 0) + 1);

            // Return an Unauthorized response with a specific error message
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid email or password"));
        } catch (AuthenticationException e) {
            // Handle other authentication-related exceptions
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Authentication failed"));
        }
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest, BindingResult result) {
        // Check for validation errors in the registration request
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input"));
        }

        // Check if a user with the provided email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already in use!");
        }

        // Check if a user with the provided username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Username is already taken!");
        }

        try {
            // Create a new User entity
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            // Encode the password before saving it to the database
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

            // Handle user roles
            List<Map<String, ?>> rolesData = registerRequest.getRoles();
            Set<com.taskmanager.entity.Role> userRoles = new HashSet<>();

            if (rolesData != null) {
                for (Map<String, ?> roleInfo : rolesData) {
                    if (roleInfo.containsKey("name")) {
                        String roleName = (String) roleInfo.get("name");
                        roleRepository.findByName(roleName)
                                .ifPresent(userRoles::add);
                    }
                    if (roleInfo.containsKey("id")) {
                        try {
                            Long roleId = Long.parseLong(roleInfo.get("id").toString());
                            roleRepository.findById(roleId)
                                    .ifPresent(userRoles::add);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid role ID format: " + roleInfo.get("id"));
                        }
                    }
                }
            }
            user.setRoles(userRoles);

            // Save the new user to the database
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
        } catch (Exception e) {
            System.err.println("Error during user registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to register user. Please try again.");
        }
    }

    // Inner class for structuring error responses as JSON
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}