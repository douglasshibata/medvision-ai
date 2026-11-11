package com.medvision.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_BUCKETS = 10000;

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    private static class BucketEntry {
        final Bucket bucket;
        volatile long lastAccessTime;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String clientIp) {
        BucketEntry entry = buckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket()));
        entry.touch();
        return entry.bucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        Bucket bucket = resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Try again later.\"}");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client) from the comma-separated list
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/health");
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void evictStaleBuckets() {
        long now = System.currentTimeMillis();
        long staleThreshold = 10 * 60 * 1000; // 10 minutes

        Iterator<Map.Entry<String, BucketEntry>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BucketEntry> entry = iterator.next();
            if (now - entry.getValue().lastAccessTime > staleThreshold) {
                iterator.remove();
            }
        }

        // Hard cap: if still too many, remove oldest entries
        if (buckets.size() > MAX_BUCKETS) {
            buckets.entrySet().stream()
                    .sorted((a, b) -> Long.compare(a.getValue().lastAccessTime, b.getValue().lastAccessTime))
                    .limit(buckets.size() - MAX_BUCKETS)
                    .forEach(entry -> buckets.remove(entry.getKey()));
        }
    }
}
