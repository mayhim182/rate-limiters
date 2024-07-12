package org.example;


import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Writing code for leaking Bucket Rate Limiter
public class LeakingBucket {
  private final int capacity;
  private final int leakRate; // Number of requests to process per unit time
  private final long leakInterval; // Interval between leaks in milliSeconds

  private final Queue<Long> bucket;
  private final ScheduledExecutorService scheduledExecutorService;

  public LeakingBucket(int capacity, int leakRate, long leakInterval,
                                TimeUnit timeUnit) {
    this.capacity = capacity;
    this.leakRate = leakRate;
    this.leakInterval = timeUnit.toMillis(leakInterval);
    this.bucket = new LinkedList<>();
    this.scheduledExecutorService = Executors.newScheduledThreadPool(1);

    //Schedule the leaking process
    this.scheduledExecutorService.scheduleAtFixedRate(this::leak, this.leakInterval,
      this.leakInterval, TimeUnit.MILLISECONDS);
  }

  public synchronized boolean tryAcquire() {
    long currentTime = System.currentTimeMillis();
    if(bucket.size() < capacity) {
      bucket.add(currentTime);
      return true;
    }
    return false; //Bucket overflow, request is denied
  }

  private synchronized void leak() {
    for(int i=0; i<leakRate && !bucket.isEmpty();i++) {
      bucket.poll(); //Process the request; or remove the element at head in context of JAVA
    }
  }

  public synchronized int getBucketSize() {
    return bucket.size();
  }

  public void stop() {
    scheduledExecutorService.shutdown();
  }

  public static void main(String [] args) throws InterruptedException {
    //  Creating a leaky bucket rate limiter with a capacity of 10,
    //  leak rate of 2 requests per second
    LeakingBucket leakingBucket = new LeakingBucket(10, 2, 1, TimeUnit.SECONDS);

    //Simulate some requests
    for (int i = 0; i < 15; i++) {
      if (leakingBucket.tryAcquire()) {
        System.out.println("Request "+ (i+1) + ": allowed");
      } else {
        System.out.println("Request "+(i+1)+ ": denied");
      }

      //Sleep for 100 milliseconds before next request
      Thread.sleep(100);
    }

    // Simulate a delay to allow some requests to be processed
    Thread.sleep(5000);
    // Check the bucket size
    System.out.println("Bucket size after processing: " + leakingBucket.getBucketSize());

    // Stop the rate limiter
    leakingBucket.stop();
  }
}


