package com.agent.financialadvisor.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;
    private final int remainingTokens;

    public RateLimitExceededException(String message, long retryAfterSeconds, int remainingTokens) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.remainingTokens = remainingTokens;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public int getRemainingTokens() {
        return remainingTokens;
    }
}

