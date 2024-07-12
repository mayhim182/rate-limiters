package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedWindowCount {
  private final int maxRequests;
  private final long windowSize;
  private final Lock lock;
  private long currentWindowStart;
  private int currentWindowRequestCount;

  public FixedWindowCount(int maxRequests, long windowSize, TimeUnit timeUnit) {
    this.maxRequests = maxRequests;
    this.windowSize = windowSize;
    this.lock = new ReentrantLock();
    this.currentWindowStart = System.currentTimeMillis();
    this.currentWindowRequestCount = 0;
  }

  public boolean tryAcquire() {
    long now = System.currentTimeMillis();
    lock.lock();
    try {
      if (now - currentWindowStart >= windowSize) {
        // New Window
        currentWindowStart = now;
        currentWindowRequestCount = 0;
      }
      if (currentWindowRequestCount < maxRequests) {
        currentWindowRequestCount++;
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
    }
  }

  public int getCurrentWindowRequestCount() {
    lock.lock();
    try {
      return currentWindowRequestCount;
    } finally {
      lock.unlock();
    }
  }

  public long getCurrentWindowStart() {
    lock.lock();
    try {
      return currentWindowStart;
    } finally {
      lock.unlock();
    }
  }

}
