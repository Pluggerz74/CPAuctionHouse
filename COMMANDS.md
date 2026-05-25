# Commands

All auction commands are accessed through `/ah`, `/auctionhouse`, or `/auktion` (aliases). Plugin admin commands use `/cpauctionhouse` or `/cpah`.

Color codes and messages are defined in `messages.yml` (German by default).

## Player commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ah` | `cpauctionhouse.use` | Opens the auction house GUI (if enabled and backend active) |
| `/auctionhouse` | `cpauctionhouse.use` | Alias for `/ah` |
| `/auktion` | `cpauctionhouse.use` | Alias for `/ah` |
| `/ah help` | `cpauctionhouse.use` | Lists available commands |
| `/ah sell <price>` | `cpauctionhouse.sell` | Sell the item in your main hand at the given price |
| `/ah browse [page]` | `cpauctionhouse.browse` | Browse active listings (text output, page optional) |
| `/ah search <text>` | `cpauctionhouse.browse` | Search listings by item name, seller, or listing ID |
| `/ah buy <id>` | `cpauctionhouse.buy` | Purchase a listing by ID |
| `/ah listings` | `cpauctionhouse.listings` | Show your active listings |
| `/ah cancel <id>` | `cpauctionhouse.cancel` | Cancel your own listing (item goes to collect) |
| `/ah collect` | `cpauctionhouse.collect` | Collect items from your auction mailbox |

### GUI equivalents

Many actions are also available in the GUI opened with `/ah`:

- **Marktplatz** — browse with sort, search, pagination
- **Item verkaufen** — anvil price input (when enabled)
- **Meine Angebote** — own listings with cancel
- **Abholen** — collect mailbox items
- **Suche** — anvil search input (when enabled)

If the backend is disabled, `/ah` shows a German unavailable message instead of opening the GUI.

## Admin commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ah admin info` | `cpauctionhouse.admin` | Detailed auction backend statistics |
| `/ah admin reload` | `cpauctionhouse.admin` | Reload auction config and messages |
| `/ah admin cleanup` | `cpauctionhouse.admin` | Remove old expired/cancelled listing records |
| `/ah admin remove <id>` | `cpauctionhouse.admin` | Force-remove a listing (item sent to seller collect) |

## Plugin commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/cpauctionhouse` | `cpauctionhouse.use` | Plugin summary (version, GUI, backend, storage, economy) |
| `/cpauctionhouse info` | `cpauctionhouse.admin` | Full status including listing counts and tax settings |
| `/cpauctionhouse reload` | `cpauctionhouse.admin` | Reload config, messages, economy, and auction backend |
| `/cpah` | same as above | Alias for `/cpauctionhouse` |

## Examples

```
/ah
/ah help
/ah sell 500
/ah browse
/ah browse 2
/ah search diamant
/ah buy 42
/ah listings
/ah cancel 15
/ah collect
/ah admin info
/ah admin reload
/ah admin cleanup
/ah admin remove 99
/cpauctionhouse info
/cpauctionhouse reload
```

## Tab completion

- `/ah` — subcommands: sell, listings, cancel, collect, browse, search, buy, admin, help
- `/ah admin` — remove, info, reload, cleanup
- `/cpauctionhouse` — info, reload (admin only)
