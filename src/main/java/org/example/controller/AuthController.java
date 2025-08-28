package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.AuthApi;
import org.example.entity.User;
import org.example.model.LoginUser200Response;
import org.example.model.LoginUserRequest;
import org.example.security.JwtService;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication Controller implementing the generated AuthApi interface
 * Handles user authentication and JWT token generation
 */
@RestController
@RequestMapping("/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AuthController implements AuthApi {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    public ResponseEntity<LoginUser200Response> loginUser(@Valid LoginUserRequest loginUserRequest) {
        log.info("Login attempt for email: {}", loginUserRequest.getEmail());

        try {
            // Validate input data
            if (!isValidEmail(loginUserRequest.getEmail())) {
                log.warn("Invalid email format: {}", loginUserRequest.getEmail());
                return ResponseEntity.badRequest().build();
            }

            if (!isValidPassword(loginUserRequest.getPassword())) {
                log.warn("Invalid password format for email: {}", loginUserRequest.getEmail());
                return ResponseEntity.badRequest().build();
            }

            // Authenticate user
            AuthenticationResult authResult = authenticateUser(
                loginUserRequest.getEmail(),
                loginUserRequest.getPassword()
            );

            if (!authResult.isAuthenticated()) {
                log.warn("Authentication failed for email: {}", loginUserRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Generate JWT token
            String jwtToken = jwtService.generateToken(authResult.getUserId());

            // Create successful response
            LoginUser200Response response = new LoginUser200Response();
            response.setToken(jwtToken);
            response.setUserId(authResult.getUserId());

            log.info("Successfully authenticated user: {}", authResult.getUserId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error during authentication: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate password requirements
     */
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Authenticate user using UserService
     */
    private AuthenticationResult authenticateUser(String email, String password) {
        // Real authentication using UserService
        try {
            Optional<User> userOptional = userService.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Verify password using passwordEncoder
                if (passwordEncoder.matches(password, user.getPasswordHash())) {
                    log.info("Authentication successful for user: {}", user.getId());
                    return new AuthenticationResult(true, user.getId(), "Authentication successful");
                }
            }

            log.warn("Authentication failed for email: {}", email);
            return new AuthenticationResult(false, null, "Invalid credentials");

        } catch (Exception e) {
            log.error("Error during authentication for email {}: {}", email, e.getMessage(), e);
            return new AuthenticationResult(false, null, "Authentication error");
        }
    }

    /**
     * Inner class to hold authentication results
     */
    private static class AuthenticationResult {
        private final boolean authenticated;
        private final String userId;
        private final String message;

        public AuthenticationResult(boolean authenticated, String userId, String message) {
            this.authenticated = authenticated;
            this.userId = userId;
            this.message = message;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public String getUserId() {
            return userId;
        }

        public String getMessage() {
            return message;
        }
    }
}
