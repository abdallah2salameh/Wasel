package com.wasel.backend.integration;

import com.wasel.backend.common.RateLimitException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FixedWindowRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void acquire(String key, int maxRequestsPerMinute) {
        Window window = windows.computeIfAbsent(key, ignored -> new Window(Instant.now().plus(1, ChronoUnit.MINUTES)));
        synchronized (window) {
            if (Instant.now().isAfter(window.resetAt())) {
                window.counter().set(0);
                window.setResetAt(Instant.now().plus(1, ChronoUnit.MINUTES));
            }
            if (window.counter().incrementAndGet() > maxRequestsPerMinute) {
                throw new RateLimitException("Outbound integration rate limit exceeded for " + key);
            }
        }
    }

    private static final class Window {
        private final AtomicInteger counter = new AtomicInteger();
        private Instant resetAt;

        private Window(Instant resetAt) {
            this.resetAt = resetAt;
        }

        public AtomicInteger counter() {
            return counter;
        }

        public Instant resetAt() {
            return resetAt;
        }

        public void setResetAt(Instant resetAt) {
            this.resetAt = resetAt;
        }
    }
}
