package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Immutable snapshot of a row in {@code auction_collect_items}.
 */
public final class AuctionCollectItem {

    private final long collectId;
    private final UUID ownerUuid;
    private final ItemStack itemStack;
    private final AuctionCollectReason reason;
    private final long createdAt;
    private final Long sourceListingId;

    public AuctionCollectItem(long collectId,
                              UUID ownerUuid,
                              ItemStack itemStack,
                              AuctionCollectReason reason,
                              long createdAt,
                              Long sourceListingId) {
        this.collectId = collectId;
        this.ownerUuid = ownerUuid;
        this.itemStack = itemStack == null ? null : itemStack.clone();
        this.reason = reason;
        this.createdAt = createdAt;
        this.sourceListingId = sourceListingId;
    }

    public long collectId() {
        return collectId;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public ItemStack itemStack() {
        return itemStack == null ? null : itemStack.clone();
    }

    public AuctionCollectReason reason() {
        return reason;
    }

    public long createdAt() {
        return createdAt;
    }

    public Long sourceListingId() {
        return sourceListingId;
    }
}
