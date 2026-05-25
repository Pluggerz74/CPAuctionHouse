package de.crafterspoint.cpauctionhouse.command;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.auction.AuctionHouseManager;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles /cpauctionhouse, /cpah, /auctionhouse, /ah, and /auktion.
 */
public final class CPAuctionHouseCommand implements CommandExecutor, TabCompleter {

    private final CPAuctionHousePlugin plugin;
    private final MessageService messages;

    public CPAuctionHouseCommand(CPAuctionHousePlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if ("cpauctionhouse".equals(name) || "cpah".equals(name)) {
            return handleAdminCommand(sender, args);
        }

        if ("auctionhouse".equals(name) || "ah".equals(name) || "auktion".equals(name)) {
            return handlePlayerCommand(sender);
        }

        return false;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("cpauctionhouse.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }
            plugin.reloadPlugin();
            messages.send(sender, "reload-success");
            return true;
        }

        if (args.length >= 1 && "info".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("cpauctionhouse.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }
            sendAdminInfo(sender);
            return true;
        }

        if (!sender.hasPermission("cpauctionhouse.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        messages.sendPrefixedPlaceholder(sender);
        return true;
    }

    private void sendAdminInfo(final CommandSender sender) {
        AuctionHouseManager manager = plugin.getAuctionHouseManager();
        if (manager == null) {
            messages.sendRaw(sender, "auction.disabled");
            return;
        }
        manager.getStats().thenAccept(new java.util.function.Consumer<AuctionHouseManager.Stats>() {
            @Override
            public void accept(AuctionHouseManager.Stats stats) {
                Map<String, String> state = new HashMap<String, String>();
                state.put("state", stats.active() ? "aktiv" : "inaktiv");
                state.put("reason", stats.inactiveReason() == null ? "-" : stats.inactiveReason());
                messages.sendRaw(sender, "auction.admin-info-header");
                messages.sendRaw(sender, "auction.admin-info-state", state);
                Map<String, String> storage = new HashMap<String, String>();
                storage.put("type", stats.storageType());
                messages.sendRaw(sender, "auction.admin-info-storage", storage);
                Map<String, String> counts = new HashMap<String, String>();
                counts.put("active", Integer.toString(stats.activeListings()));
                counts.put("sold", Integer.toString(stats.soldListings()));
                counts.put("expired", Integer.toString(stats.expiredListings()));
                counts.put("cancelled", Integer.toString(stats.cancelledListings()));
                counts.put("collect", Integer.toString(stats.collectItems()));
                messages.sendRaw(sender, "auction.admin-info-counts", counts);
                Map<String, String> tax = new HashMap<String, String>();
                tax.put("percent", Double.toString(stats.saleTaxPercent()));
                messages.sendRaw(sender, "auction.admin-info-tax", tax);
                Map<String, String> economy = new HashMap<String, String>();
                economy.put("bridge", stats.economyBridge());
                economy.put("provider", stats.economyProvider());
                economy.put("available", stats.economyAvailable() ? "ja" : "nein");
                messages.sendRaw(sender, "auction.admin-info-economy", economy);
            }
        });
    }

    private boolean handlePlayerCommand(CommandSender sender) {
        if (!sender.hasPermission("cpauctionhouse.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        messages.sendPrefixedPlaceholder(sender);
        AuctionHouseManager manager = plugin.getAuctionHouseManager();
        if (manager != null && manager.isActive()) {
            messages.sendRaw(sender, "backend-active");
        } else {
            messages.sendRaw(sender, "backend-inactive");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!"cpauctionhouse".equals(name) && !"cpah".equals(name)) {
            return Collections.emptyList();
        }
        if (args.length == 1 && sender.hasPermission("cpauctionhouse.admin")) {
            List<String> suggestions = new ArrayList<String>();
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial)) {
                suggestions.add("reload");
            }
            if ("info".startsWith(partial)) {
                suggestions.add("info");
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
