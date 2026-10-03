package com.example.demo.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final int MAX_ENTRIES = 10000;
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    private long lastCleanupTime = System.currentTimeMillis();

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class AttemptInfo {
        int count;
        long lastAttemptTime;
        long blockedUntil;

        AttemptInfo(int count, long lastAttemptTime, long blockedUntil) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
            this.blockedUntil = blockedUntil;
        }
    }

    public String buildKey(String email, String ip) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanIp = ip != null && !ip.isBlank() ? ip.trim() : "unknown";
        return cleanEmail + "|" + cleanIp;
    }

    public boolean isBlocked(String email, String ip) {
        return isBlocked(buildKey(email, ip));
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        evictIfNecessary();
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;

        long now = System.currentTimeMillis();
        if (now < info.blockedUntil) {
            return true;
        }

        if (now - info.lastAttemptTime > TimeUnit.MINUTES.toMillis(15)) {
            attempts.remove(key);
            return false;
        }

        return false;
    }

    public void recordFailedAttempt(String email, String ip) {
        recordFailedAttempt(buildKey(email, ip));
    }

    public void recordFailedAttempt(String key) {
        if (key == null) return;
        evictIfNecessary();
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, info) -> {
            if (info == null || (now - info.lastAttemptTime > TimeUnit.MINUTES.toMillis(15))) {
                return new AttemptInfo(1, now, 0);
            }
            info.count++;
            info.lastAttemptTime = now;

            // Progressive delay & short lockouts to prevent persistent denial of service
            if (info.count >= 8) {
                info.blockedUntil = now + TimeUnit.MINUTES.toMillis(2); // 2 minute short lockout
            } else if (info.count >= 5) {
                info.blockedUntil = now + TimeUnit.MINUTES.toMillis(1); // 1 minute progressive lockout
            } else if (info.count >= 3) {
                info.blockedUntil = now + TimeUnit.SECONDS.toMillis(30); // 30 second progressive delay
            }
            return info;
        });
    }

    public void clear(String email, String ip) {
        clear(buildKey(email, ip));
    }

    public void clear(String key) {
        if (key != null) {
            attempts.remove(key);
        }
    }

    public void evictExpired() {
        long now = System.currentTimeMillis();
        long expiryThreshold = TimeUnit.MINUTES.toMillis(15);
        attempts.entrySet().removeIf(entry -> {
            AttemptInfo info = entry.getValue();
            return now >= info.blockedUntil && (now - info.lastAttemptTime > expiryThreshold);
        });
    }

    private void evictIfNecessary() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS || attempts.size() > MAX_ENTRIES) {
            lastCleanupTime = now;
            evictExpired();
            if (attempts.size() > MAX_ENTRIES) {
                attempts.clear();
            }
        }
    }
}
