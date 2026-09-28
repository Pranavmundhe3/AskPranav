package com.askpranav.ai.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AskRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AskRateLimiter limiter = new AskRateLimiter(2, 300, now::get);

    @Test
    void allowsTwoQuestionsThenBlocksTheThird() {
        assertThat(limiter.tryAcquire("1.2.3.4").allowed()).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4").allowed()).isTrue();

        AskRateLimiter.Decision third = limiter.tryAcquire("1.2.3.4");

        assertThat(third.allowed()).isFalse();
        assertThat(third.retryAfterSeconds()).isEqualTo(300);
    }

    @Test
    void retryAfterShrinksAsTheWindowAdvances() {
        limiter.tryAcquire("a");
        now.addAndGet(100_000);
        limiter.tryAcquire("a");
        now.addAndGet(50_000);

        AskRateLimiter.Decision blocked = limiter.tryAcquire("a");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(150); // first hit expires 300s after it was made
    }

    @Test
    void allowsAgainOnceTheOldestHitLeavesTheWindow() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        now.addAndGet(300_000);

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void tracksEachClientSeparately() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");

        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }
}
