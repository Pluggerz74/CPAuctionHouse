package de.crafterspoint.cpauctionhouse.auction.gui;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.auction.AuctionCollectItem;
import de.crafterspoint.cpauctionhouse.auction.AuctionConfig;
import de.crafterspoint.cpauctionhouse.auction.AuctionHouseManager;
import de.crafterspoint.cpauctionhouse.auction.AuctionListing;
import de.crafterspoint.cpauctionhouse.auction.AuctionPermission;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import de.crafterspoint.cpauctionhouse.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates CPAuctionHouse GUI screens using session slot actions (no PDC).
 */
public final class AuctionGuiManager {

    private static final int MAIN_SIZE = 27;
    private static final int MAIN_BROWSE = 11;
    private static final int MAIN_SELL = 13;
    private static final int MAIN_LISTINGS = 15;
    private static final int MAIN_COLLECT = 20;
    private static final int MAIN_SEARCH = 22;
    private static final int MAIN_CLOSE = 26;

    private static final int PAGED_SIZE = 54;
    private static final int PAGED_GRID = 45;
    private static final int NAV_BACK = 45;
    private static final int NAV_REFRESH = 46;
    private static final int NAV_PREV = 48;
    private static final int NAV_PAGE = 49;
    private static final int NAV_NEXT = 50;
    private static final int NAV_CLOSE = 53;
    private static final int NAV_COLLECT_ALL = 49;

    private static final int CONFIRM_SIZE = 27;
    private static final int CONFIRM_YES = 11;
    private static final int CONFIRM_ITEM = 13;
    private static final int CONFIRM_NO = 15;

    private static final int HELP_SIZE = 27;
    private static final int HELP_CENTER = 13;
    private static final int HELP_BACK = 22;

    private final CPAuctionHousePlugin plugin;
    private final AuctionHouseManager auction;
    private final MessageService messages;
    private final AuctionGuiItemFactory items;

    private final Map<UUID, AuctionGuiSession> sessions = new ConcurrentHashMap<UUID, AuctionGuiSession>();
    private final Map<UUID, Long> lastRefreshMs = new ConcurrentHashMap<UUID, Long>();

    public AuctionGuiManager(CPAuctionHousePlugin plugin, AuctionHouseManager auction) {
        this.plugin = plugin;
        this.auction = auction;
        this.messages = plugin.getMessageService();
        this.items = new AuctionGuiItemFactory(messages, auction);
    }

    public boolean isEnabled() {
        AuctionConfig cfg = auction.getConfig();
        return cfg != null && cfg.isGuiEnabled();
    }

    public void reload() {
        closeAll();
        lastRefreshMs.clear();
    }

    public void closeAll() {
        for (UUID id : new ArrayList<UUID>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        sessions.clear();
    }

    public AuctionGuiSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void openMain(Player player) {
        if (!checkGuiReady(player)) {
            return;
        }
        AuctionGuiSession session = ensureSession(player);
        Inventory inv = buildMain(session);
        show(player, session, AuctionGuiScreen.MAIN, inv, titleFor("auction.gui.title"));
    }

    public void openBrowse(Player player, int page) {
        if (!checkGuiReady(player)) {
            return;
        }
        if (!player.hasPermission(AuctionPermission.BROWSE)) {
            messages.sendPrefixed(player, "auction.no-permission");
            return;
        }
        AuctionGuiSession session = ensureSession(player);
        session.setCurrentPage(page);
        loadBrowse(player, session, page);
    }

    public void openMyListings(Player player, int page) {
        if (!checkGuiReady(player)) {
            return;
        }
        if (!player.hasPermission(AuctionPermission.LISTINGS)) {
            messages.sendPrefixed(player, "auction.no-permission");
            return;
        }
        AuctionGuiSession session = ensureSession(player);
        session.setCurrentPage(page);
        loadMyListings(player, session, page);
    }

    public void openCollect(Player player) {
        if (!checkGuiReady(player)) {
            return;
        }
        if (!player.hasPermission(AuctionPermission.COLLECT)) {
            messages.sendPrefixed(player, "auction.no-permission");
            return;
        }
        AuctionGuiSession session = ensureSession(player);
        loadCollect(player, session);
    }

    public void handleSessionClose(Player player, AuctionGuiSession session) {
        if (session.consumeControlledTransition()) {
            return;
        }
        sessions.remove(player.getUniqueId());
    }

    public void handlePlayerQuit(Player player) {
        sessions.remove(player.getUniqueId());
        lastRefreshMs.remove(player.getUniqueId());
    }

    public void handleClick(Player player, AuctionGuiSession session, int rawSlot) {
        AuctionGuiAction action = session.getAction(rawSlot);
        if (action == null || action == AuctionGuiAction.NONE) {
            return;
        }
        switch (action) {
            case OPEN_BROWSE:
                openBrowse(player, 1);
                break;
            case OPEN_SELL_HELP:
                openSellHelp(player);
                break;
            case OPEN_MY_LISTINGS:
                openMyListings(player, 1);
                break;
            case OPEN_COLLECT:
                openCollect(player);
                break;
            case OPEN_SEARCH_HELP:
                openSearchHelp(player);
                break;
            case BACK_TO_MAIN:
            case BACK:
                openMain(player);
                break;
            case CLOSE:
                player.closeInventory();
                break;
            case REFRESH_BROWSE:
                refreshBrowse(player, session);
                break;
            case PREV_PAGE:
                if (session.getCurrentPage() > 1) {
                    navigatePaged(player, session, session.getCurrentPage() - 1);
                }
                break;
            case NEXT_PAGE:
                if (session.getCurrentPage() < session.getTotalPages()) {
                    navigatePaged(player, session, session.getCurrentPage() + 1);
                }
                break;
            case OPEN_BUY_CONFIRM:
                openBuyConfirm(player, session, session.getListingId(rawSlot));
                break;
            case CONFIRM_BUY:
                confirmBuy(player, session);
                break;
            case BACK_FROM_BUY_CONFIRM:
                openBrowse(player, session.getCurrentPage());
                break;
            case OPEN_CANCEL_CONFIRM:
                openCancelConfirm(player, session, session.getListingId(rawSlot));
                break;
            case CONFIRM_CANCEL:
                confirmCancel(player, session);
                break;
            case BACK_FROM_CANCEL_CONFIRM:
                openMyListings(player, session.getCurrentPage());
                break;
            case COLLECT_ALL:
                collectAll(player, session);
                break;
            default:
                break;
        }
    }

    private void navigatePaged(Player player, AuctionGuiSession session, int page) {
        if (session.getCurrentScreen() == AuctionGuiScreen.BROWSE) {
            openBrowse(player, page);
        } else if (session.getCurrentScreen() == AuctionGuiScreen.MY_LISTINGS) {
            openMyListings(player, page);
        }
    }

    private void refreshBrowse(Player player, AuctionGuiSession session) {
        AuctionConfig cfg = auction.getConfig();
        long cooldown = cfg != null ? cfg.getGuiRefreshCooldownMs() : 750L;
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRefreshMs.get(id);
        if (last != null && now - last.longValue() < cooldown) {
            return;
        }
        lastRefreshMs.put(id, Long.valueOf(now));
        loadBrowse(player, session, session.getCurrentPage());
    }

    private void openSellHelp(Player player) {
        AuctionGuiSession session = ensureSession(player);
        Inventory inv = Bukkit.createInventory(session, HELP_SIZE, titleFor("auction.gui.sell-help-title"));
        session.clearSlotBindings();
        fill(inv);
        ItemStack hand = player.getInventory().getItemInMainHand();
        inv.setItem(HELP_CENTER, items.sellHelpTile(hand));
        inv.setItem(HELP_BACK, items.backButton());
        session.bind(HELP_BACK, AuctionGuiAction.BACK_TO_MAIN);
        show(player, session, AuctionGuiScreen.SELL_HELP, inv, titleFor("auction.gui.sell-help-title"));
    }

    private void openSearchHelp(Player player) {
        AuctionGuiSession session = ensureSession(player);
        Inventory inv = Bukkit.createInventory(session, HELP_SIZE, titleFor("auction.gui.button-search"));
        session.clearSlotBindings();
        fill(inv);
        inv.setItem(HELP_CENTER, items.searchHelpTile());
        inv.setItem(HELP_BACK, items.backButton());
        session.bind(HELP_BACK, AuctionGuiAction.BACK_TO_MAIN);
        show(player, session, AuctionGuiScreen.SEARCH_HELP, inv, titleFor("auction.gui.button-search"));
        messages.sendPrefixed(player, "auction.gui.search-help");
    }

    private void openBuyConfirm(Player player, AuctionGuiSession session, long listingId) {
        AuctionListing listing = session.findVisibleListing(listingId);
        if (listing == null) {
            messages.sendPrefixed(player, "auction.gui.error");
            openBrowse(player, session.getCurrentPage());
            return;
        }
        AuctionConfig cfg = auction.getConfig();
        if (listing.sellerUuid().equals(player.getUniqueId()) && !cfg.isAllowOwnPurchase()) {
            messages.sendPrefixed(player, "auction.buy-own-listing");
            return;
        }
        session.setPendingBuyListing(listing);
        Inventory inv = Bukkit.createInventory(session, CONFIRM_SIZE, titleFor("auction.gui.button-confirm-buy"));
        session.clearSlotBindings();
        fill(inv);
        long now = System.currentTimeMillis();
        inv.setItem(CONFIRM_YES, items.confirmBuyButton());
        inv.setItem(CONFIRM_ITEM, items.confirmPreview(listing, now));
        inv.setItem(CONFIRM_NO, items.backButton());
        session.bind(CONFIRM_YES, AuctionGuiAction.CONFIRM_BUY);
        session.bind(CONFIRM_NO, AuctionGuiAction.BACK_FROM_BUY_CONFIRM);
        show(player, session, AuctionGuiScreen.BUY_CONFIRM, inv, titleFor("auction.gui.button-confirm-buy"));
    }

    private void openCancelConfirm(Player player, AuctionGuiSession session, long listingId) {
        AuctionListing listing = session.findVisibleListing(listingId);
        if (listing == null) {
            messages.sendPrefixed(player, "auction.gui.error");
            openMyListings(player, session.getCurrentPage());
            return;
        }
        session.setPendingCancelListing(listing);
        Inventory inv = Bukkit.createInventory(session, CONFIRM_SIZE, titleFor("auction.gui.button-confirm-cancel"));
        session.clearSlotBindings();
        fill(inv);
        long now = System.currentTimeMillis();
        inv.setItem(CONFIRM_YES, items.confirmCancelButton());
        inv.setItem(CONFIRM_ITEM, items.confirmPreview(listing, now));
        inv.setItem(CONFIRM_NO, items.backButton());
        session.bind(CONFIRM_YES, AuctionGuiAction.CONFIRM_CANCEL);
        session.bind(CONFIRM_NO, AuctionGuiAction.BACK_FROM_CANCEL_CONFIRM);
        show(player, session, AuctionGuiScreen.CANCEL_CONFIRM, inv, titleFor("auction.gui.button-confirm-cancel"));
    }

    private void confirmBuy(final Player player, final AuctionGuiSession session) {
        AuctionListing listing = session.getPendingBuyListing();
        if (listing == null) {
            messages.sendPrefixed(player, "auction.buy-not-found");
            openBrowse(player, session.getCurrentPage());
            return;
        }
        auction.buyListing(player, listing.listingId()).thenAccept(new java.util.function.Consumer<AuctionHouseManager.BuyResult>() {
            @Override
            public void accept(AuctionHouseManager.BuyResult result) {
                if (!player.isOnline()) {
                    return;
                }
                if (result instanceof AuctionHouseManager.BuyResult.Success) {
                    AuctionHouseManager.BuyResult.Success s = (AuctionHouseManager.BuyResult.Success) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("id", Long.toString(s.listing().listingId()));
                    ph.put("item", describeItem(s.listing().itemStack()));
                    ph.put("amount", Integer.toString(s.listing().itemStack().getAmount()));
                    ph.put("price", auction.formatPrice(s.price()));
                    ph.put("tax", auction.formatPrice(s.tax()));
                    ph.put("payout", auction.formatPrice(s.sellerPayout()));
                    ph.put("seller", s.listing().sellerName() != null ? s.listing().sellerName() : "-");
                    messages.sendPrefixed(player, "auction.buy-success", ph);
                    if (s.tax() > 0.0D) {
                        messages.sendPrefixed(player, "auction.sale-tax-info", ph);
                    }
                    if (s.inventoryFull()) {
                        messages.sendPrefixed(player, "auction.buy-inventory-full");
                    }
                } else if (result instanceof AuctionHouseManager.BuyResult.Failure) {
                    AuctionHouseManager.BuyResult.Failure f = (AuctionHouseManager.BuyResult.Failure) result;
                    messages.sendPrefixed(player, f.messageKey(), f.placeholders());
                }
                openBrowse(player, session.getCurrentPage());
            }
        });
    }

    private void confirmCancel(final Player player, final AuctionGuiSession session) {
        AuctionListing listing = session.getPendingCancelListing();
        if (listing == null) {
            messages.sendPrefixed(player, "auction.listing-not-found");
            openMyListings(player, session.getCurrentPage());
            return;
        }
        auction.cancelListing(player.getUniqueId(), listing.listingId(), false)
                .thenAccept(new java.util.function.Consumer<AuctionHouseManager.CancelResult>() {
                    @Override
                    public void accept(AuctionHouseManager.CancelResult result) {
                        if (!player.isOnline()) {
                            return;
                        }
                        if (result instanceof AuctionHouseManager.CancelResult.Success) {
                            AuctionHouseManager.CancelResult.Success s =
                                    (AuctionHouseManager.CancelResult.Success) result;
                            Map<String, String> ph = new HashMap<String, String>();
                            ph.put("id", Long.toString(s.listing().listingId()));
                            messages.sendPrefixed(player, "auction.listing-cancelled", ph);
                        } else if (result instanceof AuctionHouseManager.CancelResult.Failure) {
                            AuctionHouseManager.CancelResult.Failure f =
                                    (AuctionHouseManager.CancelResult.Failure) result;
                            messages.sendPrefixed(player, f.messageKey());
                        }
                        openMyListings(player, session.getCurrentPage());
                    }
                });
    }

    private void collectAll(final Player player, final AuctionGuiSession session) {
        auction.collectAll(player).thenAccept(new java.util.function.Consumer<AuctionHouseManager.CollectResult>() {
            @Override
            public void accept(AuctionHouseManager.CollectResult result) {
                if (!player.isOnline()) {
                    return;
                }
                if (result instanceof AuctionHouseManager.CollectResult.Empty) {
                    messages.sendPrefixed(player, "auction.collect-empty");
                } else if (result instanceof AuctionHouseManager.CollectResult.Success) {
                    AuctionHouseManager.CollectResult.Success s =
                            (AuctionHouseManager.CollectResult.Success) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("count", Integer.toString(s.delivered()));
                    messages.sendPrefixed(player, "auction.collect-success", ph);
                } else if (result instanceof AuctionHouseManager.CollectResult.Partial) {
                    AuctionHouseManager.CollectResult.Partial p =
                            (AuctionHouseManager.CollectResult.Partial) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("count", Integer.toString(p.delivered()));
                    ph.put("remaining", Integer.toString(p.remaining()));
                    messages.sendPrefixed(player, "auction.collect-partial", ph);
                } else if (result instanceof AuctionHouseManager.CollectResult.Failure) {
                    AuctionHouseManager.CollectResult.Failure f =
                            (AuctionHouseManager.CollectResult.Failure) result;
                    messages.sendPrefixed(player, f.messageKey());
                }
                loadCollect(player, session);
            }
        });
    }

    private Inventory buildMain(AuctionGuiSession session) {
        Inventory inv = Bukkit.createInventory(session, MAIN_SIZE, titleFor("auction.gui.title"));
        session.clearSlotBindings();
        fill(inv);
        inv.setItem(MAIN_BROWSE, items.mainBrowseButton());
        inv.setItem(MAIN_SELL, items.mainSellButton());
        inv.setItem(MAIN_LISTINGS, items.mainListingsButton());
        inv.setItem(MAIN_COLLECT, items.mainCollectButton());
        inv.setItem(MAIN_SEARCH, items.mainSearchButton());
        inv.setItem(MAIN_CLOSE, items.closeButton());
        session.bind(MAIN_BROWSE, AuctionGuiAction.OPEN_BROWSE);
        session.bind(MAIN_SELL, AuctionGuiAction.OPEN_SELL_HELP);
        session.bind(MAIN_LISTINGS, AuctionGuiAction.OPEN_MY_LISTINGS);
        session.bind(MAIN_COLLECT, AuctionGuiAction.OPEN_COLLECT);
        session.bind(MAIN_SEARCH, AuctionGuiAction.OPEN_SEARCH_HELP);
        session.bind(MAIN_CLOSE, AuctionGuiAction.CLOSE);
        return inv;
    }

    private void loadBrowse(final Player player, final AuctionGuiSession session, final int page) {
        AuctionConfig cfg = auction.getConfig();
        final int fetchGen = session.beginBrowseFetch();
        Inventory loading = Bukkit.createInventory(session, PAGED_SIZE, titleFor("auction.gui.title"));
        session.clearSlotBindings();
        fill(loading);
        loading.setItem(PAGED_GRID / 2, items.loadingTile());
        session.bind(NAV_BACK, AuctionGuiAction.BACK_TO_MAIN);
        loading.setItem(NAV_BACK, items.backButton());
        show(player, session, AuctionGuiScreen.BROWSE, loading, titleFor("auction.gui.title"));

        auction.browseListings(page, PAGED_GRID, cfg.getGuiBrowseDefaultSort(), null)
                .thenAccept(new java.util.function.Consumer<AuctionHouseManager.BrowsePage>() {
                    @Override
                    public void accept(AuctionHouseManager.BrowsePage browsePage) {
                        if (!player.isOnline()) {
                            return;
                        }
                        if (fetchGen != session.getBrowseFetchGeneration()) {
                            return;
                        }
                        if (sessions.get(player.getUniqueId()) != session) {
                            return;
                        }
                        session.setVisibleListings(browsePage.listings());
                        session.setCurrentPage(browsePage.page());
                        session.setTotalPages(browsePage.totalPages());
                        Inventory inv = buildBrowse(session, browsePage, cfg);
                        show(player, session, AuctionGuiScreen.BROWSE, inv, titleFor("auction.gui.title"));
                    }
                });
    }

    private Inventory buildBrowse(AuctionGuiSession session, AuctionHouseManager.BrowsePage page, AuctionConfig cfg) {
        Inventory inv = Bukkit.createInventory(session, PAGED_SIZE, titleFor("auction.gui.title"));
        session.clearSlotBindings();
        fill(inv);
        long now = System.currentTimeMillis();
        UUID viewer = session.getPlayerId();
        boolean allowOwn = cfg.isAllowOwnPurchase();
        if (page.totalListings() == 0) {
            inv.setItem(PAGED_GRID / 2, items.emptyBrowseTile());
        } else {
            List<AuctionListing> listings = page.listings();
            for (int i = 0; i < listings.size() && i < PAGED_GRID; i++) {
                AuctionListing listing = listings.get(i);
                inv.setItem(i, items.browseListingTile(listing, viewer, allowOwn, now));
                session.bindListing(i, AuctionGuiAction.OPEN_BUY_CONFIRM, listing.listingId());
            }
        }
        bindBrowseNav(session, inv, page.page(), page.totalPages());
        return inv;
    }

    private void bindBrowseNav(AuctionGuiSession session, Inventory inv, int page, int totalPages) {
        inv.setItem(NAV_BACK, items.backButton());
        session.bind(NAV_BACK, AuctionGuiAction.BACK_TO_MAIN);
        inv.setItem(NAV_REFRESH, items.refreshButton());
        session.bind(NAV_REFRESH, AuctionGuiAction.REFRESH_BROWSE);
        if (page > 1) {
            inv.setItem(NAV_PREV, items.previousPageButton());
            session.bind(NAV_PREV, AuctionGuiAction.PREV_PAGE);
        }
        inv.setItem(NAV_PAGE, items.pageIndicator(page, totalPages));
        if (page < totalPages) {
            inv.setItem(NAV_NEXT, items.nextPageButton());
            session.bind(NAV_NEXT, AuctionGuiAction.NEXT_PAGE);
        }
        inv.setItem(NAV_CLOSE, items.closeButton());
        session.bind(NAV_CLOSE, AuctionGuiAction.CLOSE);
    }

    private void loadMyListings(final Player player, final AuctionGuiSession session, final int requestedPage) {
        auction.getActiveListings(player.getUniqueId()).thenAccept(new java.util.function.Consumer<List<AuctionListing>>() {
            @Override
            public void accept(List<AuctionListing> all) {
                if (!player.isOnline()) {
                    return;
                }
                if (sessions.get(player.getUniqueId()) != session) {
                    return;
                }
                int total = all.size();
                int totalPages = Math.max(1, (total + PAGED_GRID - 1) / PAGED_GRID);
                int page = Math.max(1, Math.min(requestedPage, totalPages));
                int from = (page - 1) * PAGED_GRID;
                int to = Math.min(total, from + PAGED_GRID);
                List<AuctionListing> visible = total == 0
                        ? new ArrayList<AuctionListing>()
                        : all.subList(from, to);
                session.setVisibleListings(visible);
                session.setCurrentPage(page);
                session.setTotalPages(totalPages);
                Inventory inv = buildMyListings(session, visible, page, totalPages, total);
                show(player, session, AuctionGuiScreen.MY_LISTINGS, inv, titleFor("auction.gui.button-my-listings"));
            }
        });
    }

    private Inventory buildMyListings(AuctionGuiSession session, List<AuctionListing> visible,
                                      int page, int totalPages, int total) {
        Inventory inv = Bukkit.createInventory(session, PAGED_SIZE,
                titleFor("auction.gui.button-my-listings"));
        session.clearSlotBindings();
        fill(inv);
        long now = System.currentTimeMillis();
        if (total == 0) {
            inv.setItem(PAGED_GRID / 2, items.emptyOwnListingsTile());
        } else {
            for (int i = 0; i < visible.size(); i++) {
                AuctionListing listing = visible.get(i);
                inv.setItem(i, items.ownListingTile(listing, now));
                session.bindListing(i, AuctionGuiAction.OPEN_CANCEL_CONFIRM, listing.listingId());
            }
        }
        inv.setItem(NAV_BACK, items.backButton());
        session.bind(NAV_BACK, AuctionGuiAction.BACK_TO_MAIN);
        if (page > 1) {
            inv.setItem(NAV_PREV, items.previousPageButton());
            session.bind(NAV_PREV, AuctionGuiAction.PREV_PAGE);
        }
        inv.setItem(NAV_PAGE, items.pageIndicator(page, totalPages));
        if (page < totalPages) {
            inv.setItem(NAV_NEXT, items.nextPageButton());
            session.bind(NAV_NEXT, AuctionGuiAction.NEXT_PAGE);
        }
        inv.setItem(NAV_CLOSE, items.closeButton());
        session.bind(NAV_CLOSE, AuctionGuiAction.CLOSE);
        return inv;
    }

    private void loadCollect(final Player player, final AuctionGuiSession session) {
        auction.getCollectItems(player.getUniqueId()).thenAccept(new java.util.function.Consumer<List<AuctionCollectItem>>() {
            @Override
            public void accept(List<AuctionCollectItem> collectItems) {
                if (!player.isOnline()) {
                    return;
                }
                if (sessions.get(player.getUniqueId()) != session) {
                    return;
                }
                List<AuctionCollectItem> visible = collectItems.size() > PAGED_GRID
                        ? collectItems.subList(0, PAGED_GRID)
                        : collectItems;
                session.setVisibleCollect(visible);
                Inventory inv = buildCollect(session, visible, collectItems.size());
                show(player, session, AuctionGuiScreen.COLLECT, inv, titleFor("auction.gui.button-collect"));
            }
        });
    }

    private Inventory buildCollect(AuctionGuiSession session, List<AuctionCollectItem> visible, int total) {
        Inventory inv = Bukkit.createInventory(session, PAGED_SIZE, titleFor("auction.gui.button-collect"));
        session.clearSlotBindings();
        fill(inv);
        long now = System.currentTimeMillis();
        if (total == 0) {
            inv.setItem(PAGED_GRID / 2, items.collectInfoTile());
        } else {
            for (int i = 0; i < visible.size(); i++) {
                inv.setItem(i, items.collectPreviewTile(visible.get(i), now));
            }
            inv.setItem(NAV_COLLECT_ALL, items.collectAllButton());
            session.bind(NAV_COLLECT_ALL, AuctionGuiAction.COLLECT_ALL);
        }
        inv.setItem(NAV_BACK, items.backButton());
        session.bind(NAV_BACK, AuctionGuiAction.BACK_TO_MAIN);
        inv.setItem(NAV_CLOSE, items.closeButton());
        session.bind(NAV_CLOSE, AuctionGuiAction.CLOSE);
        return inv;
    }

    private void show(Player player, AuctionGuiSession session, AuctionGuiScreen screen,
                      Inventory inv, String title) {
        session.markControlledTransition();
        session.setCurrentScreen(screen);
        session.setCurrentInventory(inv);
        session.setInventoryTitle(title);
        player.openInventory(inv);
    }

    private AuctionGuiSession ensureSession(Player player) {
        AuctionGuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            session = new AuctionGuiSession(player.getUniqueId());
            sessions.put(player.getUniqueId(), session);
        }
        return session;
    }

    private boolean checkGuiReady(Player player) {
        if (!isEnabled()) {
            messages.sendPrefixed(player, "auction.disabled");
            return false;
        }
        if (!auction.isActive()) {
            messages.sendPrefixed(player, "auction.disabled");
            return false;
        }
        if (!player.hasPermission(AuctionPermission.BASE)) {
            messages.sendPrefixed(player, "auction.no-permission");
            return false;
        }
        return true;
    }

    private void fill(Inventory inv) {
        AuctionConfig cfg = auction.getConfig();
        if (cfg == null || !cfg.isGuiFillerEnabled()) {
            return;
        }
        ItemStack filler = items.filler(cfg);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private String titleFor(String messageKey) {
        AuctionConfig cfg = auction.getConfig();
        if (cfg != null && cfg.getGuiTitle() != null && cfg.getGuiTitle().trim().length() > 0
                && "auction.gui.title".equals(messageKey)) {
            return Text.colorize(cfg.getGuiTitle());
        }
        return Text.colorize(messages.message(messageKey));
    }

    private static String describeItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return "-";
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        }
        return stack.getType().name();
    }
}
