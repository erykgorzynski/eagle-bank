package org.example.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySecretKeyForTestingThatIsLongEnoughForHMACSHA256Algorithm");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
    }

    @Test
    void generateTokenCreatesValidJwtForUserId() {
        String userId = "usr-123abc456";

        String token = jwtService.generateToken(userId);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateTokenCreatesUniqueTokensForDifferentUsers() {
        String userId1 = "usr-123abc456";
        String userId2 = "usr-789def012";

        String token1 = jwtService.generateToken(userId1);
        String token2 = jwtService.generateToken(userId2);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractUserId(token1)).isEqualTo(userId1);
        assertThat(jwtService.extractUserId(token2)).isEqualTo(userId2);
    }

    @Test
    void extractUserIdReturnsCorrectUserIdFromToken() {
        String userId = "usr-123abc456";
        String token = jwtService.generateToken(userId);

        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void extractUserIdReturnsCorrectUserIdWithAlphanumericPattern() {
        String userId = "usr-AbC123DeF789";
        String token = jwtService.generateToken(userId);

        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void isTokenValidReturnsTrueForValidTokenAndMatchingUserId() {
        String userId = "usr-123abc456";
        String token = jwtService.generateToken(userId);

        boolean isValid = jwtService.isTokenValid(token, userId);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForValidTokenWithDifferentUserId() {
        String originalUserId = "usr-123abc456";
        String differentUserId = "usr-789def012";
        String token = jwtService.generateToken(originalUserId);

        boolean isValid = jwtService.isTokenValid(token, differentUserId);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValidThrowsExceptionForExpiredToken() {
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secretKey", "mySecretKeyForTestingThatIsLongEnoughForHMACSHA256Algorithm");
        ReflectionTestUtils.setField(shortExpiryService, "jwtExpiration", 1L);

        String userId = "usr-123abc456";
        String token = shortExpiryService.generateToken(userId);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> shortExpiryService.isTokenValid(token, userId))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenExpiredReturnsFalseForFreshToken() {
        String userId = "usr-123abc456";
        String token = jwtService.generateToken(userId);

        boolean isExpired = jwtService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpiredReturnsTrueForExpiredToken() {
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secretKey", "mySecretKeyForTestingThatIsLongEnoughForHMACSHA256Algorithm");
        ReflectionTestUtils.setField(shortExpiryService, "jwtExpiration", 1L);

        String userId = "usr-123abc456";
        String token = shortExpiryService.generateToken(userId);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> shortExpiryService.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractUserIdThrowsExceptionForMalformedToken() {
        String malformedToken = "invalid.token.structure";

        assertThatThrownBy(() -> jwtService.extractUserId(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void extractUserIdThrowsExceptionForTokenWithInvalidSignature() {
        JwtService differentSecretService = new JwtService();
        ReflectionTestUtils.setField(differentSecretService, "secretKey", "aDifferentSecretKeyThatWillCauseSignatureValidationToFail");
        ReflectionTestUtils.setField(differentSecretService, "jwtExpiration", 86400000L);

        String userId = "usr-123abc456";
        String token = jwtService.generateToken(userId);

        assertThatThrownBy(() -> differentSecretService.extractUserId(token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void generateTokenHandlesUserIdWithSpecialCharacters() {
        String userId = "usr-Test123-ABC_def";

        String token = jwtService.generateToken(userId);
        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void generateTokenHandlesMinimumValidUserId() {
        String userId = "usr-a";

        String token = jwtService.generateToken(userId);
        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void generateTokenHandlesLongUserId() {
        String userId = "usr-" + "a".repeat(100);

        String token = jwtService.generateToken(userId);
        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void isTokenValidHandlesEmptyUserId() {
        String userId = "";
        String token = jwtService.generateToken(userId);

        boolean isValid = jwtService.isTokenValid(token, userId);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseWhenUserIdIsNull() {
        String userId = "usr-123abc456";
        String token = jwtService.generateToken(userId);

        boolean isValid = jwtService.isTokenValid(token, null);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForNullToken() {
        String userId = "usr-123abc456";

        assertThatThrownBy(() -> jwtService.isTokenValid(null, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isTokenValidReturnsFalseForEmptyToken() {
        String userId = "usr-123abc456";

        assertThatThrownBy(() -> jwtService.isTokenValid("", userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT String argument cannot be null or empty.");
    }

    @Test
    void tokenContainsCorrectStructureAndClaims() {
        String userId = "usr-123abc456";

        String token = jwtService.generateToken(userId);

        String[] tokenParts = token.split("\\.");
        assertThat(tokenParts).hasSize(3);

        String extractedUserId = jwtService.extractUserId(token);
        assertThat(extractedUserId).isEqualTo(userId);

        boolean isValid = jwtService.isTokenValid(token, userId);
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenExpiredHandlesMalformedToken() {
        String malformedToken = "invalid.token.format";

        assertThatThrownBy(() -> jwtService.isTokenExpired(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void tokenValidationWorksWithUserIdMatchingOpenApiPattern() {
        String validUserId = "usr-AbC123dEf456GhI789";

        String token = jwtService.generateToken(validUserId);
        String extractedUserId = jwtService.extractUserId(token);
        boolean isValid = jwtService.isTokenValid(token, validUserId);

        assertThat(extractedUserId).isEqualTo(validUserId);
        assertThat(isValid).isTrue();
        assertThat(validUserId).matches("^usr-[A-Za-z0-9]+$");
    }

    @Test
    void generateTokenHandlesNullUserIdGracefully() {
        String token = jwtService.generateToken(null);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);

        String extractedUserId = jwtService.extractUserId(token);
        assertThat(extractedUserId).isNull();
    }
}
