# CPAuctionHouse

Premium Auction House plugin for **Bukkit**, **Spigot**, **Paper**, and **Purpur**.

## Overview

CPAuctionHouse is a standalone premium auction house plugin targeting **Minecraft 1.13+**. It uses SQLite by default, with optional MySQL/MariaDB support planned for production deployments.

## Requirements

- Java 8 or newer (bytecode target: Java 8)
- Bukkit / Spigot / Paper / Purpur 1.13+
- [Vault](https://www.spigotmc.org/resources/vault.34315/) with a registered economy provider (required for auction features)

## Features (scaffold)

This repository currently contains the **standalone project scaffold**:

- Plugin bootstrap and lifecycle
- Configuration and German messages
- Vault economy bridge abstraction
- Storage backend selection (SQLite / MySQL placeholders)
- Command registration with placeholder responses

Auction house logic will be integrated in subsequent development steps.

## Build

```bash
mvn clean package
```

The compiled plugin JAR is written to `target/cpauctionhouse-1.0.0-SNAPSHOT.jar`.

## Configuration

- `config.yml` — storage, economy, and general settings
- `messages.yml` — user-facing German messages

## License

This repository is **source-available**, not open source.

**License:** Proprietary — All Rights Reserved

See [LICENSE.txt](LICENSE.txt) and [EULA.md](EULA.md) for terms. Viewing or forking this repository on GitHub does **not** grant usage rights. A purchased license is required to run the compiled plugin on a server.

## Author

CraftersPoint
