# Testing CPAuctionHouse

## MySQL / MariaDB storage checklist

1. Start with `storage.type: sqlite` and verify existing auction behavior (sell, browse, buy, collect).
2. Create a MySQL/MariaDB database and user with privileges on that database.
3. Switch to `storage.type: mysql` with valid host, port, database, username, and password.
4. Start the server and confirm schema creation (`auction_listings`, `auction_collect_items`).
5. Run `/cpauctionhouse info` or `/ah admin info` — storage line should show **MySQL/MariaDB**.
6. Sell an item through the GUI anvil flow (`/ah` → Sell).
7. Browse listings with `/ah browse` or the GUI marketplace.
8. Buy a listing (`/ah buy <ID>` or GUI buy confirm).
9. Collect items with `/ah collect` or the GUI collect screen.
10. Restart the server and verify listings and collect items persist.
11. Set invalid MySQL credentials with `storage.fallback-to-sqlite-on-error: false` — backend should stay disabled; check console for a severe error.
12. Set invalid MySQL credentials with `storage.fallback-to-sqlite-on-error: true` — plugin should log a warning and use SQLite; admin info should show **SQLite (MySQL fallback)**.

## SQLite default checklist

1. Fresh install with default config — `auctionhouse.db` is created in the plugin data folder.
2. `/cpauctionhouse info` shows **SQLite** as storage type.
3. Full sell / browse / buy / collect cycle works after restart.
