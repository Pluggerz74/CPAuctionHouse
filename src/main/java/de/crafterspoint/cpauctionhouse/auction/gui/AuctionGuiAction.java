package de.crafterspoint.cpauctionhouse.auction.gui;

/**
 * Identifies what a clicked GUI slot should do. Bound per-slot in
 * {@link AuctionGuiSession} — no item NBT/PDC required.
 */
public enum AuctionGuiAction {
    NONE,
    OPEN_BROWSE,
    OPEN_SELL_HELP,
    OPEN_MY_LISTINGS,
    OPEN_COLLECT,
    OPEN_SEARCH_HELP,
    OPEN_SEARCH_INPUT,
    SEARCH_INPUT,
    SORT_CYCLE,
    RESET_SEARCH,
    BACK_TO_MAIN,
    CLOSE,
    REFRESH_BROWSE,
    PREV_PAGE,
    NEXT_PAGE,
    OPEN_BUY_CONFIRM,
    CONFIRM_BUY,
    BACK_FROM_BUY_CONFIRM,
    OPEN_CANCEL_CONFIRM,
    CONFIRM_CANCEL,
    BACK_FROM_CANCEL_CONFIRM,
    COLLECT_ALL,
    BACK
}
