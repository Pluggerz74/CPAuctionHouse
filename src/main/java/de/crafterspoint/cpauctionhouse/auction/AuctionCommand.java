package de.crafterspoint.cpauctionhouse.auction;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The single {@code /ah} entry point for text-based auction commands.
 */
public final class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = Collections.unmodifiableList(Arrays.asList(
            "sell", "listings", "cancel", "collect", "browse", "search", "buy", "admin", "help"));
    private static final List<String> ADMIN_ACTIONS = Collections.unmodifiableList(Arrays.asList(
            "remove", "info", "reload", "cleanup"));

    private final CPAuctionHousePlugin plugin;
    private final AuctionHouseManager auction;

    public AuctionCommand(CPAuctionHousePlugin plugin, AuctionHouseManager auction) {
        this.plugin = plugin;
        this.auction = auction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessageService();
        if (!sender.hasPermission(AuctionPermission.BASE)) {
            messages.sendPrefixed(sender, "auction.no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("sell".equals(sub)) {
            return handleSell(sender, args);
        }
        if ("listings".equals(sub)) {
            return handleListings(sender);
        }
        if ("cancel".equals(sub)) {
            return handleCancel(sender, args);
        }
        if ("collect".equals(sub)) {
            return handleCollect(sender);
        }
        if ("browse".equals(sub)) {
            return handleBrowse(sender, args);
        }
        if ("search".equals(sub)) {
            return handleSearch(sender, args);
        }
        if ("buy".equals(sub)) {
            return handleBuy(sender, args);
        }
        if ("admin".equals(sub)) {
            return handleAdmin(sender, args);
        }
        if ("help".equals(sub)) {
            sendHelp(sender);
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        m.sendPrefixed(sender, "auction.help");
        m.sendPrefixed(sender, "auction.help-browse");
        m.sendPrefixed(sender, "auction.help-search");
        m.sendPrefixed(sender, "auction.help-buy");
        m.sendPrefixed(sender, "auction.help-sell");
        m.sendPrefixed(sender, "auction.help-listings");
        m.sendPrefixed(sender, "auction.help-cancel");
        m.sendPrefixed(sender, "auction.help-collect");
        if (sender.hasPermission(AuctionPermission.ADMIN)) {
            m.sendPrefixed(sender, "auction.help-admin");
        }
    }

    private boolean handleSell(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!(sender instanceof Player)) {
            m.sendPrefixed(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(AuctionPermission.SELL)) {
            m.sendPrefixed(player, "auction.no-permission");
            return true;
        }
        if (args.length < 2) {
            m.sendPrefixed(player, "auction.help-sell");
            return true;
        }
        AuctionPriceParser.Result parsed = AuctionPriceParser.parse(args[1]);
        if (!parsed.ok()) {
            m.sendPrefixed(player, "auction.invalid-price");
            return true;
        }
        auction.createListing(player, parsed.value()).thenAccept(new java.util.function.Consumer<AuctionHouseManager.ListingCreateResult>() {
            @Override
            public void accept(AuctionHouseManager.ListingCreateResult result) {
                if (result instanceof AuctionHouseManager.ListingCreateResult.Success) {
                    AuctionHouseManager.ListingCreateResult.Success s =
                            (AuctionHouseManager.ListingCreateResult.Success) result;
                    Map<String, String> placeholders = new HashMap<String, String>();
                    placeholders.put("id", Long.toString(s.listingId()));
                    placeholders.put("price", auction.formatPrice(s.price()));
                    placeholders.put("item", describeItem(s.snapshot()));
                    placeholders.put("amount", Integer.toString(s.snapshot().getAmount()));
                    m.sendPrefixed(player, "auction.listing-created", placeholders);
                } else if (result instanceof AuctionHouseManager.ListingCreateResult.Failure) {
                    AuctionHouseManager.ListingCreateResult.Failure f =
                            (AuctionHouseManager.ListingCreateResult.Failure) result;
                    m.sendPrefixed(player, f.messageKey(), f.placeholders());
                }
            }
        });
        return true;
    }

    private boolean handleListings(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        if (!(sender instanceof Player)) {
            m.sendPrefixed(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(AuctionPermission.LISTINGS)) {
            m.sendPrefixed(player, "auction.no-permission");
            return true;
        }
        auction.getActiveListings(player.getUniqueId()).thenAccept(new java.util.function.Consumer<List<AuctionListing>>() {
            @Override
            public void accept(List<AuctionListing> list) {
                if (list.isEmpty()) {
                    m.sendPrefixed(player, "auction.listings-empty");
                    return;
                }
                Map<String, String> header = new HashMap<String, String>();
                header.put("count", Integer.toString(list.size()));
                m.sendPrefixed(player, "auction.listings-header", header);
                long now = System.currentTimeMillis();
                for (AuctionListing listing : list) {
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("id", Long.toString(listing.listingId()));
                    ph.put("item", describeItem(listing.itemStack()));
                    ph.put("amount", Integer.toString(listing.itemStack().getAmount()));
                    ph.put("price", auction.formatPrice(listing.price()));
                    ph.put("time", AuctionTimeFormatter.formatRemaining(listing.remainingMillis(now)));
                    m.sendPrefixed(player, "auction.listings-row", ph);
                }
            }
        });
        return true;
    }

    private boolean handleCancel(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!(sender instanceof Player)) {
            m.sendPrefixed(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(AuctionPermission.CANCEL)) {
            m.sendPrefixed(player, "auction.no-permission");
            return true;
        }
        if (args.length < 2) {
            m.sendPrefixed(player, "auction.help-cancel");
            return true;
        }
        Long id = parseListingId(args[1]);
        if (id == null) {
            m.sendPrefixed(player, "auction.listing-not-found");
            return true;
        }
        auction.cancelListing(player.getUniqueId(), id, false).thenAccept(new java.util.function.Consumer<AuctionHouseManager.CancelResult>() {
            @Override
            public void accept(AuctionHouseManager.CancelResult result) {
                if (result instanceof AuctionHouseManager.CancelResult.Success) {
                    AuctionHouseManager.CancelResult.Success s =
                            (AuctionHouseManager.CancelResult.Success) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("id", Long.toString(s.listing().listingId()));
                    m.sendPrefixed(player, "auction.listing-cancelled", ph);
                } else if (result instanceof AuctionHouseManager.CancelResult.Failure) {
                    AuctionHouseManager.CancelResult.Failure f =
                            (AuctionHouseManager.CancelResult.Failure) result;
                    m.sendPrefixed(player, f.messageKey());
                }
            }
        });
        return true;
    }

    private boolean handleCollect(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        if (!(sender instanceof Player)) {
            m.sendPrefixed(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(AuctionPermission.COLLECT)) {
            m.sendPrefixed(player, "auction.no-permission");
            return true;
        }
        auction.collectAll(player).thenAccept(new java.util.function.Consumer<AuctionHouseManager.CollectResult>() {
            @Override
            public void accept(AuctionHouseManager.CollectResult result) {
                if (result instanceof AuctionHouseManager.CollectResult.Empty) {
                    m.sendPrefixed(player, "auction.collect-empty");
                } else if (result instanceof AuctionHouseManager.CollectResult.Success) {
                    AuctionHouseManager.CollectResult.Success s =
                            (AuctionHouseManager.CollectResult.Success) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("count", Integer.toString(s.delivered()));
                    m.sendPrefixed(player, "auction.collect-success", ph);
                } else if (result instanceof AuctionHouseManager.CollectResult.Partial) {
                    AuctionHouseManager.CollectResult.Partial p =
                            (AuctionHouseManager.CollectResult.Partial) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("count", Integer.toString(p.delivered()));
                    ph.put("remaining", Integer.toString(p.remaining()));
                    m.sendPrefixed(player, "auction.collect-partial", ph);
                } else if (result instanceof AuctionHouseManager.CollectResult.Failure) {
                    AuctionHouseManager.CollectResult.Failure f =
                            (AuctionHouseManager.CollectResult.Failure) result;
                    m.sendPrefixed(player, f.messageKey());
                }
            }
        });
        return true;
    }

    private boolean handleBrowse(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!sender.hasPermission(AuctionPermission.BROWSE)) {
            m.sendPrefixed(sender, "auction.no-permission");
            return true;
        }
        int requestedPage = 1;
        if (args.length >= 2) {
            try {
                requestedPage = Math.max(1, Integer.parseInt(args[1].trim()));
            } catch (NumberFormatException ex) {
                m.sendPrefixed(sender, "auction.help-browse");
                return true;
            }
        }
        final int pageNum = requestedPage;
        auction.browseListings(pageNum).thenAccept(new java.util.function.Consumer<AuctionHouseManager.BrowsePage>() {
            @Override
            public void accept(AuctionHouseManager.BrowsePage page) {
                renderBrowsePage(sender, m, page);
            }
        });
        return true;
    }

    private boolean handleSearch(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!sender.hasPermission(AuctionPermission.BROWSE)) {
            m.sendPrefixed(sender, "auction.no-permission");
            return true;
        }
        if (args.length < 2) {
            m.sendPrefixed(sender, "auction.search-usage");
            return true;
        }
        String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (query.isEmpty()) {
            m.sendPrefixed(sender, "auction.search-usage");
            return true;
        }
        Map<String, String> queryPh = new HashMap<String, String>();
        queryPh.put("query", query);
        m.sendPrefixed(sender, "auction.search-header", queryPh);
        auction.browseListings(1, auction.getConfig().getBrowsePageSize(),
                auction.getConfig().getGuiBrowseDefaultSort(), query)
                .thenAccept(new java.util.function.Consumer<AuctionHouseManager.BrowsePage>() {
                    @Override
                    public void accept(AuctionHouseManager.BrowsePage page) {
                        if (page.totalListings() == 0) {
                            m.sendPrefixed(sender, "auction.search-empty");
                            return;
                        }
                        renderBrowsePage(sender, m, page);
                    }
                });
        return true;
    }

    private void renderBrowsePage(CommandSender sender, MessageService m, AuctionHouseManager.BrowsePage page) {
        if (page.totalListings() == 0) {
            m.sendPrefixed(sender, "auction.browse-empty");
            return;
        }
        Map<String, String> header = new HashMap<String, String>();
        header.put("page", Integer.toString(page.page()));
        header.put("pages", Integer.toString(page.totalPages()));
        header.put("total", Integer.toString(page.totalListings()));
        m.sendPrefixed(sender, "auction.browse-header", header);
        long now = System.currentTimeMillis();
        for (AuctionListing listing : page.listings()) {
            Map<String, String> ph = new HashMap<String, String>();
            ph.put("id", Long.toString(listing.listingId()));
            ph.put("item", describeItem(listing.itemStack()));
            ph.put("amount", Integer.toString(listing.itemStack().getAmount()));
            ph.put("price", auction.formatPrice(listing.price()));
            ph.put("seller", listing.sellerName() != null ? listing.sellerName() : "-");
            ph.put("time", AuctionTimeFormatter.formatRemaining(listing.remainingMillis(now)));
            m.sendPrefixed(sender, "auction.browse-entry", ph);
        }
        if (page.page() < page.totalPages()) {
            Map<String, String> next = new HashMap<String, String>();
            next.put("next", Integer.toString(page.page() + 1));
            next.put("pages", Integer.toString(page.totalPages()));
            m.sendPrefixed(sender, "auction.browse-page", next);
        }
    }

    private boolean handleBuy(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!(sender instanceof Player)) {
            m.sendPrefixed(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(AuctionPermission.BUY)) {
            m.sendPrefixed(player, "auction.no-permission");
            return true;
        }
        if (args.length < 2) {
            m.sendPrefixed(player, "auction.buy-usage");
            return true;
        }
        Long id = parseListingId(args[1]);
        if (id == null) {
            m.sendPrefixed(player, "auction.buy-not-found");
            return true;
        }
        auction.buyListing(player, id).thenAccept(new java.util.function.Consumer<AuctionHouseManager.BuyResult>() {
            @Override
            public void accept(AuctionHouseManager.BuyResult result) {
                if (result instanceof AuctionHouseManager.BuyResult.Success) {
                    AuctionHouseManager.BuyResult.Success s =
                            (AuctionHouseManager.BuyResult.Success) result;
                    Map<String, String> ph = new HashMap<String, String>();
                    ph.put("id", Long.toString(s.listing().listingId()));
                    ph.put("item", describeItem(s.listing().itemStack()));
                    ph.put("amount", Integer.toString(s.listing().itemStack().getAmount()));
                    ph.put("price", auction.formatPrice(s.price()));
                    ph.put("tax", auction.formatPrice(s.tax()));
                    ph.put("payout", auction.formatPrice(s.sellerPayout()));
                    ph.put("seller", s.listing().sellerName() != null ? s.listing().sellerName() : "-");
                    m.sendPrefixed(player, "auction.buy-success", ph);
                    if (s.tax() > 0.0D) {
                        m.sendPrefixed(player, "auction.sale-tax-info", ph);
                    }
                    if (s.inventoryFull()) {
                        m.sendPrefixed(player, "auction.buy-inventory-full");
                    }
                } else if (result instanceof AuctionHouseManager.BuyResult.Failure) {
                    AuctionHouseManager.BuyResult.Failure f =
                            (AuctionHouseManager.BuyResult.Failure) result;
                    m.sendPrefixed(player, f.messageKey(), f.placeholders());
                }
            }
        });
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (!sender.hasPermission(AuctionPermission.ADMIN)) {
            m.sendPrefixed(sender, "auction.no-permission");
            return true;
        }
        if (args.length < 2) {
            m.sendPrefixed(sender, "auction.admin-usage");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("remove".equals(action)) {
            return handleAdminRemove(sender, args);
        }
        if ("info".equals(action)) {
            return handleAdminInfo(sender);
        }
        if ("reload".equals(action)) {
            return handleAdminReload(sender);
        }
        if ("cleanup".equals(action)) {
            return handleAdminCleanup(sender);
        }
        m.sendPrefixed(sender, "auction.admin-usage");
        return true;
    }

    private boolean handleAdminRemove(CommandSender sender, String[] args) {
        MessageService m = plugin.getMessageService();
        if (args.length < 3) {
            m.sendPrefixed(sender, "auction.admin-usage");
            return true;
        }
        Long id = parseListingId(args[2]);
        if (id == null) {
            m.sendPrefixed(sender, "auction.listing-not-found");
            return true;
        }
        auction.cancelListing(new UUID(0L, 0L), id, true)
                .thenAccept(new java.util.function.Consumer<AuctionHouseManager.CancelResult>() {
                    @Override
                    public void accept(AuctionHouseManager.CancelResult result) {
                        if (result instanceof AuctionHouseManager.CancelResult.Success) {
                            AuctionHouseManager.CancelResult.Success s =
                                    (AuctionHouseManager.CancelResult.Success) result;
                            Map<String, String> ph = new HashMap<String, String>();
                            ph.put("id", Long.toString(s.listing().listingId()));
                            ph.put("seller", s.listing().sellerName() != null ? s.listing().sellerName() : "-");
                            m.sendPrefixed(sender, "auction.admin-removed", ph);
                        } else if (result instanceof AuctionHouseManager.CancelResult.Failure) {
                            AuctionHouseManager.CancelResult.Failure f =
                                    (AuctionHouseManager.CancelResult.Failure) result;
                            m.sendPrefixed(sender, f.messageKey());
                        }
                    }
                });
        return true;
    }

    private boolean handleAdminInfo(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        auction.getStats().thenAccept(new java.util.function.Consumer<AuctionHouseManager.Stats>() {
            @Override
            public void accept(AuctionHouseManager.Stats stats) {
                m.sendPrefixed(sender, "auction.admin-info-header");
                Map<String, String> state = new HashMap<String, String>();
                state.put("state", stats.active() ? "AN" : "AUS");
                state.put("reason", stats.inactiveReason() != null ? stats.inactiveReason() : "-");
                m.sendPrefixed(sender, "auction.admin-info-state", state);
                Map<String, String> storage = new HashMap<String, String>();
                storage.put("type", stats.storageType());
                m.sendPrefixed(sender, "auction.admin-info-storage", storage);
                Map<String, String> counts = new HashMap<String, String>();
                counts.put("active", Integer.toString(stats.activeListings()));
                counts.put("sold", Integer.toString(stats.soldListings()));
                counts.put("expired", Integer.toString(stats.expiredListings()));
                counts.put("cancelled", Integer.toString(stats.cancelledListings()));
                counts.put("collect", Integer.toString(stats.collectItems()));
                m.sendPrefixed(sender, "auction.admin-info-counts", counts);
                Map<String, String> tax = new HashMap<String, String>();
                tax.put("percent", formatPercent(stats.saleTaxPercent()));
                m.sendPrefixed(sender, "auction.admin-info-tax", tax);
                Map<String, String> economy = new HashMap<String, String>();
                economy.put("bridge", stats.economyBridge());
                economy.put("provider", stats.economyProvider());
                economy.put("available", stats.economyAvailable() ? "ja" : "nein");
                m.sendPrefixed(sender, "auction.admin-info-economy", economy);
            }
        });
        return true;
    }

    private static String formatPercent(double value) {
        if (value == Math.floor(value)) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private boolean handleAdminReload(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        plugin.reloadEverything();
        m.sendPrefixed(sender, "auction.reload-success");
        return true;
    }

    private boolean handleAdminCleanup(CommandSender sender) {
        MessageService m = plugin.getMessageService();
        auction.adminCleanupOldTerminalListings().thenAccept(new java.util.function.Consumer<Integer>() {
            @Override
            public void accept(Integer n) {
                Map<String, String> ph = new HashMap<String, String>();
                ph.put("count", Integer.toString(n != null ? n.intValue() : 0));
                m.sendPrefixed(sender, "auction.admin-cleanup-complete", ph);
            }
        });
        return true;
    }

    private static Long parseListingId(String raw) {
        try {
            long id = Long.parseLong(raw.trim());
            return id > 0L ? Long.valueOf(id) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String describeItem(ItemStack stack) {
        if (stack == null) {
            return "-";
        }
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String display = meta.getDisplayName();
                if (display != null && display.trim().length() > 0) {
                    return ChatColor.stripColor(display);
                }
            }
        }
        return stack.getType().name();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(AuctionPermission.BASE)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<String>(ROOT);
            if (!sender.hasPermission(AuctionPermission.ADMIN)) {
                options.remove("admin");
            }
            if (!sender.hasPermission(AuctionPermission.BROWSE)) {
                options.remove("browse");
                options.remove("search");
            }
            if (!sender.hasPermission(AuctionPermission.BUY)) {
                options.remove("buy");
            }
            if (!sender.hasPermission(AuctionPermission.SELL)) {
                options.remove("sell");
            }
            if (!sender.hasPermission(AuctionPermission.LISTINGS)) {
                options.remove("listings");
            }
            if (!sender.hasPermission(AuctionPermission.CANCEL)) {
                options.remove("cancel");
            }
            if (!sender.hasPermission(AuctionPermission.COLLECT)) {
                options.remove("collect");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])
                && sender.hasPermission(AuctionPermission.ADMIN)) {
            return filter(ADMIN_ACTIONS, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>(options.size());
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
