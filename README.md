# CPAuctionHouse

Premium Auction House plugin for **Bukkit**, **Spigot**, **Paper**, and **Purpur**.

## Overview

CPAuctionHouse is a standalone premium auction house plugin targeting **Minecraft 1.13+**. It uses SQLite by default, with optional MySQL/MariaDB support planned for production deployments.

## Requirements

- Java 8 or newer (bytecode target: Java 8)
- Bukkit / Spigot / Paper / Purpur 1.13+
- [Vault](https://www.spigotmc.org/resources/vault.34315/) with a registered economy provider (required for auction features)

## Features

- Full auction backend (listings, buy, cancel, collect, browse, search)
- German GUI hub (`/ah`) with browse, my listings, collect, and anvil sell price input
- Text commands as fallback (`/ah sell`, `/ah browse`, etc.)
- SQLite storage (shaded), Vault economy integration

## Build

```bash
mvn clean package
```

The compiled plugin JAR is written to `target/cpauctionhouse-1.0.0-SNAPSHOT.jar`.

## Configuration

- `config.yml` — storage, economy, auction, and GUI settings
- `messages.yml` — user-facing German messages

### Storage

**SQLite (default)** — no external database required. Data is stored in `plugins/CPAuctionHouse/auctionhouse.db`.

**MySQL / MariaDB (optional)** — recommended for larger servers or networks that need a shared database.

```yaml
storage:
  type: mysql
  fallback-to-sqlite-on-error: false

mysql:
  host: localhost
  port: 3306
  database: cpauctionhouse
  username: root
  password: "your-password"
  pool-size: 10
  use-ssl: false
  connection-timeout-ms: 10000
  max-lifetime-ms: 1800000
  idle-timeout-ms: 600000
  parameters: "useUnicode=true&characterEncoding=utf8"
```

MySQL requires a reachable server, an existing database, and valid credentials. If connection fails and `fallback-to-sqlite-on-error` is `false`, the auction backend stays disabled until the issue is fixed.

See [TESTING.md](TESTING.md) for a manual test checklist.

## Third-Party Libraries

This plugin shades the following libraries into the JAR:

- **AnvilGUI** (WesJD, MIT) — anvil text input for GUI sell flow
- **sqlite-jdbc** (Xerial, Apache 2.0) — SQLite database driver
- **MySQL Connector/J** (Oracle, GPL 2.0 with Universal FOSS Exception) — MySQL/MariaDB driver
- **HikariCP** (brettwooldridge, Apache 2.0) — JDBC connection pooling for MySQL
- **SLF4J API** (QOS.ch, MIT) — logging API used by HikariCP

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for attribution details.

## License

This repository is **source-available**, not open source.

**License:** Proprietary — All Rights Reserved

See [LICENSE.txt](LICENSE.txt) and [EULA.md](EULA.md) for terms. Viewing or forking this repository on GitHub does **not** grant usage rights. A purchased license is required to run the compiled plugin on a server.

## Author

CraftersPoint
