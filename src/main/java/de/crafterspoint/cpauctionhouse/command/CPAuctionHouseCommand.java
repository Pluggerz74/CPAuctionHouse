package de.crafterspoint.cpauctionhouse.command;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        String name = command.getName().toLowerCase();

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

        if (!sender.hasPermission("cpauctionhouse.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        messages.sendPrefixedPlaceholder(sender);
        return true;
    }

    private boolean handlePlayerCommand(CommandSender sender) {
        if (!sender.hasPermission("cpauctionhouse.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        messages.sendPrefixedPlaceholder(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (!"cpauctionhouse".equals(name) && !"cpah".equals(name)) {
            return Collections.emptyList();
        }
        if (args.length == 1 && sender.hasPermission("cpauctionhouse.admin")) {
            List<String> suggestions = new ArrayList<String>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                suggestions.add("reload");
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
