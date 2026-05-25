package de.crafterspoint.cpauctionhouse.auction.gui;

import de.crafterspoint.cpauctionhouse.auction.AuctionCollectItem;
import de.crafterspoint.cpauctionhouse.auction.AuctionListing;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player GUI state. Slot actions are stored in a map instead of item PDC.
 */
public final class AuctionGuiSession implements InventoryHolder {

    private final UUID playerId;

    private Inventory currentInventory;
    private String inventoryTitle = "";
    private AuctionGuiScreen currentScreen = AuctionGuiScreen.MAIN;
    private int currentPage = 1;
    private int totalPages = 1;

    private final Map<Integer, AuctionGuiAction> slotActions = new HashMap<Integer, AuctionGuiAction>();
    private final Map<Integer, Long> slotListingIds = new HashMap<Integer, Long>();

    private List<AuctionListing> visibleListings = new ArrayList<AuctionListing>();
    private List<AuctionCollectItem> visibleCollect = new ArrayList<AuctionCollectItem>();

    private AuctionListing pendingBuyListing;
    private AuctionListing pendingCancelListing;

    private int browseFetchGeneration;
    private boolean controlledTransition;

    public AuctionGuiSession(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Inventory getCurrentInventory() {
        return currentInventory;
    }

    @Override
    public Inventory getInventory() {
        if (currentInventory == null) {
            throw new IllegalStateException("AuctionGuiSession has no inventory yet");
        }
        return currentInventory;
    }

    public void setCurrentInventory(Inventory currentInventory) {
        this.currentInventory = currentInventory;
    }

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public void setInventoryTitle(String inventoryTitle) {
        this.inventoryTitle = inventoryTitle == null ? "" : inventoryTitle;
    }

    public AuctionGuiScreen getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(AuctionGuiScreen currentScreen) {
        this.currentScreen = currentScreen;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage);
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
    }

    public void clearSlotBindings() {
        slotActions.clear();
        slotListingIds.clear();
    }

    public void bind(int slot, AuctionGuiAction action) {
        slotActions.put(Integer.valueOf(slot), action);
    }

    public void bindListing(int slot, AuctionGuiAction action, long listingId) {
        slotActions.put(Integer.valueOf(slot), action);
        slotListingIds.put(Integer.valueOf(slot), Long.valueOf(listingId));
    }

    public AuctionGuiAction getAction(int slot) {
        AuctionGuiAction action = slotActions.get(Integer.valueOf(slot));
        return action == null ? AuctionGuiAction.NONE : action;
    }

    public long getListingId(int slot) {
        Long id = slotListingIds.get(Integer.valueOf(slot));
        return id == null ? -1L : id.longValue();
    }

    public List<AuctionListing> getVisibleListings() {
        return visibleListings;
    }

    public void setVisibleListings(List<AuctionListing> visibleListings) {
        if (visibleListings == null) {
            this.visibleListings = new ArrayList<AuctionListing>();
        } else {
            this.visibleListings = visibleListings;
        }
    }

    public List<AuctionCollectItem> getVisibleCollect() {
        return visibleCollect;
    }

    public void setVisibleCollect(List<AuctionCollectItem> visibleCollect) {
        if (visibleCollect == null) {
            this.visibleCollect = new ArrayList<AuctionCollectItem>();
        } else {
            this.visibleCollect = visibleCollect;
        }
    }

    public AuctionListing getPendingBuyListing() {
        return pendingBuyListing;
    }

    public void setPendingBuyListing(AuctionListing pendingBuyListing) {
        this.pendingBuyListing = pendingBuyListing;
    }

    public AuctionListing getPendingCancelListing() {
        return pendingCancelListing;
    }

    public void setPendingCancelListing(AuctionListing pendingCancelListing) {
        this.pendingCancelListing = pendingCancelListing;
    }

    public int beginBrowseFetch() {
        return ++browseFetchGeneration;
    }

    public int getBrowseFetchGeneration() {
        return browseFetchGeneration;
    }

    public void markControlledTransition() {
        this.controlledTransition = true;
    }

    public boolean consumeControlledTransition() {
        if (controlledTransition) {
            controlledTransition = false;
            return true;
        }
        return false;
    }

    public AuctionListing findVisibleListing(long listingId) {
        for (AuctionListing listing : visibleListings) {
            if (listing.listingId() == listingId) {
                return listing;
            }
        }
        return null;
    }
}
