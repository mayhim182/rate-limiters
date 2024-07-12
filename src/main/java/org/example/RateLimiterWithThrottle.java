package org.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.concurrent.TimeUnit;

public class RateLimiterWithThrottle {
  public static void main(String[] args) {
//    Simple Rate Limiter
    RateLimiter rateLimiter = new RateLimiter(10, 1, 1, TimeUnit.SECONDS);

//    Throttled Rate Limiter
    ThrottledRateLimiter throttledRateLimiter = new ThrottledRateLimiter(10, 1, 1, TimeUnit.SECONDS, 500);

//    Distributed Rate Limiter with Redis
//    JedisPool jedisPool = new JedisPool("localhost", 6379);
//    DistributedRateLimiter distributedRateLimiter = new DistributedRateLimiter(jedisPool, "rate_limiter_key", 10, 1, 1, TimeUnit.SECONDS, 500);

    // Testing
    for (int i = 0; i < 15; i++) {
      System.out.println("RateLimiter: " + rateLimiter.tryAcquire());
      System.out.println("ThrottledRateLimiter: " + throttledRateLimiter.tryAcquire());
//      System.out.println("DistributedRateLimiter: " + distributedRateLimiter.tryAcquire());
    }
  }
}


//Writing a custom rateLimiter using Java

class RateLimiter {
  private final long maxTokens;
  private final long refillTokens;
  private final long refillInterval;
  private long availableTokens;
  private long lastRefillTime;

  public RateLimiter(long maxTokens, long refillTokens, long refillInterval, TimeUnit timeUnit) {
    this.maxTokens = maxTokens;
    this.refillTokens = refillTokens;
    this.refillInterval = refillInterval;
    this.availableTokens = maxTokens;
    this.lastRefillTime = System.currentTimeMillis();
  }

  public synchronized boolean tryAcquire() {
    refill();
    if (this.availableTokens > 0) {
      this.availableTokens--;
      return true;
    }
    return false;
  }

  private void refill() {
    long now = System.currentTimeMillis();
    long elasped = now - lastRefillTime;

    if (elasped > refillInterval) {
      long tokensToAdd = (elasped / refillInterval) * refillTokens;
      availableTokens = Math.min(maxTokens, availableTokens+tokensToAdd);
      lastRefillTime = now;
    }
  }
}


//Throttling
//Throttling can be added to slow down requests if the rate limit is exceeded.
//This can be done using Thread.sleep

class ThrottledRateLimiter extends RateLimiter {
  private final long throttleTime;

  public ThrottledRateLimiter(long maxTokens, long refillTokens, long refillInterval, TimeUnit timeUnit, long throttleTime) {
    super(maxTokens, refillTokens, refillInterval, timeUnit);
    this.throttleTime = throttleTime;
  }

  @Override
  public synchronized boolean tryAcquire() {
    boolean acquired= super.tryAcquire();
    if(!acquired) {
      try {
        Thread.sleep(throttleTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return acquired;
  }
}

//Scalability
//To Scale the rate Limiter, you can use a distributed approach. One common method
//is to use Redis for maintaining the rate limit counters across multiple instances
// USING REDIS FOR SCALABILITY
class DistributedRateLimiter {
  private final JedisPool jedisPool;
  private final String key;
  private final long maxTokens;
  private final long refillTokens;
  private final long refillInterval;
  private final long throttleTime;

  public DistributedRateLimiter(JedisPool jedisPool, String key, long maxTokens, long refillTokens,
                                long refillInterval,
                                TimeUnit timeUnit, long throttleTime) {
    this.jedisPool = jedisPool;
    this.key = key;
    this.maxTokens = maxTokens;
    this.refillTokens = refillTokens;
    this.refillInterval = refillInterval;
    this.throttleTime = timeUnit.toMillis(throttleTime);
  }

  public boolean tryAcquire() {
    try (Jedis jedis = jedisPool.getResource()) {
      long now = System.currentTimeMillis();
      String script = "local tokens = redis.call('get', KEYS[1]) " +
        " if token == false then " +
        " redis.call('set', KEYS[1], ARGV[1]) " +
        " return ARGV[1] " +
        "else " +
        "tokens = tonumber(tokens)" +
        "if tokens > 0 then " +
        "redis.call('decr', KEYS[1])" +
        "return tokens - 1" +
        "else return -1" +
        "end" +
        "end";

      Object result = jedis.eval(script, 1, key, String.valueOf(maxTokens));
      if (result.equals(-1L)){
        try{
          Thread.sleep(throttleTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return false;
      }
      return true;
    }
  }
}
