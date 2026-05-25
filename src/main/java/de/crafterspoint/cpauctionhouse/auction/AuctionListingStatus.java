package de.crafterspoint.cpauctionhouse.auction;

/**
 * Lifecycle states of an auction listing row in {@code auction_listings}.
 */
public enum AuctionListingStatus {
    ACTIVE,
    SOLD,
    EXPIRED,
    CANCELLED,
    REMOVED
}
