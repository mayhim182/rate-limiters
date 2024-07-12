package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class SlidingWindowRateLimiter {
  private final int maxRequest;
  private final long windowSize;
  private final Queue<Long> requestTimeStamps;

  public SlidingWindowRateLimiter(int maxRequest, long windowSize, TimeUnit timeUnit) {
    this.maxRequest = maxRequest;
    this.windowSize = timeUnit.toMillis(windowSize);
    this.requestTimeStamps = new LinkedList<>();
  }

  public synchronized boolean tryAcquire() {
    long now = System.currentTimeMillis();

    //Remove timestamps outside the current window
    while(!requestTimeStamps.isEmpty() && now - requestTimeStamps.peek() > windowSize) {
      requestTimeStamps.poll();
    }

    // Check if the number of requests in the current window is within the limit
    if (requestTimeStamps.size() < maxRequest) {
      requestTimeStamps.add(now);
      return true;
    } else {
      return false; // Request denied, rate limit exceeded
    }
  }

  public synchronized int getCurrentWindowRequestCount() {
    long now = System.currentTimeMillis();

    // Remove timestamps outside the current window
    while (!requestTimeStamps.isEmpty() && now - requestTimeStamps.peek() > windowSize) {
      requestTimeStamps.poll();
    }

    return requestTimeStamps.size();
  }

}
