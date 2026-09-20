package com.pipeline.crm.auth;

import com.pipeline.crm.common.exception.ForbiddenException;
import com.pipeline.crm.common.exception.UnauthorizedException;
import com.pipeline.crm.config.JwtProperties;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.JwtService;
import com.pipeline.crm.security.LoginRateLimiter;
import com.pipeline.crm.security.SecurityUtils;
import com.pipeline.crm.user.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String REFRESH_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter rateLimiter;

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ip = SecurityUtils.clientIp(httpRequest);
        rateLimiter.checkAllowed(ip, request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);
        if (user == null || user.getDeletedAt() != null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(ip, request.username());
            throw new UnauthorizedException("Invalid credentials");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            rateLimiter.recordFailure(ip, request.username());
            throw new ForbiddenException("Account is blocked");
        }

        rateLimiter.recordSuccess(ip, request.username());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String access = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtService.generateRefreshToken(user.getId());
        setRefreshCookie(response, refresh);

        return new AuthResponse(access, toDto(user));
    }

    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = extractRefreshToken(request);
        if (token == null) {
            throw new ForbiddenException("Missing refresh token");
        }
        JwtService.TokenPayload payload;
        try {
            payload = jwtService.parse(token, "refresh");
        } catch (Exception e) {
            throw new ForbiddenException("Invalid refresh token");
        }

        User user = userRepository.findById(payload.userId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ForbiddenException("Account is blocked");
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefresh = jwtService.generateRefreshToken(user.getId());
        setRefreshCookie(response, newRefresh);

        return new AuthResponse(access, toDto(user));
    }

    public void logout(HttpServletResponse response) {
        clearRefreshCookie(response);
    }

    public AuthUserDto me(CurrentUser user) {
        User entity = userRepository.findById(user.id())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        return toDto(entity);
    }

    public void changePassword(CurrentUser user, ChangePasswordRequest request) {
        User entity = userRepository.findById(user.id())
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), entity.getPasswordHash())) {
            throw new ForbiddenException("Invalid current password");
        }
        entity.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(entity);
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.cookieSecure());
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (jwtProperties.refreshTtlDays() * 24 * 3600));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.cookieSecure());
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private AuthUserDto toDto(User user) {
        return new AuthUserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole(), user.getAvatarColor());
    }

    public record AuthResponse(String accessToken, AuthUserDto user) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }
}
