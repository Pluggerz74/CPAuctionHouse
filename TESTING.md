# Testing CPAuctionHouse

Release testing checklist for **1.0.0-RC1**. Run on a test server before production deployment.

## Startup

- [ ] Server starts without errors with CPAuctionHouse + Vault + economy plugin
- [ ] Console shows `CPAuctionHouse enabled.` and `Auction backend: active`
- [ ] `/cpauctionhouse info` shows version **1.0.0-RC1**, storage, economy, GUI, backend

## Vault / economy

- [ ] **Vault missing:** remove Vault, restart — backend inactive, clean German message on `/ah`
- [ ] **Economy present:** Vault + EssentialsX Economy (or compatible) — backend active, buy/sell works

## Storage — SQLite (default)

- [ ] Fresh install creates `plugins/CPAuctionHouse/auctionhouse.db`
- [ ] `/cpauctionhouse info` shows **SQLite**
- [ ] Sell → browse → buy → collect cycle works
- [ ] **Restart persistence:** listings and collect items survive server restart

## Storage — MySQL / MariaDB

- [ ] Valid credentials: `storage.type: mysql` — schema created, info shows MySQL/MariaDB
- [ ] Full sell / browse / buy / collect cycle on MySQL
- [ ] Restart persistence on MySQL
- [ ] **Invalid credentials + `fallback-to-sqlite-on-error: false`:** backend stays disabled, console error
- [ ] **Invalid credentials + `fallback-to-sqlite-on-error: true`:** falls back to SQLite, warning logged

## GUI — core flows

- [ ] `/ah` opens main GUI
- [ ] Browse shows listings, pagination works
- [ ] **Sell:** anvil price input → listing created
- [ ] **Buy:** listing click → confirm → purchase completes
- [ ] **Cancel:** own listing → confirm → item in collect
- [ ] **Collect:** mailbox items retrieved

## GUI — search and sort

- [ ] Sort button cycles all modes (newest, oldest, price asc/desc, ending soon)
- [ ] Listing order changes with sort
- [ ] Search button opens anvil input
- [ ] Search term filters results
- [ ] Reset search clears filter
- [ ] Refresh keeps current page/sort/search
- [ ] Previous/next keep sort/search
- [ ] Empty or placeholder search clears safely
- [ ] Text `/ah search <text>` still works
- [ ] GUI labels show localized text (not raw message keys)

## Text commands

- [ ] `/ah help` lists commands (no placeholder/scaffold text)
- [ ] `/ah sell`, `/ah browse`, `/ah buy`, `/ah listings`, `/ah cancel`, `/ah collect`
- [ ] `/ah admin info`, `/ah admin reload`, `/ah admin cleanup`, `/ah admin remove <id>`

## GUI anti-dupe

Test with real items in the auction GUI inventory:

- [ ] **Shift-click** — items cannot leave GUI
- [ ] **Hotbar number key** — cannot swap items out
- [ ] **Drag** — cancelled, no item movement
- [ ] **Double-click** — no collection or duplication
- [ ] **Drop (Q)** — items cannot be dropped from GUI
- [ ] **Close inventory during transaction** — no item loss or duplication
- [ ] **Item swap during anvil price input** — sell aborted safely if hand item changes

## Version matrix

Record pass/fail for each target environment:

| Platform | Version | Java | Result | Notes |
|----------|---------|------|--------|-------|
| Spigot | 1.13.2 | 8 | | |
| Paper | 1.16.5 | 8/11 | | |
| Paper | 1.20.6 | 17/21 | | |
| Paper / Purpur | 1.21.x | 21 | | |

## Reload

- [ ] `/cpauctionhouse reload` reloads config and messages without errors
- [ ] `/ah admin reload` reloads auction config
- [ ] New `messages.yml` keys appear after jar update (default merge)

## Console

- [ ] No errors during normal GUI use
- [ ] No Wrapper26 / class version errors on Paper 1.21.x (AnvilGUI shading)

## Sign-off

| Tester | Date | Environment | RC1 approved |
|--------|------|-------------|--------------|
| | | | |
