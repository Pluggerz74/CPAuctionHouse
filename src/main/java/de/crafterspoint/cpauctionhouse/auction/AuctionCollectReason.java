package de.crafterspoint.cpauctionhouse.auction;

/**
 * Why an item ended up in a player's collect storage.
 */
public enum AuctionCollectReason {
    CANCELLED_LISTING,
    EXPIRED_LISTING,
    ADMIN_REMOVED,
    PURCHASED_ITEM_INVENTORY_FULL,
    SYSTEM_RETURN;

    public static AuctionCollectReason fromStringOrDefault(String raw) {
        if (raw == null) {
            return SYSTEM_RETURN;
        }
        try {
            return AuctionCollectReason.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return SYSTEM_RETURN;
        }
    }
}
