# Installation

This guide covers a standard CPAuctionHouse setup on a Bukkit-compatible server.

## Requirements

- Minecraft server: Bukkit, Spigot, Paper, or Purpur **1.13+**
- Java **8** or newer
- Vault plugin
- An economy plugin that registers with Vault (e.g. EssentialsX Economy or any compatible Vault economy)

## Steps

### 1. Install Vault

Place Vault in your server's `plugins/` folder. Vault is a dependency bridge; it does not provide economy by itself.

### 2. Install an economy plugin

Install a Vault-compatible economy plugin, for example EssentialsX with its economy module, or another supported Vault economy provider.

Verify on startup that Vault reports a registered economy provider in the console.

### 3. Install CPAuctionHouse

Copy `cpauctionhouse-1.0.0-RC1.jar` into the `plugins/` folder.

### 4. Start the server

Start or restart the server. CPAuctionHouse creates:

- `plugins/CPAuctionHouse/config.yml`
- `plugins/CPAuctionHouse/messages.yml`
- `plugins/CPAuctionHouse/auctionhouse.db` (SQLite default)

Check the console for:

```
CPAuctionHouse enabled.
Auction backend: active
Economy: <provider> (available)
```

If the backend is inactive, see [CONFIGURATION.md](CONFIGURATION.md) and the troubleshooting section below.

### 5. Verify with `/cpauctionhouse info`

Run `/cpauctionhouse info` (requires `cpauctionhouse.admin`).

Confirm:

- **Version:** 1.0.0-RC1
- **Speicher:** SQLite (or MySQL/MariaDB if configured)
- **Economy:** your provider name
- **GUI:** aktiviert
- **Backend:** aktiv

### 6. Open the marketplace

Players with `cpauctionhouse.use` can run `/ah` to open the GUI, or use text commands such as `/ah browse`.

### 7. Optional: MySQL / MariaDB

For large servers or networks sharing one database:

1. Create a MySQL/MariaDB database and user with full privileges on that database.
2. Edit `plugins/CPAuctionHouse/config.yml`:

```yaml
storage:
  type: mysql
  fallback-to-sqlite-on-error: false

mysql:
  host: localhost
  port: 3306
  database: cpauctionhouse
  username: your_user
  password: "your_password"
```

3. Restart the server or run `/cpauctionhouse reload` and `/ah admin reload`.
4. Confirm `/cpauctionhouse info` shows MySQL/MariaDB as storage.

Set `fallback-to-sqlite-on-error: true` if you want the plugin to use SQLite when MySQL is unreachable (useful for dev/staging).

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| Backend inactive, economy unavailable | Vault missing or no economy provider registered |
| Backend inactive, storage error | Invalid MySQL credentials or database unreachable |
| `/ah` shows disabled message | Backend inactive or `auction.enabled: false` |
| GUI does not open | `auction.gui.enabled: false` or backend inactive |

## Permissions

Grant players `cpauctionhouse.use` (default: true) and feature permissions as needed. See [PERMISSIONS.md](PERMISSIONS.md).

## Next steps

- Review [CONFIGURATION.md](CONFIGURATION.md) for listing limits, tax, and GUI options.
- Run the checklist in [TESTING.md](TESTING.md) before production use.
