package com.askpranav.ai.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Sliding-window limiter: at most {@code maxRequests} per {@code windowSeconds} for each client key
 * (the caller's IP). One question costs several Gemini calls, so this protects the API quota from
 * anyone who finds the public URL. State is in memory: it resets on restart and is per instance,
 * which is fine for a single-instance personal deployment but would need a shared store to scale out.
 */
@Component
public class AskRateLimiter {

    /** Above this many tracked clients, expired entries are swept so the map cannot grow unbounded. */
    private static final int SWEEP_THRESHOLD = 1000;

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clockMillis;
    private final Map<String, Deque<Long>> hitsByClient = new ConcurrentHashMap<>();

    @Autowired
    public AskRateLimiter(@Value("${askpranav.rate-limit.max-requests:2}") int maxRequests,
                          @Value("${askpranav.rate-limit.window-seconds:300}") long windowSeconds) {
        this(maxRequests, windowSeconds, System::currentTimeMillis);
    }

    AskRateLimiter(int maxRequests, long windowSeconds, LongSupplier clockMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.clockMillis = clockMillis;
    }

    /** Records a request for {@code clientKey} if allowed; otherwise says how long until a slot frees up. */
    public Decision tryAcquire(String clientKey) {
        long now = clockMillis.getAsLong();
        if (hitsByClient.size() > SWEEP_THRESHOLD) {
            sweep(now);
        }
        Deque<Long> hits = hitsByClient.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        synchronized (hits) {
            evictExpired(hits, now);
            if (hits.size() >= maxRequests) {
                long retryAfterMillis = hits.peekFirst() + windowMillis - now;
                return new Decision(false, Math.max(1, (retryAfterMillis + 999) / 1000));
            }
            hits.addLast(now);
            return new Decision(true, 0);
        }
    }

    public int maxRequests() {
        return maxRequests;
    }

    public long windowSeconds() {
        return windowMillis / 1000;
    }

    private void evictExpired(Deque<Long> hits, long now) {
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) {
            hits.removeFirst();
        }
    }

    private void sweep(long now) {
        Iterator<Map.Entry<String, Deque<Long>>> it = hitsByClient.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Long> hits = it.next().getValue();
            synchronized (hits) {
                evictExpired(hits, now);
                if (hits.isEmpty()) {
                    it.remove();
                }
            }
        }
    }
}
