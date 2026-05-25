package de.crafterspoint.cpauctionhouse.auction;

import de.crafterspoint.cpauctionhouse.message.MessageService;

/**
 * Maps {@link AuctionBrowseSort} values to GUI message keys.
 */
public final class AuctionBrowseSortFormatter {

    private AuctionBrowseSortFormatter() {
    }

    public static String guiLabel(AuctionBrowseSort sort, MessageService messages) {
        if (sort == null) {
            sort = AuctionBrowseSort.NEWEST;
        }
        return messages.message(guiMessageKey(sort));
    }

    public static String guiMessageKey(AuctionBrowseSort sort) {
        if (sort == null) {
            return "auction.gui.sort-newest";
        }
        switch (sort) {
            case OLDEST:
                return "auction.gui.sort-oldest";
            case PRICE_ASC:
                return "auction.gui.sort-price-low";
            case PRICE_DESC:
                return "auction.gui.sort-price-high";
            case EXPIRING_SOON:
                return "auction.gui.sort-ending-soon";
            case NEWEST:
            default:
                return "auction.gui.sort-newest";
        }
    }
}
