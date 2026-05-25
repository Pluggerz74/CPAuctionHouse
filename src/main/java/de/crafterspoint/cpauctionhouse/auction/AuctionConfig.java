package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typed, immutable snapshot of auction settings from config.yml.
 */
public final class AuctionConfig {

    private final boolean enabled;
    private final String storageType;
    private final String storageFile;

    private final long durationMillis;
    private final int maxActiveListingsDefault;
    private final double minPrice;
    private final double maxPrice;
    private final boolean allowOwnPurchase;

    private final double listingFee;
    private final double saleTaxPercent;

    private final double expensivePurchaseThreshold;

    private final Set<Material> blockedMaterials;

    private final long expireCheckIntervalSeconds;

    private final int browsePageSize;

    private final boolean guiEnabled;
    private final String guiTitle;
    private final long guiRefreshCooldownMs;
    private final int guiRowsMain;
    private final int guiRowsBrowse;
    private final int guiRowsListings;
    private final int guiRowsCollect;
    private final boolean guiRefreshAfterAction;
    private final boolean guiConfirmationEnabled;
    private final boolean guiFillerEnabled;
    private final Material guiFillerMaterial;
    private final String guiFillerName;

    private final boolean guiSellEnabled;
    private final boolean guiSellUseAnvilPriceInput;
    private final String guiAnvilPriceTitle;
    private final String guiAnvilPriceInitialText;
    private final boolean guiSellReturnToMainAfterCreate;
    private final boolean guiSellOpenListingsAfterCreate;

    private final AuctionBrowseSort guiBrowseDefaultSort;
    private final boolean guiBrowseShowRefreshButton;
    private final boolean guiBrowseShowSortButton;

    private final int guiInfoButtonCooldownSeconds;

    private final int cleanupOldListingRetentionDays;

    private final boolean debug;

    public AuctionConfig(FileConfiguration root, Logger logger) {
        this.enabled = root.getBoolean("auction.enabled", true);

        this.storageType = root.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        this.storageFile = root.getString("sqlite.file", "auctionhouse.db");

        double rawHours = root.getDouble("auction.listings.duration-hours", 72.0D);
        long hoursClamped = (long) Math.max(1.0D, Math.min(rawHours, 24.0D * 365.0D));
        this.durationMillis = hoursClamped * 60L * 60L * 1000L;

        this.maxActiveListingsDefault = Math.max(0,
                root.getInt("auction.listings.max-active-per-player", 10));
        this.minPrice = Math.max(0.0D, root.getDouble("auction.listings.min-price", 1.0D));
        this.maxPrice = Math.max(this.minPrice,
                root.getDouble("auction.listings.max-price", 1_000_000_000.0D));
        this.allowOwnPurchase = root.getBoolean("auction.listings.allow-own-purchase", false);

        this.listingFee = Math.max(0.0D, root.getDouble("auction.economy.listing-fee", 0.0D));
        this.saleTaxPercent = Math.max(0.0D, root.getDouble("auction.economy.sale-tax-percent", 5.0D));

        this.expensivePurchaseThreshold = Math.max(0.0D,
                root.getDouble("auction.confirmations.expensive-purchase-threshold", 10_000.0D));

        this.blockedMaterials = new HashSet<Material>();
        List<String> rawBlocked = root.getStringList("auction.listings.blocked-materials");
        for (String name : rawBlocked) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            Material material = Material.matchMaterial(name.trim());
            if (material != null) {
                this.blockedMaterials.add(material);
            } else {
                logger.warning("Unknown blocked material: " + name);
            }
        }

        long rawInterval = root.getLong("auction.cleanup.expire-check-interval-seconds", 60L);
        this.expireCheckIntervalSeconds = Math.max(10L, Math.min(rawInterval, 3600L));

        int rawPageSize = root.getInt("auction.browse.page-size", 8);
        this.browsePageSize = Math.max(3, Math.min(rawPageSize, 50));

        this.guiEnabled = root.getBoolean("auction.gui.enabled", true);
        this.guiTitle = root.getString("auction.gui.title", "&6Auktionshaus");
        this.guiRefreshCooldownMs = Math.max(0L, root.getLong("auction.gui.refresh-cooldown-ms", 750L));
        this.guiRowsMain = clampRows(root.getInt("auction.gui.rows.main", 3), 1, 3);
        this.guiRowsBrowse = clampRows(root.getInt("auction.gui.rows.browse", 6), 3, 6);
        this.guiRowsListings = clampRows(root.getInt("auction.gui.rows.listings", 6), 3, 6);
        this.guiRowsCollect = clampRows(root.getInt("auction.gui.rows.collect", 6), 3, 6);
        this.guiRefreshAfterAction = root.getBoolean("auction.gui.refresh-after-action", true);
        this.guiConfirmationEnabled = root.getBoolean("auction.gui.confirmation.enabled", true);
        this.guiFillerEnabled = root.getBoolean("auction.gui.filler.enabled", true);
        String fillerRaw = root.getString("auction.gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        Material fillerResolved = Material.matchMaterial(fillerRaw != null ? fillerRaw.trim() : "");
        if (fillerResolved == null) {
            logger.log(Level.WARNING, "Unknown gui.filler.material '" + fillerRaw
                    + "', falling back to GRAY_STAINED_GLASS_PANE.");
            fillerResolved = Material.matchMaterial("GRAY_STAINED_GLASS_PANE");
            if (fillerResolved == null) {
                fillerResolved = Material.STONE;
            }
        }
        this.guiFillerMaterial = fillerResolved;
        this.guiFillerName = root.getString("auction.gui.filler.name", " ");

        this.guiSellEnabled = root.getBoolean("auction.gui.sell.enabled", false);
        this.guiSellUseAnvilPriceInput = root.getBoolean("auction.gui.anvil-price-input",
                root.getBoolean("auction.gui.sell.use-anvil-price-input", true));
        this.guiAnvilPriceTitle = root.getString("auction.gui.anvil-price-title", "&6Preis eingeben");
        this.guiAnvilPriceInitialText = root.getString("auction.gui.anvil-price-initial-text", "100");
        this.guiSellReturnToMainAfterCreate = root.getBoolean(
                "auction.gui.sell.return-to-main-after-create", false);
        this.guiSellOpenListingsAfterCreate = root.getBoolean(
                "auction.gui.sell.open-listings-after-create", true);

        this.guiBrowseDefaultSort = AuctionBrowseSort.fromConfig(
                root.getString("auction.gui.browse.default-sort", "newest"));
        this.guiBrowseShowRefreshButton = root.getBoolean(
                "auction.gui.browse.show-refresh-button", true);
        this.guiBrowseShowSortButton = root.getBoolean(
                "auction.gui.browse.show-sort-button", true);

        this.guiInfoButtonCooldownSeconds = Math.max(0,
                root.getInt("auction.gui.info-button-cooldown-seconds", 5));

        int rawRetention = root.getInt("auction.cleanup.old-listing-retention-days", 30);
        this.cleanupOldListingRetentionDays = Math.max(1, Math.min(rawRetention, 3650));

        this.debug = root.getBoolean("auction.debug", false)
                || root.getBoolean("settings.debug", false);
    }

    private static int clampRows(int requested, int min, int max) {
        return Math.max(min, Math.min(requested, max));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getStorageFile() {
        return storageFile;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public int getMaxActiveListingsDefault() {
        return maxActiveListingsDefault;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public boolean isAllowOwnPurchase() {
        return allowOwnPurchase;
    }

    public double getListingFee() {
        return listingFee;
    }

    public double getSaleTaxPercent() {
        return saleTaxPercent;
    }

    public double getExpensivePurchaseThreshold() {
        return expensivePurchaseThreshold;
    }

    public Set<Material> getBlockedMaterials() {
        return blockedMaterials;
    }

    public long getExpireCheckIntervalSeconds() {
        return expireCheckIntervalSeconds;
    }

    public int getBrowsePageSize() {
        return browsePageSize;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public long getGuiRefreshCooldownMs() {
        return guiRefreshCooldownMs;
    }

    public int getGuiRowsMain() {
        return guiRowsMain;
    }

    public int getGuiRowsBrowse() {
        return guiRowsBrowse;
    }

    public int getGuiRowsListings() {
        return guiRowsListings;
    }

    public int getGuiRowsCollect() {
        return guiRowsCollect;
    }

    public boolean isGuiRefreshAfterAction() {
        return guiRefreshAfterAction;
    }

    public boolean isGuiConfirmationEnabled() {
        return guiConfirmationEnabled;
    }

    public boolean isGuiFillerEnabled() {
        return guiFillerEnabled;
    }

    public Material getGuiFillerMaterial() {
        return guiFillerMaterial;
    }

    public String getGuiFillerName() {
        return guiFillerName;
    }

    public boolean isGuiSellEnabled() {
        return guiSellEnabled;
    }

    public boolean isGuiSellUseAnvilPriceInput() {
        return guiSellUseAnvilPriceInput;
    }

    public String getGuiAnvilPriceTitle() {
        return guiAnvilPriceTitle;
    }

    public String getGuiAnvilPriceInitialText() {
        return guiAnvilPriceInitialText;
    }

    public boolean isGuiSellReturnToMainAfterCreate() {
        return guiSellReturnToMainAfterCreate;
    }

    public boolean isGuiSellOpenListingsAfterCreate() {
        return guiSellOpenListingsAfterCreate;
    }

    public AuctionBrowseSort getGuiBrowseDefaultSort() {
        return guiBrowseDefaultSort;
    }

    public boolean isGuiBrowseShowRefreshButton() {
        return guiBrowseShowRefreshButton;
    }

    public boolean isGuiBrowseShowSortButton() {
        return guiBrowseShowSortButton;
    }

    public int getGuiInfoButtonCooldownSeconds() {
        return guiInfoButtonCooldownSeconds;
    }

    public int getCleanupOldListingRetentionDays() {
        return cleanupOldListingRetentionDays;
    }

    public boolean isDebug() {
        return debug;
    }
}
