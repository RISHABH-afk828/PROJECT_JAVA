package com.example.hyperlocal.common.security;

import com.example.hyperlocal.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Check if rate limiting should be skipped (e.g. test bypass header)
        String bypass = request.getHeader("X-RateLimit-Bypass");
        if ("test-suite-bypass".equals(bypass)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);

        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/api/v1/auth/login")) {
                boolean allowed = rateLimiterService.tryAcquire("login:" + ip, 5, Duration.ofMinutes(15));
                if (!allowed) {
                    writeRateLimitError(response, request.getRequestURI(), 900, "Too many login attempts. Please try again in 15 minutes.");
                    return;
                }
            } else if (path.equals("/api/v1/auth/register")) {
                boolean allowed = rateLimiterService.tryAcquire("register:" + ip, 10, Duration.ofHours(1));
                if (!allowed) {
                    writeRateLimitError(response, request.getRequestURI(), 3600, "Too many registration attempts. Please try again later.");
                    return;
                }
            } else if (path.equals("/api/v1/auth/forgot-password")) {
                boolean allowed = rateLimiterService.tryAcquire("forgot:" + ip, 5, Duration.ofHours(1));
                if (!allowed) {
                    writeRateLimitError(response, request.getRequestURI(), 3600, "Too many password reset requests. Please try again later.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitError(HttpServletResponse response, String path, int retryAfterSeconds, String message) throws IOException {
        response.setStatus(429); // 429 Too Many Requests
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ApiResponse<Object> body = ApiResponse.error("RATE_LIMIT_EXCEEDED", message, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
