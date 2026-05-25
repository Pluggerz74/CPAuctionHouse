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
 * Handles /cpauctionhouse and /cpah plugin admin commands.
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
        if (!"cpauctionhouse".equals(name) && !"cpah".equals(name)) {
            return false;
        }
        return handleAdminCommand(sender, args);
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("cpauctionhouse.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }
            plugin.reloadEverything();
            messages.send(sender, "reload-success");
            return true;
        }

        if (args.length >= 1 && "info".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("cpauctionhouse.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }
            sendPluginInfo(sender, true);
            return true;
        }

        if (!sender.hasPermission("cpauctionhouse.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        sendPluginInfo(sender, false);
        return true;
    }

    private void sendPluginInfo(final CommandSender sender, final boolean detailed) {
        final AuctionHouseManager manager = plugin.getAuctionHouseManager();
        final String version = plugin.getDescription().getVersion();
        final boolean guiEnabled = manager != null
                && manager.getConfig() != null
                && manager.getConfig().isGuiEnabled();

        Map<String, String> versionPh = new HashMap<String, String>();
        versionPh.put("version", version);
        messages.sendRaw(sender, "plugin-info-header");
        messages.sendRaw(sender, "plugin-info-version", versionPh);

        Map<String, String> guiPh = new HashMap<String, String>();
        guiPh.put("gui", guiEnabled ? "aktiviert" : "deaktiviert");
        messages.sendRaw(sender, "plugin-info-gui", guiPh);

        if (manager == null) {
            Map<String, String> backendPh = new HashMap<String, String>();
            backendPh.put("backend", "inaktiv");
            messages.sendRaw(sender, "plugin-info-backend", backendPh);
            return;
        }

        Map<String, String> backendPh = new HashMap<String, String>();
        backendPh.put("backend", manager.isActive() ? "aktiv" : "inaktiv");
        messages.sendRaw(sender, "plugin-info-backend", backendPh);

        manager.getStats().thenAccept(new java.util.function.Consumer<AuctionHouseManager.Stats>() {
            @Override
            public void accept(AuctionHouseManager.Stats stats) {
                Map<String, String> storagePh = new HashMap<String, String>();
                storagePh.put("storage", stats.storageType());
                messages.sendRaw(sender, "plugin-info-storage", storagePh);

                Map<String, String> economyPh = new HashMap<String, String>();
                economyPh.put("provider", formatEconomyProvider(stats));
                messages.sendRaw(sender, "plugin-info-economy", economyPh);

                if (!detailed) {
                    return;
                }

                Map<String, String> state = new HashMap<String, String>();
                state.put("state", stats.active() ? "aktiv" : "inaktiv");
                state.put("reason", stats.inactiveReason() == null ? "-" : stats.inactiveReason());
                messages.sendRaw(sender, "auction.admin-info-state", state);

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

                Map<String, String> economyDetail = new HashMap<String, String>();
                economyDetail.put("bridge", stats.economyBridge());
                economyDetail.put("provider", stats.economyProvider());
                economyDetail.put("available", stats.economyAvailable() ? "ja" : "nein");
                messages.sendRaw(sender, "auction.admin-info-economy", economyDetail);
            }
        });
    }

    private static String formatEconomyProvider(AuctionHouseManager.Stats stats) {
        if (stats == null) {
            return "-";
        }
        String provider = stats.economyProvider();
        if (provider == null || provider.trim().isEmpty()) {
            return stats.economyAvailable() ? "Vault" : "nicht verfuegbar";
        }
        return provider;
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
