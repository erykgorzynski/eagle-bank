package org.example.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseControllerTest {

    private TestableBaseController baseController;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        baseController = new TestableBaseController();
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserIdReturnsUserIdWhenAuthenticatedWithStringPrincipal() {
        String expectedUserId = "usr-123456789";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(expectedUserId);

        String actualUserId = baseController.getCurrentUserId();

        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        String actualUserId = baseController.getCurrentUserId();

        assertNull(actualUserId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        String actualUserId = baseController.getCurrentUserId();

        assertNull(actualUserId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenPrincipalIsNotString() {
        Object nonStringPrincipal = new Object();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(nonStringPrincipal);

        String actualUserId = baseController.getCurrentUserId();

        assertNull(actualUserId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenPrincipalIsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(null);

        String actualUserId = baseController.getCurrentUserId();

        assertNull(actualUserId);
    }

    @Test
    void getCurrentUserIdReturnsEmptyStringWhenPrincipalIsEmptyString() {
        String emptyUserId = "";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(emptyUserId);

        String actualUserId = baseController.getCurrentUserId();

        assertEquals(emptyUserId, actualUserId);
    }

    @Test
    void getCurrentUserIdHandlesSecurityContextWithoutAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        String actualUserId = baseController.getCurrentUserId();

        assertNull(actualUserId);
        verify(securityContext).getAuthentication();
    }

    private static class TestableBaseController extends BaseController {
        @Override
        public String getCurrentUserId() {
            return super.getCurrentUserId();
        }
    }
}
