# Permissions

CPAuctionHouse uses a flat permission structure under the `cpauctionhouse.*` namespace.

## Permission nodes

| Node | Default | Description |
|------|---------|-------------|
| `cpauctionhouse.use` | `true` | Base access: `/ah`, `/cpauctionhouse`, GUI entry |
| `cpauctionhouse.sell` | `true` | Create listings (`/ah sell`, GUI sell) |
| `cpauctionhouse.buy` | `true` | Purchase listings (`/ah buy`, GUI buy confirm) |
| `cpauctionhouse.cancel` | `true` | Cancel own listings (`/ah cancel`, GUI cancel) |
| `cpauctionhouse.collect` | `true` | Collect mailbox items (`/ah collect`, GUI collect) |
| `cpauctionhouse.browse` | `true` | Browse and search marketplace (`/ah browse`, `/ah search`, GUI browse) |
| `cpauctionhouse.listings` | `true` | View own active listings (`/ah listings`, GUI my listings) |
| `cpauctionhouse.admin` | `op` | Admin commands: reload, cleanup, remove, info |

## Notes

- **`cpauctionhouse.use`** is required for any auction or plugin command access.
- Feature permissions (sell, buy, etc.) are checked in addition to `use` where applicable.
- **`cpauctionhouse.admin`** grants `/ah admin *` and `/cpauctionhouse info|reload`.
- Default values are set in `plugin.yml` and apply when no permission plugin overrides them.

## Example (LuckPerms)

```
/lp group default permission set cpauctionhouse.use true
/lp group default permission set cpauctionhouse.sell true
/lp group default permission set cpauctionhouse.buy true
/lp group vip permission set cpauctionhouse.sell true
/lp group staff permission set cpauctionhouse.admin true
```

## Restricting the auction house

To disable selling for a group while keeping browse/buy:

```
/lp group guest permission set cpauctionhouse.sell false
```

To disable the auction house entirely for a group:

```
/lp group banned permission set cpauctionhouse.use false
```
