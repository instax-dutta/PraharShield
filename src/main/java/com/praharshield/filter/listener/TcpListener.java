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

package com.praharshield.filter.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.net.InetAddress;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import com.praharshield.filter.PraharFilter;
import com.praharshield.filter.ratelimit.IPRateLimiter;

public class TcpListener {

  private final PraharFilter plugin;

  public TcpListener(PraharFilter plugin) {
    this.plugin = plugin;
  }

  public void registerAddress(InetAddress address) {
    // PCAP-based L4/L7 proxy detector would register here
    // Kept as stub for API compatibility
  }

  public void removeAddress(InetAddress address) {
    // PCAP-based L4/L7 proxy detector would remove here
  }

  public void start() {
    // PCAP initialization stub
  }

  public void stop() {
    // PCAP cleanup stub
  }
}
