package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_JWT_TOKEN = "valid.jwt.token";
    private static final String INVALID_JWT_TOKEN = "invalid.jwt.token";
    private static final String USER_ID = "usr-123456789";
    private static final String BEARER_TOKEN = "Bearer " + VALID_JWT_TOKEN;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void successfullyAuthenticatesUserWithValidJwtToken() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic " + VALID_JWT_TOKEN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenUserAlreadyAuthenticated() throws ServletException, IOException {
        Authentication existingAuth = mock(Authentication.class);
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(any(), any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_JWT_TOKEN);
        when(jwtService.extractUserId(INVALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(INVALID_JWT_TOKEN, USER_ID)).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenUserIdIsNull() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(any(), any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void clearsSecurityContextWhenJwtServiceThrowsException() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenThrow(new RuntimeException("JWT parsing error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void allowsPostRequestToUserCreationEndpoint() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("POST");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void allowsPostRequestToLoginEndpoint() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void allowsSwaggerUiEndpoints() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void allowsApiDocsEndpoints() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api-docs/openapi.json");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void allowsSwaggerUiHtmlEndpoint() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui.html");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractUserId(any());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requiresAuthenticationForGetRequestToUserCreationEndpoint() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requiresAuthenticationForGetRequestToLoginEndpoint() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/auth/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesEmptyBearerToken() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtService.extractUserId("")).thenThrow(new RuntimeException("Invalid token format"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).extractUserId("");
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesBearerTokenWithOnlySpaces() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");
        when(jwtService.extractUserId("   ")).thenThrow(new RuntimeException("Invalid token format"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void setsCorrectAuthenticationTokenDetails() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return USER_ID.equals(token.getPrincipal()) &&
                   token.getCredentials() == null &&
                   token.getAuthorities().isEmpty() &&
                   token.getDetails() != null;
        }));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesSecurityContextWithExistingNullAuthentication() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void processesValidTokenWithSpecialCharacters() throws ServletException, IOException {
        String tokenWithSpecialChars = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.special-chars-token";
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenWithSpecialChars);
        when(jwtService.extractUserId(tokenWithSpecialChars)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(tokenWithSpecialChars, USER_ID)).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesTokenValidationReturnsFalse() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/v1/accounts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtService.extractUserId(VALID_JWT_TOKEN)).thenReturn(USER_ID);
        when(jwtService.isTokenValid(VALID_JWT_TOKEN, USER_ID)).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }
}
