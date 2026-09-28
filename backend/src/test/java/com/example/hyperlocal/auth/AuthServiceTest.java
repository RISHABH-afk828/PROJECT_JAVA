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
}
