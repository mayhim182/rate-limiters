package org.example;

import java.util.Deque;
import java.util.LinkedList;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    //Implementing rate limiter using fixed window approach
    RateLimiter1 rateLimiter1 = new RateLimiter1(5, 4000);

    for(int i = 0; i < 10;i++) {
      if (rateLimiter1.isAllowed()) {
        System.out.println("Request allowed at " + System.currentTimeMillis());
      } else {
        System.out.println("Request denied at " + System.currentTimeMillis());
      }
      try {
        Thread.sleep(500);  // Simulate time between requests
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}

//RATE LIMITER BASED ON TIME WINDOW

class RateLimiter1 {
  private final int maxRequest;
  private final long windowMillis;
  private final Deque<Long> requestTimeStamps;

  public RateLimiter1(int maxRequest, long windowMillis) {
    this.maxRequest = maxRequest;
    this.windowMillis = windowMillis;
    this.requestTimeStamps = new LinkedList<>();
  }

  public synchronized boolean isAllowed() {
    long currentTime = System.currentTimeMillis();

    //Remove timestamp outside the current window
    while(!requestTimeStamps.isEmpty() && requestTimeStamps.peekFirst() <= currentTime - windowMillis) {
      requestTimeStamps.pollFirst();
    }

    if(requestTimeStamps.size() < maxRequest) {
      requestTimeStamps.addLast(currentTime);
      return true;
    }
    else {
      return false;
    }
  }

}

