package com.payment.paymentservice.config;

import com.payment.paymentservice.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-caller token-bucket rate limiter for write-heavy endpoints (payment creation).
 * Keyed by authenticated username, falling back to the client IP for unauthenticated calls.
 * In a multi-instance deployment this would move to a distributed store (e.g. Redis);
 * the in-memory map is sufficient for the single-instance dev/demo footprint.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int CAPACITY = 20;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only throttle writes; list/detail reads pass through untouched.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Bucket bucket = buckets.computeIfAbsent(resolveClientKey(request), key -> newBucket());

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded: max " + CAPACITY + " requests per minute. Please retry shortly."
            );
        }
        return true;
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, REFILL_PERIOD)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = StringUtils.hasText(forwardedFor)
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return "ip:" + clientIp;
    }
}
