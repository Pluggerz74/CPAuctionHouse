package de.crafterspoint.cpauctionhouse.auction;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.economy.EconomyBridge;
import de.crafterspoint.cpauctionhouse.economy.EconomyTransactionResult;
import de.crafterspoint.cpauctionhouse.storage.AuctionStorageFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Owns the Auction House backend: live {@link AuctionConfig}, {@link
 * AuctionStorage}, {@link AuctionExpiryService}, and the single-threaded
 * executor used for every database call.
 *
 * <p>Threading rules:
 * <ul>
 *     <li>Every method on {@link AuctionStorage} runs on the dedicated
 *         {@code dbExecutor} - never on the main server thread.</li>
 *     <li>Every Bukkit API call (inventory access, fee withdrawal,
 *         message send) hops back to the main thread before touching
 *         player state.</li>
 *     <li>The public {@code createListing} / {@code cancelListing} /
 *         {@code collectAll} / {@code adminRemoveListing} methods return
 *         {@link CompletableFuture}s that always complete on the main
 *         thread, so command handlers can use the results directly.</li>
 * </ul>
 *
 * <p>Dupe-protection contract for the sell flow:
 * <ol>
 *     <li>The seller's item is cloned on the main thread.</li>
 *     <li>The listing fee is withdrawn via {@link EconomyBridge} on the
 *         main thread.</li>
 *     <li>The original item is removed from the seller's main hand on
 *         the main thread.</li>
 *     <li>The DB insert runs on the executor.</li>
 *     <li>If the insert fails, the cloned item is restored on the main
 *         thread (to the hand if it is empty, otherwise into the
 *         seller's collect storage) and the fee is refunded.</li>
 * </ol>
 * The item is therefore in exactly one place at every observable point
 * in time: either in the seller's hand, in the listings table, or in
 * the collect table.
 */
public final class AuctionHouseManager {

    private static final int EXPIRY_BATCH_LIMIT = 200;

    private final CPAuctionHousePlugin plugin;

    private ExecutorService dbExecutor;
    private AuctionStorage storage;
    private AuctionConfig config;
    private AuctionExpiryService expiryService;

    /**
     * True once {@link #enable()} has finished without storage errors.
     * When false every public operation completes with
     * {@code auction.disabled} so we never partially execute a flow
     * against a half-initialised backend.
     */
    private volatile boolean active;

    /**
     * Sticky reason why the backend is inactive (e.g. SQLite driver
     * missing). Surfaced on {@code /ah admin info}.
     */
    private volatile String inactiveReason;

    /** Human-readable storage label for admin info (e.g. SQLite, MySQL/MariaDB). */
    private volatile String storageDisplayType = "-";

    public AuctionHouseManager(CPAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Loads config, opens the storage backend and starts the expiry service.
     * Safe to call multiple times; reload uses {@link #reload()} instead.
     */
    public void enable() {
        this.config = new AuctionConfig(plugin.getConfig(), plugin.getLogger());
        if (!config.isEnabled()) {
            this.active = false;
            this.inactiveReason = "disabled-in-config";
            plugin.getLogger().info("Auction House disabled in config.");
            return;
        }

        this.dbExecutor = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "CPAuctionHouse-AH-DB");
                t.setDaemon(true);
                return t;
            }
        });

        try {
            AuctionStorageFactory.InitResult init = AuctionStorageFactory.create(plugin);
            this.storage = init.getStorage();
            this.storageDisplayType = init.getDisplayType();
            this.active = true;
            this.inactiveReason = null;
            plugin.getLogger().info("Auction House backend ready (storage="
                    + storageDisplayType + ").");
        } catch (AuctionStorageException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Auction House storage init failed; AH is disabled. " + ex.getMessage(), ex);
            this.active = false;
            this.inactiveReason = "storage-error";
            this.storageDisplayType = "mysql".equals(config.getStorageType())
                    ? "MySQL/MariaDB (failed)" : config.getStorageType();
            shutdownExecutor();
            return;
        }

        this.expiryService = new AuctionExpiryService(plugin, this);
        this.expiryService.start(config.getExpireCheckIntervalSeconds());
    }

    /**
     * Re-reads config and restarts the expiry service with the new interval.
     * The storage backend itself is not closed or reopened; the SQLite file
     * is unaffected by reloads.
     */
    public void reload() {
        AuctionConfig newConfig = new AuctionConfig(plugin.getConfig(), plugin.getLogger());

        // Hot-swap a config-only change in the simple case (still
        // enabled, same storage type and file). Anything more invasive
        // requires a full restart of the backend.
        boolean canHotSwap = active
                && newConfig.isEnabled()
                && newConfig.getStorageType().equals(config.getStorageType());
        if ("sqlite".equals(config.getStorageType())) {
            canHotSwap = canHotSwap && newConfig.getStorageFile().equals(config.getStorageFile());
        }

        if (canHotSwap) {
            this.config = newConfig;
            if (expiryService != null) {
                expiryService.stop();
            }
            this.expiryService = new AuctionExpiryService(plugin, this);
            this.expiryService.start(config.getExpireCheckIntervalSeconds());
            return;
        }
        disable();
        enable();
    }

    /**
     * Stops the expiry service, drains the executor and closes the
     * storage backend. Safe to call when {@link #active} is false.
     */
    public void disable() {
        if (expiryService != null) {
            expiryService.stop();
            expiryService = null;
        }
        if (dbExecutor != null) {
            // Submit close() through the executor so it runs after any
            // pending work, then shut the executor down.
            AuctionStorage local = this.storage;
            if (local != null) {
                try {
                    dbExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            local.close();
                        }
                    }).get(5, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    plugin.getLogger().warning("[AH] Storage close did not finish cleanly: "
                            + ex.getMessage());
                }
            }
            shutdownExecutor();
        }
        this.storage = null;
        this.active = false;
    }

    private void shutdownExecutor() {
        if (dbExecutor == null) return;
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            dbExecutor.shutdownNow();
        }
        dbExecutor = null;
    }

    // -------------------------------------------------------------- public API

    public boolean isActive() {
        return active;
    }

    public AuctionConfig getConfig() {
        return config;
    }

    public String getInactiveReason() {
        return inactiveReason;
    }

    /**
     * Creates an ACTIVE listing for the item in the seller's main hand.
     * See class-level dupe-protection contract.
     */
    public CompletableFuture<ListingCreateResult> createListing(Player seller, double price) {
        if (!active) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.disabled"));
        }
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR || hand.getAmount() <= 0) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.invalid-item"));
        }
        Material type = hand.getType();
        if (!isListableMaterial(type)) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.invalid-item"));
        }
        if (config.getBlockedMaterials().contains(type)) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.blocked-item",
                    singlePlaceholder("material", type.name())));
        }
        if (price < config.getMinPrice()) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.price-too-low",
                    singlePlaceholder("min", formatMoney(config.getMinPrice()))));
        }
        if (price > config.getMaxPrice()) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.price-too-high",
                    singlePlaceholder("max", formatMoney(config.getMaxPrice()))));
        }
        return createListingFromSnapshot(seller, hand.clone(), price, true);
    }

    /**
     * Creates an ACTIVE listing from an explicit stack (escrow). Does not
     * read, clear, or modify the player's main hand or any other inventory slot.
     */
    public CompletableFuture<ListingCreateResult> createListing(Player seller,
                                                                ItemStack offeredItem,
                                                                double price) {
        if (!active) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.disabled"));
        }
        if (offeredItem == null || offeredItem.getType() == Material.AIR || offeredItem.getAmount() <= 0) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.invalid-item"));
        }
        Material type = offeredItem.getType();
        if (!isListableMaterial(type)) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.invalid-item"));
        }
        if (isInternalGuiItem(offeredItem)) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure("auction.invalid-item"));
        }
        if (config.getBlockedMaterials().contains(type)) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.blocked-item",
                    singlePlaceholder("material", type.name())));
        }
        if (price < config.getMinPrice()) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.price-too-low",
                    singlePlaceholder("min", formatMoney(config.getMinPrice()))));
        }
        if (price > config.getMaxPrice()) {
            return CompletableFuture.completedFuture(ListingCreateResult.failure(
                    "auction.price-too-high",
                    singlePlaceholder("max", formatMoney(config.getMaxPrice()))));
        }
        return createListingFromSnapshot(seller, offeredItem.clone(), price, false);
    }

    /**
     * Shared listing creation: {@code snapshot} is an isolated clone; when
     * {@code consumeFromMainHand} is {@code true} the main hand is cleared
     * after the fee succeeds (command flow). When {@code false}, no inventory
     * slot is touched (escrow flow).
     */
    private CompletableFuture<ListingCreateResult> createListingFromSnapshot(final Player seller,
                                                                             final ItemStack snapshot,
                                                                             final double price,
                                                                             final boolean consumeFromMainHand) {
        final UUID sellerId = seller.getUniqueId();
        final String sellerName = seller.getName();
        final CompletableFuture<ListingCreateResult> out = new CompletableFuture<ListingCreateResult>();
        runDb(new SqlCallable<Integer>() {
            @Override
            public Integer call() throws AuctionStorageException {
                return storage.countListingsBySellerAndStatus(sellerId, AuctionListingStatus.ACTIVE);
            }
        }).whenComplete(new java.util.function.BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(final Integer count, final Throwable countEx) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (countEx != null) {
                            logStorage("countListingsBySellerAndStatus", countEx);
                            out.complete(ListingCreateResult.failure("auction.storage-error"));
                            return;
                        }
                        int max = config.getMaxActiveListingsDefault();
                        if (!seller.hasPermission(AuctionPermission.ADMIN) && count >= max) {
                            out.complete(ListingCreateResult.failure(
                                    "auction.max-listings-reached",
                                    singlePlaceholder("max", Integer.toString(max))));
                            return;
                        }

                        if (!seller.isOnline()) {
                            out.complete(ListingCreateResult.failure("auction.invalid-item"));
                            return;
                        }
                        if (consumeFromMainHand) {
                            ItemStack currentHand = seller.getInventory().getItemInMainHand();
                            if (currentHand == null || !currentHand.isSimilar(snapshot)
                                    || currentHand.getAmount() != snapshot.getAmount()) {
                                out.complete(ListingCreateResult.failure("auction.invalid-item"));
                                return;
                            }
                        }

                        EconomyBridge bridge = plugin.getEconomyBridge();
                        boolean requireEconomy = requireEconomyForAuctionHouse();
                        double fee = config.getListingFee();
                        if ((fee > 0.0D || requireEconomy) && !bridge.isAvailable()) {
                            out.complete(ListingCreateResult.failure("auction.economy-required"));
                            return;
                        }
                        boolean charged = false;
                        if (fee > 0.0D) {
                            EconomyTransactionResult feeResult = bridge.withdraw(sellerId, fee, "AH listing fee");
                            if (!feeResult.success()) {
                                out.complete(ListingCreateResult.failure(feeResult.reasonKey()));
                                return;
                            }
                            charged = true;
                        }

                        if (consumeFromMainHand) {
                            seller.getInventory().setItemInMainHand(null);
                        }

                        long now = System.currentTimeMillis();
                        long expires = now + config.getDurationMillis();
                        final boolean finalCharged = charged;
                        runDb(new SqlCallable<Long>() {
                            @Override
                            public Long call() throws AuctionStorageException {
                                return storage.insertListing(
                                        sellerId, sellerName, snapshot, price, now, expires);
                            }
                        }).whenComplete(new java.util.function.BiConsumer<Long, Throwable>() {
                            @Override
                            public void accept(final Long listingId, final Throwable insertEx) {
                                runOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (insertEx != null) {
                                            logStorage("insertListing", insertEx);
                                            if (consumeFromMainHand) {
                                                restoreAfterFailure(seller, snapshot, fee, finalCharged);
                                            } else {
                                                restoreAfterFailureWithoutMainHand(seller, snapshot,
                                                        fee, finalCharged);
                                            }
                                            out.complete(ListingCreateResult.failure("auction.storage-error"));
                                            return;
                                        }
                                        if (config.isDebug()) {
                                            plugin.getLogger().info("[AH] Listing #" + listingId
                                                    + " created by " + sellerName
                                                    + " for " + formatMoney(price));
                                        }
                                        out.complete(ListingCreateResult.success(listingId, price, snapshot));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        return out;
    }

    /**
     * Cancels an ACTIVE listing. {@code admin} controls whether the
     * caller may cancel listings owned by other players; pass {@code true}
     * for {@code /ah admin remove}.
     */
    public CompletableFuture<CancelResult> cancelListing(final UUID requester,
                                                         final long listingId,
                                                         final boolean admin) {
        if (!active) {
            return CompletableFuture.completedFuture(CancelResult.failure("auction.disabled"));
        }
        final CompletableFuture<CancelResult> out = new CompletableFuture<CancelResult>();
        runDb(new SqlCallable<java.util.Optional<AuctionListing>>() {
            @Override
            public java.util.Optional<AuctionListing> call() throws AuctionStorageException {
                return storage.getListing(listingId);
            }
        }).whenComplete(new java.util.function.BiConsumer<java.util.Optional<AuctionListing>, Throwable>() {
            @Override
            public void accept(final java.util.Optional<AuctionListing> opt, final Throwable getEx) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (getEx != null) {
                            logStorage("getListing", getEx);
                            out.complete(CancelResult.failure("auction.storage-error"));
                            return;
                        }
                        if (!opt.isPresent()) {
                            out.complete(CancelResult.failure("auction.listing-not-found"));
                            return;
                        }
                        AuctionListing listing = opt.get();
                        if (!admin && !listing.sellerUuid().equals(requester)) {
                            out.complete(CancelResult.failure("auction.not-your-listing"));
                            return;
                        }
                        if (listing.status() != AuctionListingStatus.ACTIVE) {
                            // Already cancelled / expired / sold - treat
                            // every non-ACTIVE state as "nothing to do" to
                            // stay idempotent against double-clicks.
                            out.complete(CancelResult.failure("auction.listing-not-found"));
                            return;
                        }
                        final AuctionListingStatus newStatus = admin
                                ? AuctionListingStatus.REMOVED
                                : AuctionListingStatus.CANCELLED;
                        final AuctionCollectReason reason = admin
                                ? AuctionCollectReason.ADMIN_REMOVED
                                : AuctionCollectReason.CANCELLED_LISTING;

                        runDb(new SqlCallable<Boolean>() {
                            @Override
                            public Boolean call() throws AuctionStorageException {
                                boolean transitioned = storage.transitionListingStatus(
                                        listing.listingId(),
                                        AuctionListingStatus.ACTIVE, newStatus);
                                if (!transitioned) {
                                    return Boolean.FALSE;
                                }
                                storage.insertCollectItem(
                                        listing.sellerUuid(),
                                        listing.itemStack(),
                                        reason,
                                        System.currentTimeMillis(),
                                        listing.listingId());
                                return Boolean.TRUE;
                            }
                        }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                            @Override
                            public void accept(final Boolean ok, final Throwable txEx) {
                                runOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (txEx != null) {
                                            logStorage("cancel/transition", txEx);
                                            out.complete(CancelResult.failure("auction.storage-error"));
                                            return;
                                        }
                                        if (!ok) {
                                            out.complete(CancelResult.failure("auction.listing-not-found"));
                                            return;
                                        }
                                        out.complete(CancelResult.success(listing));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        return out;
    }

    /**
     * Returns every ACTIVE listing owned by {@code owner}. Used by
     * {@code /ah listings}.
     */
    public CompletableFuture<List<AuctionListing>> getActiveListings(final UUID owner) {
        if (!active) {
            return CompletableFuture.completedFuture(Collections.<AuctionListing>emptyList());
        }
        final CompletableFuture<List<AuctionListing>> out = new CompletableFuture<List<AuctionListing>>();
        runDb(new SqlCallable<List<AuctionListing>>() {
            @Override
            public List<AuctionListing> call() throws AuctionStorageException {
                return storage.getListingsBySellerAndStatus(owner, AuctionListingStatus.ACTIVE);
            }
        }).whenComplete(new java.util.function.BiConsumer<List<AuctionListing>, Throwable>() {
            @Override
            public void accept(final List<AuctionListing> list, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("getListingsBySellerAndStatus", ex);
                            out.complete(Collections.<AuctionListing>emptyList());
                            return;
                        }
                        out.complete(list);
                    }
                });
            }
        });
        return out;
    }

    /**
     * Hands the player every collect row that fits into their inventory.
     * Partial stacks are split: the slice that fit is delivered, the
     * remainder stays in storage with a smaller amount. Never drops
     * items on the ground; never deletes a row whose contents were not
     * fully delivered.
     */
    public CompletableFuture<CollectResult> collectAll(final Player player) {
        if (!active) {
            return CompletableFuture.completedFuture(CollectResult.failure("auction.disabled"));
        }
        final UUID owner = player.getUniqueId();
        final CompletableFuture<CollectResult> out = new CompletableFuture<CollectResult>();
        runDb(new SqlCallable<List<AuctionCollectItem>>() {
            @Override
            public List<AuctionCollectItem> call() throws AuctionStorageException {
                return storage.getCollectItemsForOwner(owner);
            }
        }).whenComplete(new java.util.function.BiConsumer<List<AuctionCollectItem>, Throwable>() {
            @Override
            public void accept(final List<AuctionCollectItem> rawItems, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("getCollectItemsForOwner", ex);
                            out.complete(CollectResult.failure("auction.storage-error"));
                            return;
                        }
                        List<AuctionCollectItem> items = filterGuiMarkedCollectRows(owner, rawItems);
                        if (items.isEmpty()) {
                            out.complete(CollectResult.empty());
                            return;
                        }
                        if (!player.isOnline()) {
                            // Logged off between the query and now; nothing
                            // delivered, nothing changed in storage.
                            out.complete(CollectResult.failure("general.player-only"));
                            return;
                        }
                        PlayerInventory inv = player.getInventory();
                        List<Long> toDelete = new ArrayList<Long>();
                        List<UpdatePartial> toUpdate = new ArrayList<UpdatePartial>();
                        int delivered = 0;
                        int remaining = 0;
                        for (AuctionCollectItem ci : items) {
                            ItemStack candidate = ci.itemStack().clone();
                            int before = candidate.getAmount();
                            Map<Integer, ItemStack> leftover = inv.addItem(candidate);
                            if (leftover.isEmpty()) {
                                delivered++;
                                toDelete.add(ci.collectId());
                            } else {
                                ItemStack rest = leftover.values().iterator().next();
                                if (rest.getAmount() >= before) {
                                    // No room at all - row untouched.
                                    remaining++;
                                } else {
                                    // Partial: update the row with the
                                    // unfulfilled remainder.
                                    delivered++;
                                    remaining++;
                                    toUpdate.add(new UpdatePartial(ci.collectId(), rest));
                                }
                            }
                        }
                        final int finalDelivered = delivered;
                        final int finalRemaining = remaining;
                        runDb(new SqlCallable<Void>() {
                            @Override
                            public Void call() throws AuctionStorageException {
                                for (Long id : toDelete) {
                                    storage.deleteCollectItem(id);
                                }
                                for (UpdatePartial up : toUpdate) {
                                    storage.updateCollectItemStack(up.collectId(), up.newStack());
                                }
                                return null;
                            }
                        }).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
                            @Override
                            public void accept(final Void ignored, final Throwable dbEx) {
                                runOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dbEx != null) {
                                            // Items already in inventory; flag the DB
                                            // error so an admin can investigate but
                                            // still report the delivery to the player.
                                            logStorage("collect cleanup", dbEx);
                                        }
                                        if (finalRemaining == 0) {
                                            out.complete(CollectResult.success(finalDelivered));
                                        } else {
                                            out.complete(CollectResult.partial(finalDelivered, finalRemaining));
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        return out;
    }

    /**
     * Fetches a page of ACTIVE non-expired listings for {@code /ah browse}.
     * Uses {@link AuctionConfig#getGuiBrowseDefaultSort} and no search filter
     * — the same default ordering as the GUI market when no per-player sort
     * override is in play.
     */
    public CompletableFuture<BrowsePage> browseListings(int requestedPage) {
        return browseListings(requestedPage, config.getBrowsePageSize(),
                config.getGuiBrowseDefaultSort(), null);
    }

    /**
     * Same as {@link #browseListings(int)} with an explicit page size.
     */
    public CompletableFuture<BrowsePage> browseListings(int requestedPage, int pageSize) {
        return browseListings(requestedPage, pageSize, config.getGuiBrowseDefaultSort(), null);
    }

    /**
     * Browse / search with sort and optional case-insensitive substring filter
     * (seller name, material name, item display name). All heavy work stays
     * on the DB executor.
     */
    public CompletableFuture<BrowsePage> browseListings(final int requestedPage,
                                                        final int pageSize,
                                                        AuctionBrowseSort sort,
                                                        final String searchQuery) {
        if (!active) {
            return CompletableFuture.completedFuture(BrowsePage.empty(requestedPage));
        }
        final AuctionBrowseSort effectiveSort = sort != null ? sort : AuctionBrowseSort.NEWEST;
        final int effectivePageSize = Math.max(1, pageSize);
        final long now = System.currentTimeMillis();
        final CompletableFuture<BrowsePage> out = new CompletableFuture<BrowsePage>();
        final String q = searchQuery == null ? null : searchQuery.trim();
        final boolean useSearch = q != null && !q.isEmpty();
        runDb(new SqlCallable<BrowsePage>() {
            @Override
            public BrowsePage call() throws AuctionStorageException {
                if (config.isDebug()) {
                    plugin.getLogger().info("[AH] browse sort=" + effectiveSort + " search="
                            + useSearch + " page=" + requestedPage);
                }
                if (!useSearch) {
                    int total = storage.countActiveBrowse(now);
                    if (total == 0) {
                        return new BrowsePage(Collections.<AuctionListing>emptyList(), 1, 1, 0, effectivePageSize);
                    }
                    int totalPages = (total + effectivePageSize - 1) / effectivePageSize;
                    int page = Math.max(1, Math.min(requestedPage, totalPages));
                    int offset = (page - 1) * effectivePageSize;
                    List<AuctionListing> rows = storage.getActiveBrowsePage(now, offset,
                            effectivePageSize, effectiveSort);
                    return new BrowsePage(rows, page, totalPages, total, effectivePageSize);
                }
                List<AuctionListing> all = storage.getAllActiveBrowseListings(now);
                List<AuctionListing> filtered = AuctionListingSearch.filter(all, q);
                AuctionListingSearch.sort(filtered, effectiveSort);
                int total = filtered.size();
                if (total == 0) {
                    return new BrowsePage(Collections.<AuctionListing>emptyList(), 1, 1, 0, effectivePageSize);
                }
                int totalPages = (total + effectivePageSize - 1) / effectivePageSize;
                int page = Math.max(1, Math.min(requestedPage, totalPages));
                int from = (page - 1) * effectivePageSize;
                int to = Math.min(from + effectivePageSize, total);
                return new BrowsePage(filtered.subList(from, to), page, totalPages, total,
                        effectivePageSize);
            }
        }).whenComplete(new java.util.function.BiConsumer<BrowsePage, Throwable>() {
            @Override
            public void accept(final BrowsePage page, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("browseListings", ex);
                            out.complete(BrowsePage.empty(requestedPage));
                            return;
                        }
                        out.complete(page);
                    }
                });
            }
        });
        return out;
    }

    /**
     * Removes terminal listing rows older than the configured retention.
     * Never deletes ACTIVE listings or collect storage.
     */
    public CompletableFuture<Integer> adminCleanupOldTerminalListings() {
        if (!active) {
            return CompletableFuture.completedFuture(Integer.valueOf(0));
        }
        final long cutoff = System.currentTimeMillis()
                - config.getCleanupOldListingRetentionDays() * 86_400_000L;
        final CompletableFuture<Integer> out = new CompletableFuture<Integer>();
        runDb(new SqlCallable<Integer>() {
            @Override
            public Integer call() throws AuctionStorageException {
                return storage.deleteTerminalListingsOlderThan(cutoff);
            }
        }).whenComplete(new java.util.function.BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(final Integer n, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("adminCleanupOldTerminalListings", ex);
                            out.complete(Integer.valueOf(0));
                            return;
                        }
                        out.complete(n != null ? n : Integer.valueOf(0));
                    }
                });
            }
        });
        return out;
    }

    /**
     * Single-row variant of {@link #collectAll(Player)}.
     * The dupe-protection contract is identical: full fit deletes the
     * row, partial fit updates the row to the remainder, no fit leaves
     * the row untouched. Items are never dropped, never deleted, never
     * duplicated.
     */
    public CompletableFuture<CollectOneResult> collectOne(final Player player, final long collectId) {
        if (!active) {
            return CompletableFuture.completedFuture(CollectOneResult.failure("auction.disabled"));
        }
        final UUID owner = player.getUniqueId();
        final CompletableFuture<CollectOneResult> out = new CompletableFuture<CollectOneResult>();
        runDb(new SqlCallable<List<AuctionCollectItem>>() {
            @Override
            public List<AuctionCollectItem> call() throws AuctionStorageException {
                return storage.getCollectItemsForOwner(owner);
            }
        }).whenComplete(new java.util.function.BiConsumer<List<AuctionCollectItem>, Throwable>() {
            @Override
            public void accept(final List<AuctionCollectItem> items, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("collectOne/list", ex);
                            out.complete(CollectOneResult.failure("auction.storage-error"));
                            return;
                        }
                        AuctionCollectItem target = null;
                        for (AuctionCollectItem ci : items) {
                            if (ci.collectId() == collectId) {
                                target = ci;
                                break;
                            }
                        }
                        if (target == null) {
                            out.complete(CollectOneResult.notFound());
                            return;
                        }
                        if (!player.isOnline()) {
                            out.complete(CollectOneResult.failure("general.player-only"));
                            return;
                        }
                        ItemStack candidate = target.itemStack().clone();
                        int before = candidate.getAmount();
                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(candidate);
                        if (leftover.isEmpty()) {
                            final long id = target.collectId();
                            runDb(new SqlCallable<Boolean>() {
                                @Override
                                public Boolean call() throws AuctionStorageException {
                                    return storage.deleteCollectItem(id);
                                }
                            }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                                @Override
                                public void accept(final Boolean ok, final Throwable dbEx) {
                                    runOnMain(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dbEx != null) {
                                                logStorage("collectOne/delete", dbEx);
                                            }
                                            out.complete(CollectOneResult.delivered());
                                        }
                                    });
                                }
                            });
                            return;
                        }
                        ItemStack rest = leftover.values().iterator().next();
                        if (rest.getAmount() >= before) {
                            // No room at all - row untouched, nothing was
                            // moved into the inventory.
                            out.complete(CollectOneResult.inventoryFull());
                            return;
                        }
                        final long id = target.collectId();
                        final ItemStack remainder = rest.clone();
                        runDb(new SqlCallable<Boolean>() {
                            @Override
                            public Boolean call() throws AuctionStorageException {
                                return storage.updateCollectItemStack(id, remainder);
                            }
                        }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                            @Override
                            public void accept(final Boolean ok, final Throwable dbEx) {
                                runOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dbEx != null) {
                                            logStorage("collectOne/update", dbEx);
                                        }
                                        out.complete(CollectOneResult.partial(
                                                before - remainder.getAmount(),
                                                remainder.getAmount()));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        return out;
    }

    /**
     * Fetches the player's collect rows for preview. Returns an
     * empty list on storage errors so callers can render a clean
     * "nothing to collect" state instead of an error popup.
     */
    public CompletableFuture<List<AuctionCollectItem>> getCollectItems(final UUID owner) {
        if (!active) {
            return CompletableFuture.completedFuture(Collections.<AuctionCollectItem>emptyList());
        }
        final CompletableFuture<List<AuctionCollectItem>> out = new CompletableFuture<List<AuctionCollectItem>>();
        runDb(new SqlCallable<List<AuctionCollectItem>>() {
            @Override
            public List<AuctionCollectItem> call() throws AuctionStorageException {
                return storage.getCollectItemsForOwner(owner);
            }
        }).whenComplete(new java.util.function.BiConsumer<List<AuctionCollectItem>, Throwable>() {
            @Override
            public void accept(final List<AuctionCollectItem> items, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            logStorage("getCollectItems", ex);
                            out.complete(Collections.<AuctionCollectItem>emptyList());
                            return;
                        }
                        out.complete(filterGuiMarkedCollectRows(owner, items));
                    }
                });
            }
        });
        return out;
    }

    /**
     * Buys the listing identified by {@code listingId} on behalf of
     * {@code buyer}. See class-level Javadoc for the dupe-protection
     * contract. The flow:
     *
     * <ol>
     *     <li>Pre-flight checks (AH active, economy available, listing
     *         exists, status, expiry, own-listing rule, balance).</li>
     *     <li>Atomic claim via {@link AuctionStorage#markSoldIfActive}
     *         - the SQL UPDATE serialises all racing buyers; only one
     *         wins. The same UPDATE writes buyer UUID + name into the
     *         row, so the SOLD state is fully formed in one round-trip.</li>
     *     <li>Withdraw buyer (main thread). On failure, async revert
     *         the claim via {@link AuctionStorage#revertSoldIfBuyer}.</li>
     *     <li>Deposit seller (main thread, net of {@code sale-tax-percent}).
     *         On failure, try to refund the buyer + revert the claim.
     *         If the refund itself fails, we cannot safely revert -
     *         keep the SOLD state, log SEVERE, and proceed with delivery
     *         so the buyer at least gets the item they paid for.</li>
     *     <li>Delivery (main thread). If the buyer's inventory has room
     *         {@code addItem} is used; otherwise the item is parked in
     *         the buyer's collect storage with reason
     *         {@link AuctionCollectReason#PURCHASED_ITEM_INVENTORY_FULL}.
     *         Items are never dropped.</li>
     * </ol>
     */
    public CompletableFuture<BuyResult> buyListing(final Player buyer, final long listingId) {
        if (!active) {
            return CompletableFuture.completedFuture(BuyResult.failure("auction.disabled"));
        }
        // Buying always moves money, so an economy bridge is mandatory
        // here regardless of the require-economy-for-auction-house flag
        // used for selling.
        EconomyBridge bridge = plugin.getEconomyBridge();
        if (!bridge.isAvailable()) {
            return CompletableFuture.completedFuture(BuyResult.failure("auction.economy-required"));
        }
        if (listingId <= 0L) {
            return CompletableFuture.completedFuture(BuyResult.failure("auction.buy-not-found"));
        }

        final UUID buyerId = buyer.getUniqueId();
        final String buyerName = buyer.getName();
        final CompletableFuture<BuyResult> out = new CompletableFuture<BuyResult>();

        runDb(new SqlCallable<java.util.Optional<AuctionListing>>() {
            @Override
            public java.util.Optional<AuctionListing> call() throws AuctionStorageException {
                return storage.getListing(listingId);
            }
        }).whenComplete(new java.util.function.BiConsumer<java.util.Optional<AuctionListing>, Throwable>() {
            @Override
            public void accept(final java.util.Optional<AuctionListing> opt, final Throwable getEx) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (getEx != null) {
                            logStorage("buy/getListing", getEx);
                            out.complete(BuyResult.failure("auction.buy-storage-error"));
                            return;
                        }
                        if (!opt.isPresent()) {
                            out.complete(BuyResult.failure("auction.buy-not-found"));
                            return;
                        }
                        AuctionListing listing = opt.get();
                        long now = System.currentTimeMillis();
                        if (listing.status() == AuctionListingStatus.SOLD) {
                            out.complete(BuyResult.failure("auction.buy-already-sold"));
                            return;
                        }
                        if (listing.status() != AuctionListingStatus.ACTIVE) {
                            out.complete(BuyResult.failure("auction.buy-not-active"));
                            return;
                        }
                        if (listing.expiresAt() <= now) {
                            out.complete(BuyResult.failure("auction.buy-expired"));
                            return;
                        }
                        if (listing.sellerUuid().equals(buyerId)
                                && !config.isAllowOwnPurchase()) {
                            out.complete(BuyResult.failure("auction.buy-own-listing"));
                            return;
                        }
                        // Balance pre-check. Not authoritative on its own
                        // (the withdraw can still fail if the balance moves
                        // between the check and the withdraw) but cheap
                        // enough to avoid claiming a listing the buyer
                        // obviously cannot afford.
                        EconomyBridge economyBridge = plugin.getEconomyBridge();
                        if (!economyBridge.hasBalance(buyerId, listing.price())) {
                            out.complete(BuyResult.failure("auction.buy-not-enough-money"));
                            return;
                        }
                        completeBuyAfterChecks(buyer, buyerId, buyerName, listing, out);
                    }
                });
            }
        });
        return out;
    }

    private void completeBuyAfterChecks(final Player buyer,
                                        final UUID buyerId,
                                        final String buyerName,
                                        final AuctionListing listing,
                                        final CompletableFuture<BuyResult> out) {
        long now = System.currentTimeMillis();
        runDb(new SqlCallable<Boolean>() {
            @Override
            public Boolean call() throws AuctionStorageException {
                return storage.markSoldIfActive(
                        listing.listingId(), buyerId, buyerName, now);
            }
        }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
            @Override
            public void accept(final Boolean claimed, final Throwable claimEx) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (claimEx != null) {
                            logStorage("buy/markSoldIfActive", claimEx);
                            out.complete(BuyResult.failure("auction.buy-storage-error"));
                            return;
                        }
                        if (!Boolean.TRUE.equals(claimed)) {
                            // Someone else won the race, or the listing expired
                            // between the pre-check and the UPDATE.
                            out.complete(BuyResult.failure("auction.buy-already-sold"));
                            return;
                        }

                        EconomyBridge bridge = plugin.getEconomyBridge();
                        double price = listing.price();
                        EconomyTransactionResult withdraw = bridge.withdraw(
                                buyerId, price, "AH purchase #" + listing.listingId());
                        if (!withdraw.success()) {
                            // Roll the claim back so the seller's listing
                            // is on the market again. revert is best-effort:
                            // if it fails the SOLD row sits there until an
                            // admin deals with it - but the buyer never lost
                            // money in this branch.
                            runDb(new SqlCallable<Boolean>() {
                                @Override
                                public Boolean call() throws AuctionStorageException {
                                    return storage.revertSoldIfBuyer(listing.listingId(), buyerId);
                                }
                            }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                                @Override
                                public void accept(final Boolean reverted, final Throwable revEx) {
                                    if (revEx != null) {
                                        logStorage("buy/revertAfterWithdrawFail", revEx);
                                    }
                                }
                            });
                            String key = "economy.insufficient-funds".equals(withdraw.reasonKey())
                                    ? "auction.buy-not-enough-money"
                                    : "auction.buy-economy-failed";
                            out.complete(BuyResult.failure(key));
                            return;
                        }

                        // Sale tax: gross stays with the buyer-debited
                        // economy; only the net sellerPayout is deposited
                        // to the seller. Tax never lands in any account -
                        // it is simply not paid out, which is the standard
                        // behavior of EssentialsX/CMI auction implementations.
                        double tax = roundCents(price * config.getSaleTaxPercent() / 100.0D);
                        double sellerPayout = Math.max(0.0D, roundCents(price - tax));

                        EconomyTransactionResult deposit = bridge.deposit(
                                listing.sellerUuid(), sellerPayout,
                                "AH payout #" + listing.listingId());
                        if (!deposit.success()) {
                            handleSellerDepositFailure(buyer, buyerId, listing, price, tax, sellerPayout, out);
                            return;
                        }

                        deliverPurchasedItem(buyer, buyerId, listing, price, tax, sellerPayout, out);
                    }
                });
            }
        });
    }

    /**
     * Recovery path for "buyer was charged but seller could not be paid".
     * Tries to refund the buyer and put the listing back on the market.
     * If even the refund fails, we keep SOLD + deliver the item: the
     * buyer paid, so they get the goods, and the seller needs admin
     * help to receive their payout.
     */
    private void handleSellerDepositFailure(final Player buyer,
                                            final UUID buyerId,
                                            final AuctionListing listing,
                                            final double price,
                                            final double tax,
                                            final double sellerPayout,
                                            final CompletableFuture<BuyResult> out) {
        EconomyBridge bridge = plugin.getEconomyBridge();
        EconomyTransactionResult refund = bridge.deposit(
                buyerId, price, "AH purchase refund (seller payout failed) #" + listing.listingId());
        if (refund.success()) {
            runDb(new SqlCallable<Boolean>() {
                @Override
                public Boolean call() throws AuctionStorageException {
                    return storage.revertSoldIfBuyer(listing.listingId(), buyerId);
                }
            }).whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                @Override
                public void accept(final Boolean reverted, final Throwable revEx) {
                    if (revEx != null) {
                        logStorage("buy/revertAfterSellerDepositFail", revEx);
                    }
                }
            });
            out.complete(BuyResult.failure("auction.seller-payout-failed"));
            return;
        }
        // Refund failed too: do not strand the buyer. They paid in full,
        // so they get the item. Log SEVERE so an admin can resolve the
        // unpaid seller payout manually.
        plugin.getLogger().severe("[AH] Seller payout AND buyer refund failed for listing #"
                + listing.listingId() + " seller=" + listing.sellerName()
                + " buyer=" + buyer.getName()
                + " price=" + formatMoney(price)
                + " payout=" + formatMoney(sellerPayout)
                + " - delivering item to buyer; manual admin intervention required.");
        deliverPurchasedItem(buyer, buyerId, listing, price, tax, sellerPayout, out);
    }

    private void deliverPurchasedItem(final Player buyer,
                                      final UUID buyerId,
                                      final AuctionListing listing,
                                      final double price,
                                      final double tax,
                                      final double sellerPayout,
                                      final CompletableFuture<BuyResult> out) {
        if (!buyer.isOnline()) {
            // Buyer logged off between paying and delivery - park the
            // whole item in their collect storage.
            parkPurchaseInCollect(buyer, buyerId, listing, price, tax, sellerPayout, out);
            return;
        }
        ItemStack delivery = listing.itemStack().clone();
        int before = delivery.getAmount();
        Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(delivery);
        if (leftover.isEmpty()) {
            if (config.isDebug()) {
                plugin.getLogger().info("[AH] Buyer " + buyer.getName()
                        + " bought #" + listing.listingId()
                        + " for " + formatMoney(price));
            }
            out.complete(BuyResult.success(listing, price, tax, sellerPayout, false));
            return;
        }
        // Partial or no-room delivery. Park the leftover in collect
        // storage with the buyer-purchase reason. We never call
        // delete-and-retry on the already-delivered slice because the
        // player legitimately owns those items now (they paid full price).
        ItemStack remainder = leftover.values().iterator().next();
        int deliveredAmount = before - remainder.getAmount();
        if (config.isDebug()) {
            plugin.getLogger().info("[AH] Buyer " + buyer.getName()
                    + " bought #" + listing.listingId()
                    + "; " + deliveredAmount + "/" + before
                    + " delivered, remainder to collect storage.");
        }
        final ItemStack toCollect = remainder.clone();
        runDb(new SqlCallable<Void>() {
            @Override
            public Void call() throws AuctionStorageException {
                storage.insertCollectItem(
                        buyerId,
                        toCollect,
                        AuctionCollectReason.PURCHASED_ITEM_INVENTORY_FULL,
                        System.currentTimeMillis(),
                        listing.listingId());
                return null;
            }
        }).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            @Override
            public void accept(final Void ignored, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            // Catastrophic: buyer paid but we cannot store the
                            // leftover. Log SEVERE with the serialised description
                            // so an admin can recover it.
                            plugin.getLogger().severe("[AH] Could not park purchase remainder for "
                                    + buyer.getName() + " listing #" + listing.listingId()
                                    + " item=" + toCollect.getType() + " amount=" + toCollect.getAmount()
                                    + " - " + ex.getMessage());
                        }
                        out.complete(BuyResult.success(listing, price, tax, sellerPayout, true));
                    }
                });
            }
        });
    }

    private void parkPurchaseInCollect(final Player buyer,
                                       final UUID buyerId,
                                       final AuctionListing listing,
                                       final double price,
                                       final double tax,
                                       final double sellerPayout,
                                       final CompletableFuture<BuyResult> out) {
        final ItemStack toCollect = listing.itemStack().clone();
        runDb(new SqlCallable<Void>() {
            @Override
            public Void call() throws AuctionStorageException {
                storage.insertCollectItem(
                        buyerId,
                        toCollect,
                        AuctionCollectReason.PURCHASED_ITEM_INVENTORY_FULL,
                        System.currentTimeMillis(),
                        listing.listingId());
                return null;
            }
        }).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            @Override
            public void accept(final Void ignored, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        if (ex != null) {
                            plugin.getLogger().severe("[AH] Offline buyer " + buyer.getName()
                                    + " bought #" + listing.listingId()
                                    + " but could not store item: " + ex.getMessage());
                        }
                        out.complete(BuyResult.success(listing, price, tax, sellerPayout, true));
                    }
                });
            }
        });
    }

    public CompletableFuture<Stats> getStats() {
        final CompletableFuture<Stats> out = new CompletableFuture<Stats>();
        if (!active) {
            EconomyBridge bridge = plugin.getEconomyBridge();
            out.complete(new Stats(
                    false,
                    inactiveReason != null ? inactiveReason : "disabled",
                    config != null ? storageDisplayType : "-",
                    0, 0, 0, 0, 0,
                    config != null ? config.getSaleTaxPercent() : 0.0D,
                    "Vault",
                    bridge.providerName(),
                    bridge.isAvailable()));
            return out;
        }
        runDb(new SqlCallable<int[]>() {
            @Override
            public int[] call() throws AuctionStorageException {
                int activeCount = storage.countListingsByStatus(AuctionListingStatus.ACTIVE);
                int soldCount = storage.countListingsByStatus(AuctionListingStatus.SOLD);
                int expiredCount = storage.countListingsByStatus(AuctionListingStatus.EXPIRED);
                int cancelledCount = storage.countListingsByStatus(AuctionListingStatus.CANCELLED);
                int collect = storage.countCollectItems();
                return new int[]{activeCount, soldCount, expiredCount, cancelledCount, collect};
            }
        }).whenComplete(new java.util.function.BiConsumer<int[], Throwable>() {
            @Override
            public void accept(final int[] counts, final Throwable ex) {
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        EconomyBridge bridge = plugin.getEconomyBridge();
                        if (ex != null) {
                            logStorage("getStats", ex);
                            out.complete(new Stats(
                                    true, "ok", storageDisplayType,
                                    -1, -1, -1, -1, -1,
                                    config.getSaleTaxPercent(),
                                    "Vault",
                                    bridge.providerName(),
                                    bridge.isAvailable()));
                            return;
                        }
                        out.complete(new Stats(
                                true, "ok", storageDisplayType,
                                counts[0], counts[1], counts[2], counts[3], counts[4],
                                config.getSaleTaxPercent(),
                                "Vault",
                                bridge.providerName(),
                                bridge.isAvailable()));
                    }
                });
            }
        });
        return out;
    }

    private static double roundCents(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    // -------------------------------------------------- package-private helpers

    /**
     * Used by {@link AuctionExpiryService} to drive the expiry batch.
     * The call returns synchronously on the executor thread because
     * the service already hops to the executor; the contract is that
     * callers must already be off the main thread.
     */
    int runExpiryPass() {
        if (!active) return 0;
        long now = System.currentTimeMillis();
        int moved = 0;
        try {
            List<AuctionListing> expired = storage.getExpiredActiveListings(
                    now, EXPIRY_BATCH_LIMIT);
            for (AuctionListing listing : expired) {
                // The transition guard makes this idempotent: if
                // another pass already flipped the status, we skip
                // the collect insert entirely.
                boolean transitioned = storage.transitionListingStatus(
                        listing.listingId(),
                        AuctionListingStatus.ACTIVE,
                        AuctionListingStatus.EXPIRED);
                if (!transitioned) continue;
                storage.insertCollectItem(
                        listing.sellerUuid(),
                        listing.itemStack(),
                        AuctionCollectReason.EXPIRED_LISTING,
                        now,
                        listing.listingId());
                moved++;
            }
        } catch (AuctionStorageException ex) {
            logStorage("expiry pass", ex);
        }
        return moved;
    }

    ExecutorService dbExecutor() {
        return dbExecutor;
    }

    boolean isDebug() {
        return config != null && config.isDebug();
    }

    // ------------------------------------------------------------- private utils

    /** Submits a DB call to the executor and returns a future for the result. */
    private <T> CompletableFuture<T> runDb(final SqlCallable<T> work) {
        final CompletableFuture<T> f = new CompletableFuture<T>();
        if (dbExecutor == null) {
            f.completeExceptionally(new AuctionStorageException(
                    "DB executor not initialised"));
            return f;
        }
        dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    f.complete(work.call());
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                }
            }
        });
        return f;
    }

    /** Runs {@code task} on the main server thread immediately or via the scheduler. */
    private void runOnMain(final Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private void restoreAfterFailure(final Player seller,
                                     final ItemStack snapshot,
                                     final double fee,
                                     final boolean charged) {
        // Refund first. If refunding fails we still try to return the
        // item; the player only loses the fee, which is logged below.
        if (charged) {
            EconomyTransactionResult refund = plugin.getEconomyBridge()
                    .deposit(seller.getUniqueId(), fee, "AH listing fee refund (storage error)");
            if (!refund.success()) {
                plugin.getLogger().severe("[AH] Refund failed for " + seller.getName()
                        + " amount=" + formatMoney(fee) + " reason=" + refund.reasonKey());
            }
        }
        if (seller.isOnline()) {
            PlayerInventory inv = seller.getInventory();
            ItemStack current = inv.getItemInMainHand();
            if (current == null || current.getType() == Material.AIR) {
                inv.setItemInMainHand(snapshot);
                return;
            }
            // Hand re-occupied (another item picked up while we were
            // talking to the DB). Park in collect storage instead.
        }
        runDb(new SqlCallable<Void>() {
            @Override
            public Void call() throws AuctionStorageException {
                storage.insertCollectItem(
                        seller.getUniqueId(),
                        snapshot,
                        AuctionCollectReason.SYSTEM_RETURN,
                        System.currentTimeMillis(),
                        null);
                return null;
            }
        }).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            @Override
            public void accept(final Void ignored, final Throwable ex) {
                if (ex != null) {
                    plugin.getLogger().severe("[AH] Failed to park rescued item for "
                            + seller.getName() + " in collect storage: "
                            + ex.getMessage() + " (item type=" + snapshot.getType()
                            + " amount=" + snapshot.getAmount() + ")");
                }
            }
        });
    }

    /**
     * Same as {@link #restoreAfterFailure(Player, ItemStack, double, boolean)}
     * for escrow listings: returns the snapshot via {@link #safeReturnItemOrCollect}
     * instead of forcing the main hand.
     */
    private void restoreAfterFailureWithoutMainHand(final Player seller,
                                                    final ItemStack snapshot,
                                                    final double fee,
                                                    final boolean charged) {
        if (charged) {
            EconomyTransactionResult refund = plugin.getEconomyBridge()
                    .deposit(seller.getUniqueId(), fee, "AH listing fee refund (storage error)");
            if (!refund.success()) {
                plugin.getLogger().severe("[AH] Refund failed for " + seller.getName()
                        + " amount=" + formatMoney(fee) + " reason=" + refund.reasonKey());
            }
        }
        if (seller.isOnline()) {
            safeReturnItemOrCollect(seller, snapshot.clone());
        } else {
            parkGuiReturnedItem(seller.getUniqueId(), snapshot.clone());
        }
    }

    private List<AuctionCollectItem> filterGuiMarkedCollectRows(UUID owner, List<AuctionCollectItem> fromDb) {
        return fromDb;
    }

    /**
     * Returns {@code stack} to an online player's inventory (empty main
     * hand preferred, then {@link PlayerInventory#addItem(ItemStack...)}).
     * Any remainder is written to collect storage with {@link
     * AuctionCollectReason#SYSTEM_RETURN}. Does not touch economy.
     */
    public void safeReturnItemOrCollect(Player player, ItemStack stack) {
        if (!active || stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return;
        }
        if (!player.isOnline()) {
            parkGuiReturnedItem(player.getUniqueId(), stack.clone());
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack hand = inv.getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            inv.setItemInMainHand(stack.clone());
            return;
        }
        Map<Integer, ItemStack> left = inv.addItem(stack.clone());
        if (left.isEmpty()) {
            return;
        }
        parkGuiReturnedItem(player.getUniqueId(), left.values().iterator().next());
    }

    private void parkGuiReturnedItem(final UUID owner, final ItemStack stack) {
        runDb(new SqlCallable<Void>() {
            @Override
            public Void call() throws AuctionStorageException {
                storage.insertCollectItem(
                        owner,
                        stack,
                        AuctionCollectReason.SYSTEM_RETURN,
                        System.currentTimeMillis(),
                        null);
                return null;
            }
        }).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            @Override
            public void accept(final Void ignored, final Throwable ex) {
                if (ex != null) {
                    plugin.getLogger().severe("[AH] Failed to park returned item for "
                            + owner + ": " + ex.getMessage());
                }
            }
        });
    }

    private boolean isInternalGuiItem(ItemStack stack) {
        return false;
    }

    private static boolean isListableMaterial(Material type) {
        return type != Material.AIR && type.getMaxStackSize() > 0;
    }

    private static Map<String, String> singlePlaceholder(String key, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        return map;
    }

    private void logStorage(String op, Throwable ex) {
        plugin.getLogger().log(Level.WARNING, "[AH] Storage operation '" + op + "' failed", ex);
    }

    private boolean requireEconomyForAuctionHouse() {
        return plugin.getConfig().getBoolean("economy.require-vault", true);
    }

    private String formatMoney(double amount) {
        EconomyBridge bridge = plugin.getEconomyBridge();
        try {
            return bridge.format(amount);
        } catch (Throwable t) {
            return String.format(Locale.ROOT, "%.2f", amount);
        }
    }

    public String formatPrice(double amount) {
        return formatMoney(amount);
    }

    // ------------------------------------------------------------ result types

    /**
     * Output of {@link #createListing(Player, double)}.
     */
    public abstract static class ListingCreateResult {

        private ListingCreateResult() {
        }

        public static final class Success extends ListingCreateResult {
            private final long listingId;
            private final double price;
            private final ItemStack snapshot;

            public Success(long listingId, double price, ItemStack snapshot) {
                this.listingId = listingId;
                this.price = price;
                this.snapshot = snapshot;
            }

            public long listingId() {
                return listingId;
            }

            public double price() {
                return price;
            }

            public ItemStack snapshot() {
                return snapshot;
            }
        }

        public static final class Failure extends ListingCreateResult {
            private final String messageKey;
            private final Map<String, String> placeholders;

            public Failure(String messageKey, Map<String, String> placeholders) {
                this.messageKey = messageKey;
                this.placeholders = placeholders;
            }

            public String messageKey() {
                return messageKey;
            }

            public Map<String, String> placeholders() {
                return placeholders;
            }
        }

        public static ListingCreateResult success(long id, double price, ItemStack snapshot) {
            return new Success(id, price, snapshot);
        }

        public static ListingCreateResult failure(String key) {
            return new Failure(key, Collections.<String, String>emptyMap());
        }

        public static ListingCreateResult failure(String key, Map<String, String> placeholders) {
            return new Failure(key, placeholders);
        }
    }

    public abstract static class CancelResult {

        private CancelResult() {
        }

        public static final class Success extends CancelResult {
            private final AuctionListing listing;

            public Success(AuctionListing listing) {
                this.listing = listing;
            }

            public AuctionListing listing() {
                return listing;
            }
        }

        public static final class Failure extends CancelResult {
            private final String messageKey;

            public Failure(String messageKey) {
                this.messageKey = messageKey;
            }

            public String messageKey() {
                return messageKey;
            }
        }

        public static CancelResult success(AuctionListing listing) {
            return new Success(listing);
        }

        public static CancelResult failure(String key) {
            return new Failure(key);
        }
    }

    public abstract static class CollectResult {

        private CollectResult() {
        }

        public static final class Empty extends CollectResult {
        }

        public static final class Success extends CollectResult {
            private final int delivered;

            public Success(int delivered) {
                this.delivered = delivered;
            }

            public int delivered() {
                return delivered;
            }
        }

        public static final class Partial extends CollectResult {
            private final int delivered;
            private final int remaining;

            public Partial(int delivered, int remaining) {
                this.delivered = delivered;
                this.remaining = remaining;
            }

            public int delivered() {
                return delivered;
            }

            public int remaining() {
                return remaining;
            }
        }

        public static final class Failure extends CollectResult {
            private final String messageKey;

            public Failure(String messageKey) {
                this.messageKey = messageKey;
            }

            public String messageKey() {
                return messageKey;
            }
        }

        public static CollectResult empty() {
            return new Empty();
        }

        public static CollectResult success(int delivered) {
            return new Success(delivered);
        }

        public static CollectResult partial(int delivered, int remaining) {
            return new Partial(delivered, remaining);
        }

        public static CollectResult failure(String key) {
            return new Failure(key);
        }
    }

    /** Output of {@link #collectOne(Player, long)}. */
    public abstract static class CollectOneResult {

        private CollectOneResult() {
        }

        public static final class Delivered extends CollectOneResult {
        }

        public static final class Partial extends CollectOneResult {
            private final int deliveredAmount;
            private final int remainingAmount;

            public Partial(int deliveredAmount, int remainingAmount) {
                this.deliveredAmount = deliveredAmount;
                this.remainingAmount = remainingAmount;
            }

            public int deliveredAmount() {
                return deliveredAmount;
            }

            public int remainingAmount() {
                return remainingAmount;
            }
        }

        public static final class InventoryFull extends CollectOneResult {
        }

        public static final class NotFound extends CollectOneResult {
        }

        public static final class Failure extends CollectOneResult {
            private final String messageKey;

            public Failure(String messageKey) {
                this.messageKey = messageKey;
            }

            public String messageKey() {
                return messageKey;
            }
        }

        public static CollectOneResult delivered() {
            return new Delivered();
        }

        public static CollectOneResult partial(int delivered, int remaining) {
            return new Partial(delivered, remaining);
        }

        public static CollectOneResult inventoryFull() {
            return new InventoryFull();
        }

        public static CollectOneResult notFound() {
            return new NotFound();
        }

        public static CollectOneResult failure(String key) {
            return new Failure(key);
        }
    }

    /** Diagnostic snapshot served to {@code /ah admin info}. */
    public static final class Stats {
        private final boolean active;
        private final String inactiveReason;
        private final String storageType;
        private final int activeListings;
        private final int soldListings;
        private final int expiredListings;
        private final int cancelledListings;
        private final int collectItems;
        private final double saleTaxPercent;
        private final String economyBridge;
        private final String economyProvider;
        private final boolean economyAvailable;

        public Stats(boolean active,
                     String inactiveReason,
                     String storageType,
                     int activeListings,
                     int soldListings,
                     int expiredListings,
                     int cancelledListings,
                     int collectItems,
                     double saleTaxPercent,
                     String economyBridge,
                     String economyProvider,
                     boolean economyAvailable) {
            this.active = active;
            this.inactiveReason = inactiveReason;
            this.storageType = storageType;
            this.activeListings = activeListings;
            this.soldListings = soldListings;
            this.expiredListings = expiredListings;
            this.cancelledListings = cancelledListings;
            this.collectItems = collectItems;
            this.saleTaxPercent = saleTaxPercent;
            this.economyBridge = economyBridge;
            this.economyProvider = economyProvider;
            this.economyAvailable = economyAvailable;
        }

        public boolean active() {
            return active;
        }

        public String inactiveReason() {
            return inactiveReason;
        }

        public String storageType() {
            return storageType;
        }

        public int activeListings() {
            return activeListings;
        }

        public int soldListings() {
            return soldListings;
        }

        public int expiredListings() {
            return expiredListings;
        }

        public int cancelledListings() {
            return cancelledListings;
        }

        public int collectItems() {
            return collectItems;
        }

        public double saleTaxPercent() {
            return saleTaxPercent;
        }

        public String economyBridge() {
            return economyBridge;
        }

        public String economyProvider() {
            return economyProvider;
        }

        public boolean economyAvailable() {
            return economyAvailable;
        }
    }

    /**
     * Output of {@link #browseListings(int)}. {@code page} and
     * {@code totalPages} are 1-indexed; the empty-market state still
     * reports {@code totalPages=1} so the message renders cleanly.
     */
    public static final class BrowsePage {
        private final List<AuctionListing> listings;
        private final int page;
        private final int totalPages;
        private final int totalListings;
        private final int pageSize;

        public BrowsePage(List<AuctionListing> listings,
                          int page,
                          int totalPages,
                          int totalListings,
                          int pageSize) {
            this.listings = listings;
            this.page = page;
            this.totalPages = totalPages;
            this.totalListings = totalListings;
            this.pageSize = pageSize;
        }

        public List<AuctionListing> listings() {
            return listings;
        }

        public int page() {
            return page;
        }

        public int totalPages() {
            return totalPages;
        }

        public int totalListings() {
            return totalListings;
        }

        public int pageSize() {
            return pageSize;
        }

        public static BrowsePage empty(int requestedPage) {
            return new BrowsePage(Collections.<AuctionListing>emptyList(),
                    Math.max(1, requestedPage), 1, 0, 0);
        }
    }

    /**
     * Output of {@link #buyListing(Player, long)}. Success carries the
     * full economic summary so the command layer can render a single
     * confirmation line plus an optional sale-tax info line.
     */
    public abstract static class BuyResult {

        private BuyResult() {
        }

        public static final class Success extends BuyResult {
            private final AuctionListing listing;
            private final double price;
            private final double tax;
            private final double sellerPayout;
            private final boolean inventoryFull;

            public Success(AuctionListing listing,
                           double price,
                           double tax,
                           double sellerPayout,
                           boolean inventoryFull) {
                this.listing = listing;
                this.price = price;
                this.tax = tax;
                this.sellerPayout = sellerPayout;
                this.inventoryFull = inventoryFull;
            }

            public AuctionListing listing() {
                return listing;
            }

            public double price() {
                return price;
            }

            public double tax() {
                return tax;
            }

            public double sellerPayout() {
                return sellerPayout;
            }

            public boolean inventoryFull() {
                return inventoryFull;
            }
        }

        public static final class Failure extends BuyResult {
            private final String messageKey;
            private final Map<String, String> placeholders;

            public Failure(String messageKey, Map<String, String> placeholders) {
                this.messageKey = messageKey;
                this.placeholders = placeholders;
            }

            public String messageKey() {
                return messageKey;
            }

            public Map<String, String> placeholders() {
                return placeholders;
            }
        }

        public static BuyResult success(AuctionListing listing,
                                        double price,
                                        double tax,
                                        double sellerPayout,
                                        boolean inventoryFull) {
            return new Success(listing, price, tax, sellerPayout, inventoryFull);
        }

        public static BuyResult failure(String key) {
            return new Failure(key, Collections.<String, String>emptyMap());
        }

        public static BuyResult failure(String key, Map<String, String> placeholders) {
            return new Failure(key, placeholders);
        }
    }

    /** Internal callable that may throw a {@link AuctionStorageException}. */
    private interface SqlCallable<T> {
        T call() throws AuctionStorageException;
    }

    private static final class UpdatePartial {
        private final long collectId;
        private final ItemStack newStack;

        UpdatePartial(long collectId, ItemStack newStack) {
            this.collectId = collectId;
            this.newStack = newStack;
        }

        long collectId() {
            return collectId;
        }

        ItemStack newStack() {
            return newStack;
        }
    }
}
