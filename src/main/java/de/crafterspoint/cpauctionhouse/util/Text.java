package de.crafterspoint.cpauctionhouse.util;

import org.bukkit.ChatColor;

/**
 * Bukkit {@link ChatColor} translation for legacy &amp; color codes.
 */
public final class Text {

    private Text() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
