package com.example.hyperlocal.auth.service;

import com.example.hyperlocal.auth.dto.*;
import com.example.hyperlocal.auth.entity.PasswordResetToken;
import com.example.hyperlocal.auth.entity.RefreshSession;
import com.example.hyperlocal.auth.repository.PasswordResetTokenRepository;
import com.example.hyperlocal.auth.repository.RefreshSessionRepository;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.common.security.JwtUtils;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.entity.UserStatus;
import com.example.hyperlocal.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        String phone = req.getPhone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", "An account with this email already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("PHONE_ALREADY_EXISTS", "An account with this phone number already exists", HttpStatus.CONFLICT);
        }

        Role role = req.getRole() != null ? req.getRole() : Role.CUSTOMER;

        User user = new User(
                req.getName().trim(),
                email,
                phone,
                passwordEncoder.encode(req.getPassword()),
                role
        );
        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String identifier = req.getEmail().trim();
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid email/phone or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ApiException("INVALID_CREDENTIALS", "Invalid email/phone or password", HttpStatus.UNAUTHORIZED);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException("ACCOUNT_SUSPENDED", "Your account has been suspended. Please contact support.", HttpStatus.FORBIDDEN);
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String rawToken = req.getRefreshToken();
        String tokenHash = jwtUtils.hashToken(rawToken);

        RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED));

        if (!session.isActive()) {
            throw new ApiException("TOKEN_EXPIRED", "Refresh token has expired or been revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException("USER_INACTIVE", "User account is not active", HttpStatus.FORBIDDEN);
        }

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getName());
        return new AuthResponse(newAccessToken, rawToken, 900, UserDto.fromEntity(user));
    }

    @Transactional
    public void logout(RefreshTokenRequest req) {
        if (req != null && req.getRefreshToken() != null) {
            String tokenHash = jwtUtils.hashToken(req.getRefreshToken());
            refreshSessionRepository.findByTokenHash(tokenHash).ifPresent(session -> {
                session.setRevokedAt(Instant.now());
                refreshSessionRepository.save(session);
            });
        }
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "No user found with this email", HttpStatus.NOT_FOUND));

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = jwtUtils.hashToken(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(900); // 15 minutes

        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), tokenHash, expiresAt);
        passwordResetTokenRepository.save(resetToken);

        log.info("Generated password reset token for user {}: {}", user.getId(), rawToken);
        return rawToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String tokenHash = jwtUtils.hashToken(req.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException("INVALID_RESET_TOKEN", "Invalid or expired reset token", HttpStatus.BAD_REQUEST));

        if (!resetToken.isValid()) {
            throw new ApiException("RESET_TOKEN_EXPIRED", "Reset token is invalid or has already been used", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getName());
        String refreshToken = jwtUtils.generateRefreshToken();
        String tokenHash = jwtUtils.hashToken(refreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtUtils.getRefreshTokenExpirationMs());
        RefreshSession session = new RefreshSession(user.getId(), tokenHash, expiresAt);
        refreshSessionRepository.save(session);

        return new AuthResponse(accessToken, refreshToken, 900, UserDto.fromEntity(user));
    }
}
