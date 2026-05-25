package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Immutable snapshot of a row in {@code auction_listings}.
 */
public final class AuctionListing {

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final double price;
    private final long createdAt;
    private final long expiresAt;
    private final AuctionListingStatus status;
    private final UUID buyerUuid;
    private final String buyerName;

    public AuctionListing(long listingId,
                          UUID sellerUuid,
                          String sellerName,
                          ItemStack itemStack,
                          double price,
                          long createdAt,
                          long expiresAt,
                          AuctionListingStatus status,
                          UUID buyerUuid,
                          String buyerName) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack == null ? null : itemStack.clone();
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
    }

    public long listingId() {
        return listingId;
    }

    public UUID sellerUuid() {
        return sellerUuid;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack itemStack() {
        return itemStack == null ? null : itemStack.clone();
    }

    public double price() {
        return price;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public AuctionListingStatus status() {
        return status;
    }

    public UUID buyerUuid() {
        return buyerUuid;
    }

    public String buyerName() {
        return buyerName;
    }

    public long remainingMillis(long now) {
        long delta = expiresAt - now;
        return Math.max(delta, 0L);
    }
}
