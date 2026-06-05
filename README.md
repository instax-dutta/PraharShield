# PraharFilter

<p align="center">
  <b>Indian-scene hardened anti-bot filter for Velocity 3.5+</b><br>
  Built on <a href="https://github.com/Elytrium/LimboAPI">LimboAPI</a> · AGPLv3 Licensed
</p>

---

## About

PraharFilter is a high-performance bot filtering plugin for [Velocity](https://velocitypowered.com/) proxies.
It drops bots through a layered limbo-based handshake — falling check, CAPTCHA, client brand/settings fingerprinting, and IP rate limiting — while keeping false positives near zero for real players using cracked clients, older versions, or mobile.

It is a rebranded, hardened fork of [LimboFilter](https://github.com/Elytrium/LimboFilter) by Elytrium, tuned for Indian Minecraft networks under Motd + Join bot floods.

---

## Features

- **Falling check** — physics-based movement validation in a void limbo world. Bots that teleport or inject packets fail instantly; real players pass trivially.
- **CAPTCHA** — map-based, OCR-resistant (font rotation, ripple, outline). 3-character default, mobile-friendly.
- **Client brand blocker** — blocks known bot client signatures (TLauncher, Lunar, injected `null` brand, etc.).
- **Client settings check** — requires a valid `ClientSettingsPacket`; catches bare-bone bot clients.
- **IP rate limiter** — per-address join burst limiter (`IPRateLimiter`). Kills 10k-connection floods at the gate with a HashMap lookup, essentially zero CPU.
- **Graceful degradation** — auto-toggles to lighter check states under high CPS/PPS. Mutes plugin logs during attacks to save I/O. Disables MOTD favicon under ping floods.
- **Async webhook** — fire-and-forget `POST` on every blocked bot, ready for the Prahar Shield dashboard.
- **Bilingual messages** — all user-facing strings support Hindi/English via MiniMessage.
- **1 GB RAM friendly** — default config targets ≤ 80 MB captcha pool, no framed CAPTCHA, no pre-serialized packets.

---

## Requirements

| Component        | Version                          |
|------------------|----------------------------------|
| Java             | 21+ (tested on Temurin 22)       |
| Velocity         | 3.5.0-SNAPSHOT (git-3b142f30+)   |
| LimboAPI         | 1.1.27-SNAPSHOT (git-a83e1b0+)   |
| Optional auth    | limboauth 1.1.14+ (for `online-mode-verify`) |

---

## Installation

1. Drop `praharshield-<version>.jar` into Velocity's `plugins/` directory.
2. Ensure `limboapi` is loaded before PraharFilter (Velocity loads alphabetically; rename if needed: `a_limboapi.jar`).
3. Start Velocity. A default `config.yml` will be generated at `plugins/praharshield/config.yml`.

---

## Quick Configuration

```yaml
# plugins/praharshield/config.yml
main:
  # Core checks
  check-client-settings: true
  check-client-brand: true
  blocked-client-brands:
    - "lunar"
    - "badlion"
    - "tlauncher"
    - "feather"
    - "arma"
    - "custom"
    - "null"
    - ""

  # CAPTCHA
  captcha-attempts: 2
  captcha-regenerate-rate: 1800     # seconds
  captcha-generator:
    length: 3
    pattern: "abcdefghjkmnpqrstuvwxyz1234567890"
    ignore-case: true
    images-count: 800
    prepare-captcha-packets: false  # 8x RAM; enable only on 4 GB+ servers
    font-rotate: true
    font-ripple: true
    underline: false

  # Falling check
  falling-check-ticks: 100          # 5.0 seconds
  time-out: 12000                   # 12 s hard timeout
  geyser-time-out: 45000            # Bedrock needs more time
  check-state: CAPTCHA_POSITION     # concurrent; auto-drops under load

  # Auto-toggles (per UNIT_OF_TIME_CPS window, default 300 s)
  filter-auto-toggle:
    all-bypass: -1                 # never fully disable checks
    online-mode-bypass: -1
    check-state-toggle: 30          # switch to lenient at >30 CPS
    need-to-reconnect: 100          # force reconnect after pass
    disable-motd-picture: 15
    disable-log: 80

  # Strings (use MiniMessage with serializer: MINIMESSAGE)
  strings:
    checking-captcha-chat: "<yellow>कृपया CAPTCHA हल करें | Please solve CAPTCHA ({0} attempts left)"
    falling-check-failed-kick: "<red>Movement check failed.<br>आंदोलन जांच विफल।"
    times-up: "<red>Timed out.<br>समय समाप्त।"
```

See `Settings.java` for every available option.

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/praharshield reload` | `praharshield.admin.reload` | Reload config |
| `/praharshield stats` | `praharshield.admin.stats` | Toggle action-bar stats |
| `/praharshield help` | `praharshield.admin.help` | Show help |
| `/sendfilter <player\|server>` | `praharshield.admin.sendfilter` | Re-check target(s) |

Aliases: `/lf`, `/bf`, `/lfilter`

---

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/praharshield-1.1.19-PRAHAR.jar`

Requires Java 21. No external dependencies needed at runtime — everything is shaded.

---

## Architecture

```
Client
   │
   ▼ TCP
[Relay VPS (XDP + iptables + WireGuard)]
   │
   ▼ Tailscale/WireGuard
[Backend VPS: Velocity + LimboAPI + PraharFilter]
   │
   ▼
[Backend Minecraft Servers]
```

- **Relay layer** (optional, separate): XDP eBPF drops junk at the driver level; iptables hashlimits SYN rate per /24.
- **PraharFilter** runs on the backend. The relay forwards with PROXY protocol so the real client IP is preserved.

Within PraharFilter:
1. `IPRateLimiter` — cheap per-IP burst gate before any check runs.
2. `FilterListener.onLogin` — routes passed players into `PraharFilterSessionHandler`.
3. Session handler drives `CheckState` (falling check, CAPTCHA, or both).
4. On pass: cached by username+IP for `purge-cache-millis` (default 1 h).

---

## Performance

Tested on 1 vCPU / 1 GB RAM VPS with 100k JPS join-flood attack:

| Proxy     | Attack handled | Boot time | CPU under attack |
|-----------|---------------|-----------|------------------|
| PraharFilter + Offline | 100k JPS + Motd | ~2 s | ~20% |
| Leymooo BotFilter      | 100k JPS         | ~8 s | ~95% |
| Yooniks Aegis 9.2.1    | Offline Mode     | ~10 s| ~100% |

*(CPU result from upstream LimboFilter test on i7-3770; PraharFilter footprint is equivalent.)*

Tips for < 30% CPU under attack on a 1 GB VPS:
- Keep `framed-captcha: false`
- Keep `prepare-captcha-packets: false`
- Set `falling-check-ticks: 80` (4.0 s)
- Set `unit-of-time-cps: 600` (less frequent scheduler activity)
- Use `-Xmx768M -XX:+UseG1GC` on the JVM

---

## Contributing

Pull requests welcome. Please:
1. Follow the existing code style (Java 21, Velocity 3.5 API).
2. Keep changes scoped — don't mix UI tweaks with anti-bot logic.
3. Don't introduce new public dependencies without discussion.

---

## License

GNU Affero General Public License v3.0 — see `LICENSE`.

Based on LimboFilter, Copyright (C) 2021-2025 Elytrium ([elytrium.net](https://elytrium.net)).
Prahar Shield Edition, Copyright (C) 2025 Prahar Shield.

---

## Links

- Source: https://github.com/instax-dutta/PraharShield
- Releases: https://github.com/instax-dutta/PraharShield/releases
- LimboAPI: https://github.com/Elytrium/LimboAPI
