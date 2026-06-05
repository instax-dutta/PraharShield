/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import com.praharshield.filter.PraharFilter;

public class WebhookNotifier {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private final String endpoint;

  public WebhookNotifier(String endpoint) {
    this.endpoint = endpoint;
  }

  public void notifyBlocked(String username, String ip, String reason) {
    if (this.endpoint == null || this.endpoint.isEmpty()) return;

    PraharFilter.getInstance().getServer().getScheduler().buildTask(PraharFilter.getInstance(),
        () -> {
          try {
            String payload = String.format(
                "{\"username\":\"%s\",\"ip\":\"%s\",\"reason\":\"%s\",\"timestamp\":%d}",
                username, ip, reason, System.currentTimeMillis()
            );
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(this.endpoint))
                .timeout(Duration.ofMillis(500))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding());
          } catch (Exception ignored) {}
        }
    ).schedule();
  }
}
