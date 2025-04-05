package com.taskmanager.controller;

import com.taskmanager.entity.PasswordResetToken;
import com.taskmanager.entity.User;
import com.taskmanager.payload.request.ForgotPasswordRequest;
import com.taskmanager.payload.request.LoginRequest;
import com.taskmanager.payload.request.RegisterRequest;
import com.taskmanager.payload.request.ResetPasswordRequest;
import com.taskmanager.payload.response.ErrorResponse;
import com.taskmanager.payload.response.JwtResponse;
import com.taskmanager.repository.PasswordResetTokenRepository;
import com.taskmanager.repository.RoleRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.jwt.JwtUtil;
import com.taskmanager.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 6;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                          JwtUtil jwtUtil, UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository,
                          EmailService emailService, @Value("${app.frontend.url}") String frontendUrl) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body(new ErrorResponse("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input")));
        }

        String email = loginRequest.getEmail();

        if (failedLoginAttempts.containsKey(email) && failedLoginAttempts.get(email) >= MAX_FAILED_ATTEMPTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many failed login attempts. Please use 'Forgot your password' functionality."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            final String token = jwtUtil.generateToken(userDetails);

            failedLoginAttempts.remove(email);

            return ResponseEntity.ok(new JwtResponse(token));

        } catch (BadCredentialsException e) {
            failedLoginAttempts.put(email, failedLoginAttempts.getOrDefault(email, 0) + 1);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid email or password"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Authentication failed"));
        }
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest, BindingResult result) {
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input"));
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already in use!");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Username is already taken!");
        }

        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

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
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
        } catch (Exception e) {
            System.err.println("Error during user registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to register user. Please try again.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, BindingResult result) {
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body(new ErrorResponse("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input")));
        }

        String email = forgotPasswordRequest.getEmail().trim();
        System.out.println("Received email for password reset: \"" + email + "\"");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(new ErrorResponse("If an account with that email exists, a password reset link has been sent."));
        }

        PasswordResetToken existingToken = passwordResetTokenRepository.findByUserAndExpiryDateAfter(user, LocalDateTime.now()).orElse(null);

        if (existingToken != null) {
            System.out.println("Found an existing active reset token for user: " + user.getEmail() + ". Invalidating it.");
            passwordResetTokenRepository.delete(existingToken);
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        // Apply the change here:
        System.out.println("Raw frontendUrl from config: \"" + frontendUrl + "\"");
        String cleanFrontendUrl = frontendUrl;
        if (cleanFrontendUrl.endsWith("/")) {
            cleanFrontendUrl = cleanFrontendUrl.substring(0, cleanFrontendUrl.length() - 1);
        }
        System.out.println("Cleaned frontendUrl: \"" + cleanFrontendUrl + "\"");

        String resetLink = cleanFrontendUrl + "/reset-password/" + token;
        System.out.println("Generated resetLink: \"" + resetLink + "\"");

        emailService.sendPasswordResetEmail(email, token);

        return ResponseEntity.ok(new ErrorResponse("If an account with that email exists, a password reset link has been sent. Please check your inbox (and spam folder)."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, BindingResult result) {
        if (result.hasErrors()) {
            FieldError error = result.getFieldError();
            return ResponseEntity.badRequest().body(new ErrorResponse("Validation error: " + (error != null ? error.getDefaultMessage() : "Invalid input")));
        }

        String token = resetPasswordRequest.getToken();
        String newPassword = resetPasswordRequest.getPassword();

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Invalid or expired password reset token."));
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(new ErrorResponse("Your password has been reset successfully. You can now log in with your new password."));
    }

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