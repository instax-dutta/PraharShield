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

package com.praharshield.filter.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookBatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebhookBatcher.class);
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  private final ConcurrentLinkedQueue<Map<String, Object>> queue = new ConcurrentLinkedQueue<>();
  private final String url;
  private final int batchSize;
  private final int queueCap;
  private final AtomicBoolean warnedOnce = new AtomicBoolean(false);

  public WebhookBatcher(String url, int batchSize, int queueCap,
      ScheduledExecutorService executor, long flushIntervalSeconds) {
    this.url = url;
    this.batchSize = batchSize;
    this.queueCap = queueCap;
    executor.scheduleAtFixedRate(this::flush, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);
  }

  public void enqueue(String ip, String username, String reason) {
    if (this.url == null || this.url.isEmpty() || this.queue.size() >= this.queueCap) {
      return;
    }
    this.queue.offer(Map.of(
        "ip", ip,
        "user", username,
        "reason", reason,
        "ts", Instant.now().toEpochMilli()
    ));
  }

  private void flush() {
    if (this.queue.isEmpty()) {
      return;
    }

    List<Map<String, Object>> batch = new ArrayList<>(this.batchSize);
    for (int i = 0; i < this.batchSize; i++) {
      Map<String, Object> entry = this.queue.poll();
      if (entry == null) {
        break;
      }
      batch.add(entry);
    }

    if (batch.isEmpty()) {
      return;
    }

    try {
      String json = toJsonArray(batch);
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(this.url))
          .timeout(Duration.ofSeconds(5))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();
      HTTP_CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding());
    } catch (Exception e) {
      if (this.warnedOnce.compareAndSet(false, true)) {
        LOGGER.warn("PraharShield WebhookBatcher: failed to send batch to '{}': {}", this.url, e.getMessage());
      }
    }
  }

  private static String toJsonArray(List<Map<String, Object>> batch) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < batch.size(); i++) {
      Map<String, Object> entry = batch.get(i);
      if (i > 0) {
        sb.append(",");
      }
      sb.append("{\"ip\":\"").append(entry.get("ip"))
          .append("\",\"user\":\"").append(entry.get("user"))
          .append("\",\"reason\":\"").append(entry.get("reason"))
          .append("\",\"ts\":").append(entry.get("ts"))
          .append("}");
    }
    sb.append("]");
    return sb.toString();
  }
}
