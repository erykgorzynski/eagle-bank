package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.AuthApi;
import org.example.entity.User;
import org.example.model.LoginResponse;
import org.example.model.LoginUserRequest;
import org.example.security.JwtService;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Authentication Controller implementing the generated AuthApi interface
 * Handles user authentication and JWT token generation according to API specification
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AuthController implements AuthApi {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public ResponseEntity<LoginResponse> loginUser(LoginUserRequest loginUserRequest) {
        log.info("Login attempt for email: {}", loginUserRequest.getEmail());

        User authenticatedUser = authenticateUser(loginUserRequest.getEmail(), loginUserRequest.getPassword());

        String jwtToken = jwtService.generateToken(authenticatedUser.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(jwtToken);
        response.setUserId(authenticatedUser.getId());

        log.info("Successfully authenticated user: {}", authenticatedUser.getId());
        return ResponseEntity.ok(response);
    }

    private User authenticateUser(String email, String password) {
        try {
            Optional<User> userOptional = userService.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                if (passwordEncoder.matches(password, user.getPasswordHash())) {
                    log.info("Authentication successful for user: {}", user.getId());
                    return user;
                }
            }

            log.warn("Authentication failed for email: {}", email);
            throw new BadCredentialsException("Invalid email or password");

        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                throw e;
            }

            log.error("Error during authentication for email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Authentication error", e);
        }
    }
}
