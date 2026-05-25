package de.crafterspoint.cpauctionhouse.auction.gui;

import de.crafterspoint.cpauctionhouse.auction.AuctionBrowseSort;
import de.crafterspoint.cpauctionhouse.auction.AuctionBrowseSortFormatter;
import de.crafterspoint.cpauctionhouse.auction.AuctionCollectItem;
import de.crafterspoint.cpauctionhouse.auction.AuctionConfig;
import de.crafterspoint.cpauctionhouse.auction.AuctionHouseManager;
import de.crafterspoint.cpauctionhouse.auction.AuctionListing;
import de.crafterspoint.cpauctionhouse.auction.AuctionTimeFormatter;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import de.crafterspoint.cpauctionhouse.util.Text;
import de.crafterspoint.cpauctionhouse.version.MaterialResolver;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds GUI item stacks using Bukkit {@link ItemMeta} and &amp; color codes.
 */
public final class AuctionGuiItemFactory {

    private final MessageService messages;
    private final AuctionHouseManager auction;

    public AuctionGuiItemFactory(MessageService messages, AuctionHouseManager auction) {
        this.messages = messages;
        this.auction = auction;
    }

    public ItemStack filler(AuctionConfig config) {
        if (!config.isGuiFillerEnabled()) {
            return new ItemStack(Material.AIR);
        }
        ItemStack stack = new ItemStack(config.getGuiFillerMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(config.getGuiFillerName()));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack mainBrowseButton() {
        return button(MaterialResolver.resolve("CHEST", "TRAPPED_CHEST"),
                "auction.gui.button-browse");
    }

    public ItemStack mainSellButton() {
        return button(Material.EMERALD, "auction.gui.button-sell");
    }

    public ItemStack mainListingsButton() {
        return button(Material.BOOK, "auction.gui.button-my-listings");
    }

    public ItemStack mainCollectButton() {
        return button(Material.HOPPER, "auction.gui.button-collect");
    }

    public ItemStack mainSearchButton() {
        ItemStack stack = new ItemStack(MaterialResolver.resolve("COMPASS", "PAPER"));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-search")));
            List<String> lore = new ArrayList<String>();
            lore.add(color("&7Suche nach Item, Verkaeufer oder ID."));
            lore.add(color(messages.message("auction.gui.click-search")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack browseSearchButton() {
        ItemStack stack = new ItemStack(MaterialResolver.resolve("COMPASS", "PAPER"));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-search-input")));
            List<String> lore = new ArrayList<String>();
            lore.add(color(messages.message("auction.gui.click-search")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack browseSortButton(AuctionBrowseSort sort) {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-sort")));
            List<String> lore = new ArrayList<String>();
            Map<String, String> ph = new HashMap<String, String>();
            ph.put("sort", AuctionBrowseSortFormatter.guiLabel(sort, messages));
            lore.add(color(messages.message("auction.gui.sort-current", ph)));
            lore.add(color(messages.message("auction.gui.click-cycle-sort")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack browseResetSearchButton() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-reset-search")));
            List<String> lore = new ArrayList<String>();
            lore.add(color(messages.message("auction.gui.click-reset-search")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack browseStatusTile(int page, int totalPages, AuctionBrowseSort sort, String searchTerm) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6Marktplatz"));
            List<String> lore = new ArrayList<String>();
            Map<String, String> pagePh = new HashMap<String, String>();
            pagePh.put("page", Integer.toString(page));
            pagePh.put("pages", Integer.toString(totalPages));
            lore.add(color(messages.message("auction.gui.page-current", pagePh)));
            Map<String, String> sortPh = new HashMap<String, String>();
            sortPh.put("sort", AuctionBrowseSortFormatter.guiLabel(sort, messages));
            lore.add(color(messages.message("auction.gui.sort-current", sortPh)));
            if (searchTerm != null && searchTerm.trim().length() > 0) {
                Map<String, String> searchPh = new HashMap<String, String>();
                searchPh.put("search", searchTerm.trim());
                lore.add(color(messages.message("auction.gui.search-current", searchPh)));
            } else {
                lore.add(color(messages.message("auction.gui.search-none")));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack backButton() {
        return button(Material.ARROW, "auction.gui.button-back");
    }

    public ItemStack closeButton() {
        return button(Material.BARRIER, "auction.gui.button-close");
    }

    public ItemStack refreshButton() {
        return button(Material.CLOCK, "auction.gui.button-refresh");
    }

    public ItemStack previousPageButton() {
        return button(Material.ARROW, "auction.gui.button-previous");
    }

    public ItemStack nextPageButton() {
        return button(Material.ARROW, "auction.gui.button-next");
    }

    public ItemStack confirmBuyButton() {
        return button(Material.GOLD_INGOT, "auction.gui.button-confirm-buy");
    }

    public ItemStack confirmCancelButton() {
        return button(Material.BARRIER, "auction.gui.button-confirm-cancel");
    }

    public ItemStack collectAllButton() {
        return button(Material.CHEST, "auction.gui.button-collect-all");
    }

    public ItemStack emptyBrowseTile(boolean searchActive) {
        if (searchActive) {
            return button(Material.PAPER, "auction.search-empty");
        }
        return button(Material.PAPER, "auction.gui.no-listings");
    }

    public ItemStack emptyOwnListingsTile() {
        return button(Material.PAPER, "auction.gui.no-own-listings");
    }

    public ItemStack collectInfoTile() {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-collect")));
            List<String> lore = new ArrayList<String>();
            lore.add(color(messages.message("auction.gui.collect-info")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack sellHelpTile(ItemStack handPreview) {
        ItemStack stack;
        if (handPreview != null && handPreview.getType() != Material.AIR) {
            stack = handPreview.clone();
        } else {
            stack = new ItemStack(Material.PAPER);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.sell-help-title")));
            List<String> lore = new ArrayList<String>();
            lore.add(color(messages.message("auction.gui.sell-help-line-1")));
            lore.add(color(messages.message("auction.gui.sell-help-line-2")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack searchHelpTile() {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message("auction.gui.button-search")));
            List<String> lore = new ArrayList<String>();
            lore.add(color(messages.message("auction.gui.search-help")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack loadingTile() {
        return button(Material.CLOCK, "auction.gui.loading");
    }

    public ItemStack browseListingTile(AuctionListing listing, UUID viewerId, boolean allowOwn, long now) {
        ItemStack stack = listing.itemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        lore.add(color("&8----------------"));
        lore.add(color("&7ID: &e#" + listing.listingId()));
        lore.add(color("&7Verkaeufer: &f" + safeName(listing.sellerName())));
        lore.add(color("&7Preis: &e" + auction.formatPrice(listing.price())));
        lore.add(color("&7Laeuft ab: &f" + AuctionTimeFormatter.formatRemaining(listing.remainingMillis(now))));
        if (listing.sellerUuid().equals(viewerId) && !allowOwn) {
            lore.add(color("&cEigenes Angebot"));
        } else {
            lore.add(color("&aLinksklick: Kaufen"));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack ownListingTile(AuctionListing listing, long now) {
        ItemStack stack = listing.itemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        lore.add(color("&8----------------"));
        lore.add(color("&7ID: &e#" + listing.listingId()));
        lore.add(color("&7Preis: &e" + auction.formatPrice(listing.price())));
        lore.add(color("&7Laeuft ab: &f" + AuctionTimeFormatter.formatRemaining(listing.remainingMillis(now))));
        lore.add(color("&cLinksklick: Entfernen"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack collectPreviewTile(AuctionCollectItem item, long now) {
        ItemStack stack = item.itemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        lore.add(color("&8----------------"));
        lore.add(color("&7Abhol-ID: &e#" + item.collectId()));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack confirmPreview(AuctionListing listing, long now) {
        ItemStack stack = listing.itemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        lore.add(color("&8----------------"));
        lore.add(color("&7Preis: &e" + auction.formatPrice(listing.price())));
        lore.add(color("&7Verkaeufer: &f" + safeName(listing.sellerName())));
        lore.add(color("&7Laeuft ab: &f" + AuctionTimeFormatter.formatRemaining(listing.remainingMillis(now))));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack pageIndicator(int page, int pages) {
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("page", Integer.toString(page));
        ph.put("pages", Integer.toString(pages));
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&7Seite &e" + page + "&7/&e" + pages));
            meta.setLore(Collections.singletonList(color(messages.message("auction.gui.page-indicator", ph))));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack button(Material material, String messageKey) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(messages.message(messageKey)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "-" : name;
    }

    private static String color(String input) {
        return Text.colorize(input);
    }
}

