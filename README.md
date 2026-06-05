# PraharFilter

Most powerful bot filtering solution for Minecraft Velocity proxies. Built on LimboAPI.

Indian-scene hardened edition — tuned for cracked client ecosystems, mobile players, and high-RPS join-flood attacks.

## Features

- Highly customizable CAPTCHAs — fonts, backplates, colors, alphabet, length
- Client settings and brand checking (blocks injected/modified clients)
- Auto-disable checks on low CPS (graceful degradation under attack)
- Pre-serialized raw packet cache for low CPU during floods
- Schematic/structure world loading for themed CAPTCHA lobbies
- IP-level join rate limiter (stops 10k-connection bot floods at the gate)
- Async webhook notifier for real-time dashboard feed
- Bilingual Hindi/English message support via MiniMessage

## Commands and Permissions

- `/praharshield reload` — Reload config
- `/praharshield stats` — Toggle statistics action bar
- `/praharshield help` — Show help
- `/sendfilter <player|server>` — Send target(s) to filter limbo (admin only)

Permission nodes: `praharshield.admin.reload`, `praharshield.admin.stats`, `praharshield.admin.sendfilter`

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/praharshield-<version>.jar`

Drop into Velocity `plugins/` alongside `limboapi`.

## License

GNU AGPLv3 — see `LICENSE`.

Based on LimboFilter by Elytrium (https://elytrium.net/).
