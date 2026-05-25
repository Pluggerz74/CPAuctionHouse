# Configuration

CPAuctionHouse settings are stored in `plugins/CPAuctionHouse/config.yml`. User-facing text is in `messages.yml`.

After editing config, run `/cpauctionhouse reload` or restart the server.

## Storage

| Key | Default | Description |
|-----|---------|-------------|
| `storage.type` | `sqlite` | Storage backend: `sqlite` or `mysql` |
| `storage.fallback-to-sqlite-on-error` | `false` | If MySQL fails, fall back to SQLite when `true` |

**SQLite** is recommended for small and medium servers — no external database required.

**MySQL/MariaDB** is recommended for large servers and networks that need a shared database across instances.

### SQLite

| Key | Default | Description |
|-----|---------|-------------|
| `sqlite.file` | `auctionhouse.db` | Database file name (in plugin data folder) |

### MySQL / MariaDB

| Key | Default | Description |
|-----|---------|-------------|
| `mysql.host` | `localhost` | Database host |
| `mysql.port` | `3306` | Database port |
| `mysql.database` | `cpauctionhouse` | Database name (must exist) |
| `mysql.username` | `root` | Database user |
| `mysql.password` | `""` | Database password |
| `mysql.pool-size` | `10` | HikariCP maximum pool size |
| `mysql.use-ssl` | `false` | Enable SSL for JDBC connection |
| `mysql.parameters` | `useUnicode=true&characterEncoding=utf8` | Extra JDBC URL parameters |
| `mysql.connection-timeout-ms` | `10000` | Connection timeout |
| `mysql.max-lifetime-ms` | `1800000` | Maximum connection lifetime |
| `mysql.idle-timeout-ms` | `600000` | Idle connection timeout |

## Economy

| Key | Default | Description |
|-----|---------|-------------|
| `economy.require-vault` | `true` | Require Vault with a registered economy provider |

## Auction — general

| Key | Default | Description |
|-----|---------|-------------|
| `auction.enabled` | `true` | Master switch for the auction backend |

## Auction — listings

| Key | Default | Description |
|-----|---------|-------------|
| `auction.listings.max-active-per-player` | `10` | Maximum active listings per player |
| `auction.listings.duration-hours` | `72` | Hours until a listing expires |
| `auction.listings.min-price` | `1.0` | Minimum listing price |
| `auction.listings.max-price` | `1000000000.0` | Maximum listing price |
| `auction.listings.allow-own-purchase` | `false` | Allow players to buy their own listings |
| `auction.listings.blocked-materials` | `[]` | Materials that cannot be listed (e.g. `BEDROCK`) |

## Auction — economy fees

| Key | Default | Description |
|-----|---------|-------------|
| `auction.economy.listing-fee` | `0.0` | Fee charged when creating a listing |
| `auction.economy.sale-tax-percent` | `5.0` | Tax deducted from seller payout on sale |

## Auction — GUI

| Key | Default | Description |
|-----|---------|-------------|
| `auction.gui.enabled` | `true` | Enable the GUI marketplace |
| `auction.gui.anvil-price-input` | `true` | Use anvil GUI for sell price input |
| `auction.gui.anvil-search-input` | `true` | Use anvil GUI for browse search input |
| `auction.gui.default-sort` | `NEWEST` | Default browse sort mode |
| `auction.gui.title` | `&6Auktionshaus` | Main GUI inventory title |
| `auction.gui.refresh-cooldown-ms` | `750` | Cooldown between browse refresh clicks |

### Sort values for `auction.gui.default-sort`

- `NEWEST`
- `OLDEST`
- `PRICE_LOW_TO_HIGH` (alias: `PRICE_ASC`)
- `PRICE_HIGH_TO_LOW` (alias: `PRICE_DESC`)
- `ENDING_SOON` (alias: `EXPIRING_SOON`)

## Other auction settings

| Key | Default | Description |
|-----|---------|-------------|
| `auction.browse.page-size` | `8` | Text command browse page size |
| `auction.cleanup.expire-check-interval-seconds` | `60` | How often expired listings are processed |
| `auction.cleanup.old-listing-retention-days` | `30` | Days to keep old listing records before cleanup |
| `auction.confirmations.expensive-purchase-threshold` | `10000.0` | GUI buy confirmation threshold |

## Settings

| Key | Default | Description |
|-----|---------|-------------|
| `settings.language` | `de` | Language hint (messages from `messages.yml`) |
| `settings.debug` | `false` | Extra debug logging |
| `auction.debug` | `false` | Auction-specific debug logging |

## Messages

Edit `plugins/CPAuctionHouse/messages.yml` for all player-facing text. On plugin update, missing keys are merged automatically from the bundled defaults.

See [INSTALLATION.md](INSTALLATION.md) for MySQL setup and [TESTING.md](TESTING.md) for validation steps.
