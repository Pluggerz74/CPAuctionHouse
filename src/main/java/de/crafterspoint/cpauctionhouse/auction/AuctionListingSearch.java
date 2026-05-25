package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * In-memory filter / sort for search-backed browse.
 */
public final class AuctionListingSearch {

    private AuctionListingSearch() {
    }

    public static List<AuctionListing> filter(List<AuctionListing> listings, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<AuctionListing>(listings);
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return new ArrayList<AuctionListing>(listings);
        }
        List<AuctionListing> out = new ArrayList<AuctionListing>();
        for (AuctionListing listing : listings) {
            if (matches(listing, needle)) {
                out.add(listing);
            }
        }
        return out;
    }

    public static boolean matches(AuctionListing listing, String needleLower) {
        if (listing.sellerName() != null
                && listing.sellerName().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        ItemStack stack = listing.itemStack();
        if (stack == null) {
            return false;
        }
        if (stack.getType().name().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        String display = plainDisplayName(stack);
        return display.length() > 0 && display.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static String plainDisplayName(ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return "";
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "";
        }
        String name = meta.getDisplayName();
        return name == null ? "" : stripColorCodes(name);
    }

    private static String stripColorCodes(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\u00A7.", "").replaceAll("&.", "");
    }

    public static void sort(List<AuctionListing> listings, AuctionBrowseSort sort) {
        AuctionBrowseSort effective = sort == null ? AuctionBrowseSort.NEWEST : sort;
        Comparator<AuctionListing> cmp;
        switch (effective) {
            case OLDEST:
                cmp = new Comparator<AuctionListing>() {
                    @Override
                    public int compare(AuctionListing a, AuctionListing b) {
                        int c = Long.compare(a.createdAt(), b.createdAt());
                        if (c != 0) {
                            return c;
                        }
                        return Long.compare(a.listingId(), b.listingId());
                    }
                };
                break;
            case PRICE_ASC:
                cmp = new Comparator<AuctionListing>() {
                    @Override
                    public int compare(AuctionListing a, AuctionListing b) {
                        int c = Double.compare(a.price(), b.price());
                        if (c != 0) {
                            return c;
                        }
                        return Long.compare(a.listingId(), b.listingId());
                    }
                };
                break;
            case PRICE_DESC:
                cmp = new Comparator<AuctionListing>() {
                    @Override
                    public int compare(AuctionListing a, AuctionListing b) {
                        int c = Double.compare(b.price(), a.price());
                        if (c != 0) {
                            return c;
                        }
                        return Long.compare(b.listingId(), a.listingId());
                    }
                };
                break;
            case EXPIRING_SOON:
                cmp = new Comparator<AuctionListing>() {
                    @Override
                    public int compare(AuctionListing a, AuctionListing b) {
                        int c = Long.compare(a.expiresAt(), b.expiresAt());
                        if (c != 0) {
                            return c;
                        }
                        return Long.compare(a.listingId(), b.listingId());
                    }
                };
                break;
            case NEWEST:
            default:
                cmp = new Comparator<AuctionListing>() {
                    @Override
                    public int compare(AuctionListing a, AuctionListing b) {
                        int c = Long.compare(b.createdAt(), a.createdAt());
                        if (c != 0) {
                            return c;
                        }
                        return Long.compare(b.listingId(), a.listingId());
                    }
                };
                break;
        }
        Collections.sort(listings, cmp);
    }
}
