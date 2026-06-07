/*
 * Copyright (C) 2025 Prahar Shield
 *
 * Based on LimboFilter, Copyright (C) 2021 - 2025 Elytrium (https://elytrium.net/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.praharshield.filter.ratelimit;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPRateLimiter {

  private static final Logger LOGGER = LoggerFactory.getLogger(IPRateLimiter.class);
  private static final int WINDOW_MS = 5_000;
  private static final int MAX_JOINS_PER_WINDOW = 3;
  private static final ConcurrentHashMap<InetAddress, RateBucket> BUCKETS = new ConcurrentHashMap<>();

  private static volatile List<CidrBlock> trustedCidrBlocks = List.of();

  public static void reloadTrustedSubnets(List<String> cidrs) {
    List<CidrBlock> blocks = new ArrayList<>();
    for (String cidr : cidrs) {
      try {
        blocks.add(CidrBlock.parse(cidr));
      } catch (IllegalArgumentException e) {
        LOGGER.warn("PraharShield IPRateLimiter: invalid trusted subnet '{}', skipping.", cidr);
      }
    }
    trustedCidrBlocks = List.copyOf(blocks);
  }

  public static boolean isTrustedSubnet(InetAddress addr) {
    if (!(addr instanceof Inet4Address)) {
      return false;
    }
    int addrInt = toInt(addr.getAddress());
    for (CidrBlock block : trustedCidrBlocks) {
      if ((addrInt & block.mask) == block.network) {
        return true;
      }
    }
    return false;
  }

  public static synchronized boolean isLimited(InetAddress ip) {
    LOGGER.debug("PraharShield IPRateLimiter: source IP={}", ip.getHostAddress());

    if (isTrustedSubnet(ip)) {
      return false;
    }

    long now = System.currentTimeMillis();
    BUCKETS.entrySet().removeIf(e -> e.getValue().isExpired(now));

    RateBucket bucket = BUCKETS.computeIfAbsent(ip, k -> new RateBucket(now));
    return bucket.incrementAndCheck(now);
  }

  private static int toInt(byte[] addr) {
    return ((addr[0] & 0xFF) << 24) | ((addr[1] & 0xFF) << 16) | ((addr[2] & 0xFF) << 8) | (addr[3] & 0xFF);
  }

  private static class CidrBlock {

    final int network;
    final int mask;

    CidrBlock(int network, int mask) {
      this.network = network;
      this.mask = mask;
    }

    static CidrBlock parse(String cidr) {
      String[] parts = cidr.split("/");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid CIDR: " + cidr);
      }
      try {
        InetAddress addr = InetAddress.getByName(parts[0].trim());
        if (!(addr instanceof Inet4Address)) {
          throw new IllegalArgumentException("Only IPv4 CIDRs supported: " + cidr);
        }
        int prefix = Integer.parseInt(parts[1].trim());
        int mask = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        int network = toInt(addr.getAddress()) & mask;
        return new CidrBlock(network, mask);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException("Invalid CIDR address: " + cidr, e);
      }
    }
  }

  private static class RateBucket {

    private long windowStart;
    private final AtomicInteger count;

    RateBucket(long now) {
      this.windowStart = now;
      this.count = new AtomicInteger(1);
    }

    boolean incrementAndCheck(long now) {
      if (now - this.windowStart > WINDOW_MS) {
        this.windowStart = now;
        this.count.set(1);
        return false;
      }
      return this.count.incrementAndGet() > MAX_JOINS_PER_WINDOW;
    }

    boolean isExpired(long now) {
      return now - this.windowStart > WINDOW_MS + 1000;
    }
  }
}
