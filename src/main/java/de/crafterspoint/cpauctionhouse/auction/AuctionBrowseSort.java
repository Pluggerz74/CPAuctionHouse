package de.crafterspoint.cpauctionhouse.auction;

import java.util.Locale;

/**
 * Browse / market ordering.
 */
public enum AuctionBrowseSort {

    NEWEST,
    OLDEST,
    PRICE_ASC,
    PRICE_DESC,
    EXPIRING_SOON;

    public AuctionBrowseSort next() {
        switch (this) {
            case NEWEST:
                return OLDEST;
            case OLDEST:
                return PRICE_ASC;
            case PRICE_ASC:
                return PRICE_DESC;
            case PRICE_DESC:
                return EXPIRING_SOON;
            case EXPIRING_SOON:
            default:
                return NEWEST;
        }
    }

    public String messageKey() {
        switch (this) {
            case OLDEST:
                return "auction.sort-oldest";
            case PRICE_ASC:
                return "auction.sort-price-asc";
            case PRICE_DESC:
                return "auction.sort-price-desc";
            case EXPIRING_SOON:
                return "auction.sort-expiring";
            case NEWEST:
            default:
                return "auction.sort-newest";
        }
    }

    public static AuctionBrowseSort fromConfig(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return NEWEST;
        }
        String n = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("oldest".equals(n)) {
            return OLDEST;
        }
        if ("price_asc".equals(n) || "priceasc".equals(n)) {
            return PRICE_ASC;
        }
        if ("price_desc".equals(n) || "pricedesc".equals(n)) {
            return PRICE_DESC;
        }
        if ("expiring".equals(n) || "expiring_soon".equals(n) || "expiringsoon".equals(n)) {
            return EXPIRING_SOON;
        }
        return NEWEST;
    }
}
