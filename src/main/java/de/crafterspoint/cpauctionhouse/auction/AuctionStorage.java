package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for the Auction House backend.
 */
public interface AuctionStorage {

    void init() throws AuctionStorageException;

    void close();

    long insertListing(UUID sellerUuid,
                       String sellerName,
                       ItemStack item,
                       double price,
                       long createdAt,
                       long expiresAt) throws AuctionStorageException;

    Optional<AuctionListing> getListing(long listingId) throws AuctionStorageException;

    List<AuctionListing> getListingsBySellerAndStatus(UUID seller,
                                                      AuctionListingStatus status)
            throws AuctionStorageException;

    List<AuctionListing> getExpiredActiveListings(long now, int limit) throws AuctionStorageException;

    List<AuctionListing> getActiveBrowsePage(long now,
                                             int offset,
                                             int limit,
                                             AuctionBrowseSort sort) throws AuctionStorageException;

    List<AuctionListing> getAllActiveBrowseListings(long now) throws AuctionStorageException;

    int countActiveBrowse(long now) throws AuctionStorageException;

    int deleteTerminalListingsOlderThan(long createdBeforeMillis) throws AuctionStorageException;

    boolean transitionListingStatus(long listingId,
                                    AuctionListingStatus fromStatus,
                                    AuctionListingStatus toStatus) throws AuctionStorageException;

    boolean markSoldIfActive(long listingId,
                             UUID buyerUuid,
                             String buyerName,
                             long now) throws AuctionStorageException;

    boolean revertSoldIfBuyer(long listingId, UUID buyerUuid) throws AuctionStorageException;

    int countListingsByStatus(AuctionListingStatus status) throws AuctionStorageException;

    int countListingsBySellerAndStatus(UUID seller,
                                       AuctionListingStatus status) throws AuctionStorageException;

    long insertCollectItem(UUID ownerUuid,
                           ItemStack item,
                           AuctionCollectReason reason,
                           long createdAt,
                           Long sourceListingId) throws AuctionStorageException;

    List<AuctionCollectItem> getCollectItemsForOwner(UUID owner) throws AuctionStorageException;

    boolean deleteCollectItem(long collectId) throws AuctionStorageException;

    boolean updateCollectItemStack(long collectId, ItemStack newItem) throws AuctionStorageException;

    int countCollectItems() throws AuctionStorageException;
}
