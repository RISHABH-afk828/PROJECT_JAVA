package com.example.hyperlocal.auth;

import com.example.hyperlocal.auth.dto.AuthResponse;
import com.example.hyperlocal.auth.dto.LoginRequest;
import com.example.hyperlocal.auth.dto.RegisterRequest;
import com.example.hyperlocal.auth.service.AuthService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testRegisterAndLoginFlow() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Test User");
        registerReq.setEmail("testuser@example.com");
        registerReq.setPhone("+919876543210");
        registerReq.setPassword("secretPassword123");
        registerReq.setRole(Role.CUSTOMER);

        AuthResponse registerResponse = authService.register(registerReq);
        assertNotNull(registerResponse.getAccessToken());
        assertNotNull(registerResponse.getRefreshToken());
        assertEquals("testuser@example.com", registerResponse.getUser().getEmail());
        assertEquals(Role.CUSTOMER, registerResponse.getUser().getRole());

        // Test Duplicate Email Conflict
        RegisterRequest duplicateReq = new RegisterRequest();
        duplicateReq.setName("Another User");
        duplicateReq.setEmail("TESTUSER@example.com"); // Case insensitive check
        duplicateReq.setPhone("+919876543211");
        duplicateReq.setPassword("secretPassword123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(duplicateReq));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getCode());

        // Test Login Success
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("testuser@example.com");
        loginReq.setPassword("secretPassword123");

        AuthResponse loginResponse = authService.login(loginReq);
        assertNotNull(loginResponse.getAccessToken());
        assertNotNull(loginResponse.getRefreshToken());
        assertEquals("testuser@example.com", loginResponse.getUser().getEmail());

        // Test Login with Phone
        LoginRequest phoneLoginReq = new LoginRequest();
        phoneLoginReq.setEmail("+919876543210");
        phoneLoginReq.setPassword("secretPassword123");

        AuthResponse phoneLoginResponse = authService.login(phoneLoginReq);
        assertNotNull(phoneLoginResponse.getAccessToken());

        // Test Login Wrong Password
        LoginRequest badLogin = new LoginRequest();
        badLogin.setEmail("testuser@example.com");
        badLogin.setPassword("wrongPassword");

        ApiException badEx = assertThrows(ApiException.class, () -> authService.login(badLogin));
        assertEquals("INVALID_CREDENTIALS", badEx.getCode());
    }

    @Test
    void testAdminRegistrationRejected() {
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Malicious Admin");
        adminReq.setEmail("hacker@example.com");
        adminReq.setPhone("+919999999999");
        adminReq.setPassword("Password123!");
        adminReq.setRole(Role.ADMIN);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(adminReq));
        assertEquals("INVALID_ROLE", ex.getCode());
    }

    @Test
    void testForgotPasswordDoesNotEnumerateUsers() {
        com.example.hyperlocal.auth.dto.ForgotPasswordRequest unknownReq = new com.example.hyperlocal.auth.dto.ForgotPasswordRequest();
        unknownReq.setEmail("nonexistent@example.com");

        assertDoesNotThrow(() -> {
            String token = authService.forgotPassword(unknownReq);
            assertNull(token);
        });
    }

    @Test
    void testResetPasswordRevokesRefreshTokens() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Session User");
        registerReq.setEmail("session@example.com");
        registerReq.setPhone("+919888888888");
        registerReq.setPassword("originalPassword123");
        registerReq.setRole(Role.CUSTOMER);

        AuthResponse auth = authService.register(registerReq);
        String refreshToken = auth.getRefreshToken();

        // Generate forgot password token
        com.example.hyperlocal.auth.dto.ForgotPasswordRequest forgotReq = new com.example.hyperlocal.auth.dto.ForgotPasswordRequest();
        forgotReq.setEmail("session@example.com");
        String resetToken = authService.forgotPassword(forgotReq);
        assertNotNull(resetToken);

        // Reset password
        com.example.hyperlocal.auth.dto.ResetPasswordRequest resetReq = new com.example.hyperlocal.auth.dto.ResetPasswordRequest();
        resetReq.setToken(resetToken);
        resetReq.setNewPassword("newSecretPassword123");
        authService.resetPassword(resetReq);

        // Attempting to refresh with previous refreshToken must now fail
        com.example.hyperlocal.auth.dto.RefreshTokenRequest refreshReq = new com.example.hyperlocal.auth.dto.RefreshTokenRequest();
        refreshReq.setRefreshToken(refreshToken);

        ApiException ex = assertThrows(ApiException.class, () -> authService.refreshToken(refreshReq));
        assertEquals("INVALID_REFRESH_TOKEN", ex.getCode());
    }
}
