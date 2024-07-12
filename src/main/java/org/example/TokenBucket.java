package org.example;

import java.util.concurrent.TimeUnit;

public class TokenBucket {
  public static void main(String args[]) throws InterruptedException {
    // CREATE A RATE LIMITER WITH A CAPACITY OF 10 TOKENS
    // REFILLING 1 TOKEN EVERY SECOND
    // Simulate some requests.

    TokenBucketLimiter rateLimiter = new TokenBucketLimiter(10, 1, 1, TimeUnit.SECONDS);

    for (int i = 0; i < 15; i++) {
      if (rateLimiter.tryAcquire()) {
        System.out.println("Request " + (i + 1) + ": allowed");
      } else {
        System.out.println("Request " + (i + 1) + ": denied");
      }

      // Sleep for 100 milliseconds before next request.
      Thread.sleep(100);
    }

    // Simulate a delay to allow tokens to refill.
    Thread.sleep(5000);

    // Check available tokens after refill.
    System.out.println("Available tokens after refill: " + rateLimiter.getAvailableTokens());

  }
}

class TokenBucketLimiter {
  private final long capacity;
  private final long refillTokens;
  private final long refillInterval;
  private long availableTokens;
  private long lastRefillTime;

  public TokenBucketLimiter(long capacity, long refillTokens, long refillInterval
  , TimeUnit timeUnit) {
    this.capacity = capacity;
    this.refillTokens = refillTokens;
    this.refillInterval = timeUnit.toMillis(refillInterval);
    this.availableTokens = capacity;
    this.lastRefillTime = System.currentTimeMillis();
  }

  public synchronized boolean tryAcquire() {
    refill();
    if (this.availableTokens > 0) {
      this.availableTokens--;
      return true; //Request is allowed
    }
    return false; //Request disallowed
  }

  public void refill() {
    long now = System.currentTimeMillis();
    long elasped = now - lastRefillTime; // Getting time elasped after previous refill;

    if(elasped > refillInterval) {
      long tokensToAdd = (elasped / refillInterval) * refillTokens;
      this.availableTokens = Math.min(this.capacity, availableTokens + tokensToAdd);
      this.lastRefillTime = now;
    }
  }

  public synchronized long getAvailableTokens() {
    refill();
    return availableTokens;
  }
}



