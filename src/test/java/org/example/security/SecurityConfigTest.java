package org.example.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null);

    @Test
    void passwordEncoderCreatesBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoderEncodesPasswordsCorrectly() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";

        String encodedPassword = encoder.encode(rawPassword);

        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderGeneratesDifferentHashesForSamePassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "samePassword";

        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches(password, hash1)).isTrue();
        assertThat(encoder.matches(password, hash2)).isTrue();
    }

    @Test
    void passwordEncoderRejectsIncorrectPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";

        String encodedPassword = encoder.encode(correctPassword);

        assertThat(encoder.matches(wrongPassword, encodedPassword)).isFalse();
    }

    @Test
    void passwordEncoderHandlesEmptyPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String emptyPassword = "";

        String encodedPassword = encoder.encode(emptyPassword);

        assertThat(encodedPassword).isNotEmpty();
        assertThat(encoder.matches(emptyPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderHandlesSpecialCharacters() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String passwordWithSpecialChars = "P@ssw0rd!@#$%^&*()";

        String encodedPassword = encoder.encode(passwordWithSpecialChars);

        assertThat(encoder.matches(passwordWithSpecialChars, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderHandlesUnicodeCharacters() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String unicodePassword = "пароль123密码";

        String encodedPassword = encoder.encode(unicodePassword);

        assertThat(encoder.matches(unicodePassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderHandlesLongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String longPassword = "a".repeat(1000);

        String encodedPassword = encoder.encode(longPassword);

        assertThat(encoder.matches(longPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoderProducesConsistentBCryptFormat() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "testPassword";

        String encodedPassword = encoder.encode(password);

        assertThat(encodedPassword).startsWith("$2a$");
        assertThat(encodedPassword.length()).isGreaterThan(50);
    }

    @Test
    void passwordEncoderThrowsExceptionForNullPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThatThrownBy(() -> encoder.encode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rawPassword cannot be null");
    }

    @Test
    void passwordEncoderValidatesStrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String strongPassword = "StrongP@ssw0rd123!";

        String encodedPassword = encoder.encode(strongPassword);

        assertThat(encoder.matches(strongPassword, encodedPassword)).isTrue();
        assertThat(encodedPassword).startsWith("$2a$");
    }

    @Test
    void passwordEncoderHandlesMinimumLengthPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String minimumPassword = "Pass123!";

        String encodedPassword = encoder.encode(minimumPassword);

        assertThat(encoder.matches(minimumPassword, encodedPassword)).isTrue();
    }
}
