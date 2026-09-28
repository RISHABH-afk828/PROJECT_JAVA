package com.example.hyperlocal.common.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    private static class RequestWindow {
        private final ConcurrentLinkedDeque<Instant> timestamps = new ConcurrentLinkedDeque<>();
    }

    private final ConcurrentHashMap<String, RequestWindow> tracker = new ConcurrentHashMap<>();

    /**
     * Checks if a request identified by key is allowed under the rate limit.
     *
     * @param key unique identifier (e.g. "login:127.0.0.1")
     * @param maxRequests maximum allowed requests within the time window
     * @param window duration of the sliding window
     * @return true if request is permitted, false if rate limit is exceeded
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        RequestWindow clientWindow = tracker.computeIfAbsent(key, k -> new RequestWindow());

        // Remove expired timestamps from the front of the deque
        while (!clientWindow.timestamps.isEmpty()) {
            Instant oldest = clientWindow.timestamps.peekFirst();
            if (oldest != null && oldest.isBefore(cutoff)) {
                clientWindow.timestamps.pollFirst();
            } else {
                break;
            }
        }

        synchronized (clientWindow) {
            // Re-check size inside lock for precision
            while (!clientWindow.timestamps.isEmpty()) {
                Instant oldest = clientWindow.timestamps.peekFirst();
                if (oldest != null && oldest.isBefore(cutoff)) {
                    clientWindow.timestamps.pollFirst();
                } else {
                    break;
                }
            }

            if (clientWindow.timestamps.size() < maxRequests) {
                clientWindow.timestamps.addLast(now);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Clear all rate limits (useful for testing).
     */
    public void reset() {
        tracker.clear();
    }
}
