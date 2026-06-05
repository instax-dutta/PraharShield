package com.praharshield.filter.ratelimit;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IPRateLimiter {
  private static final int WINDOW_MS = 5_000;
  private static final int MAX_JOINS_PER_WINDOW = 3;
  private static final ConcurrentHashMap<InetAddress, RateBucket> BUCKETS = new ConcurrentHashMap<>();

  public static synchronized boolean isLimited(InetAddress ip) {
    long now = System.currentTimeMillis();
    BUCKETS.entrySet().removeIf(e -> e.getValue().isExpired(now));

    RateBucket bucket = BUCKETS.computeIfAbsent(ip, k -> new RateBucket(now));
    return bucket.incrementAndCheck(now);
  }

  private static class RateBucket {
    private long windowStart;
    private final AtomicInteger count;

    RateBucket(long now) {
      this.windowStart = now;
      this.count = new AtomicInteger(1);
    }

    boolean incrementAndCheck(long now) {
      if (now - windowStart > WINDOW_MS) {
        windowStart = now;
        count.set(1);
        return false;
      }
      return count.incrementAndGet() > MAX_JOINS_PER_WINDOW;
    }

    boolean isExpired(long now) {
      return now - windowStart > WINDOW_MS + 1000;
    }
  }
}
