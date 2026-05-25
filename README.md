# CPAuctionHouse

**Premium Auction House Plugin** for **Bukkit**, **Spigot**, **Paper**, and **Purpur**.

| | |
|---|---|
| **Version** | 1.0.0-RC1 (Release Candidate) |
| **Minecraft** | 1.13+ |
| **Java** | 8+ (bytecode target: Java 8) |
| **Economy** | Vault required |
| **Storage** | SQLite (default), MySQL/MariaDB (optional) |
| **License** | Proprietary — All Rights Reserved |

## Overview

CPAuctionHouse is a standalone premium auction house plugin with a full GUI marketplace, text command fallback, and German default messages. Players can sell items, browse listings, search and sort the market, buy listings, cancel their own offers, and collect returned items.

Built for production servers: SQLite works out of the box with no external database. MySQL/MariaDB is available for larger networks that need shared storage.

## Features

- **GUI marketplace** — `/ah` opens the main hub (browse, sell, listings, collect, search)
- **Anvil price input** — sell flow via in-game anvil GUI
- **Search & sort** — filter by item, seller, or ID; cycle sort modes in browse GUI
- **Text commands** — `/ah sell`, `/ah browse`, `/ah search`, `/ah buy`, and more
- **SQLite storage** — shaded driver, no setup required
- **MySQL/MariaDB** — optional shared database with HikariCP pooling
- **Vault economy** — listing fees, sale tax, buy/sell transactions
- **Session-based GUI** — no item duplication via shift-click, drag, or hotbar keys

## Requirements

- Java 8 or newer
- Bukkit / Spigot / Paper / Purpur **1.13+**
- [Vault](https://www.spigotmc.org/resources/vault.34315/) with a registered economy provider (e.g. EssentialsX Economy)

## Quick Start

1. Install Vault and a compatible economy plugin.
2. Place `cpauctionhouse-1.0.0-RC1.jar` in your `plugins/` folder.
3. Start or restart the server.
4. Run `/cpauctionhouse info` to verify storage and economy.
5. Open `/ah` to use the marketplace.

See [INSTALLATION.md](INSTALLATION.md) for detailed setup including MySQL.

## Documentation

| Document | Description |
|----------|-------------|
| [INSTALLATION.md](INSTALLATION.md) | Setup and first-run checklist |
| [COMMANDS.md](COMMANDS.md) | All player and admin commands |
| [PERMISSIONS.md](PERMISSIONS.md) | Permission nodes and defaults |
| [CONFIGURATION.md](CONFIGURATION.md) | `config.yml` reference |
| [TESTING.md](TESTING.md) | Release testing checklist |
| [CHANGELOG.md](CHANGELOG.md) | Version history |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | Bundled library attributions |
| [LICENSE.txt](LICENSE.txt) | Proprietary license terms |
| [EULA.md](EULA.md) | End User License Agreement |

## Build (from source)

```bash
mvn clean package
```

Output: `target/cpauctionhouse-1.0.0-RC1.jar`

## Third-Party Libraries

This plugin shades: AnvilGUI (MIT), sqlite-jdbc (Apache 2.0), MySQL Connector/J, HikariCP (Apache 2.0), SLF4J API (MIT). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

This project is **source-available**, not open source.

**License:** Proprietary — All Rights Reserved

Viewing or forking this repository does **not** grant usage rights. A purchased license is required to run the compiled plugin on a server. See [LICENSE.txt](LICENSE.txt) and [EULA.md](EULA.md).

## Author

CraftersPoint
