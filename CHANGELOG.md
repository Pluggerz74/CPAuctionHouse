# Changelog

All notable changes to CPAuctionHouse are documented here.

## 1.0.0-RC1 — 2026-05-25

First public release candidate.

### Added

- Standalone premium auction house plugin (CPAuctionHouse)
- SQLite storage (default, shaded sqlite-jdbc)
- MySQL/MariaDB storage (optional, HikariCP + MySQL Connector/J)
- Vault economy integration (buy, sell, listing fee, sale tax)
- GUI marketplace (`/ah`) with browse, sell, listings, collect, search
- AnvilGUI price input for sell flow
- AnvilGUI search input with text command fallback
- Browse sort cycling (newest, oldest, price, ending soon)
- Text commands: sell, browse, search, buy, listings, cancel, collect, admin
- German default messages (`messages.yml`)
- Session-based GUI click handling (anti-dupe)
- `/cpauctionhouse info` with version, storage, economy, GUI, and backend status
- MessageService default merge for plugin updates

### Target

- Java 8 bytecode
- Minecraft 1.13+ (Bukkit / Spigot / Paper / Purpur)
- No Paper-only APIs, no NMS

---

Future stable **1.0.0** will follow after RC testing and feedback.
