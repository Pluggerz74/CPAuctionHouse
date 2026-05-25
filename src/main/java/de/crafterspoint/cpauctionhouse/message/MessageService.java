package de.crafterspoint.cpauctionhouse.message;

import de.crafterspoint.cpauctionhouse.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads messages.yml and resolves keys with the configured prefix.
 */
public final class MessageService {

    private final Plugin plugin;
    private File messagesFile;
    private FileConfiguration messages;

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void saveDefault() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    public void load() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            saveDefault();
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultsStream = plugin.getResource("messages.yml");
        if (defaultsStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
            messages.options().copyDefaults(true);
        }
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public String getRaw(String key) {
        if (messages == null) {
            return key;
        }
        return messages.getString(key, key);
    }

    public String get(String key) {
        String prefix = getRaw("prefix");
        return prefix + getRaw(key);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(Text.colorize(get(key)));
    }

    public void sendRaw(CommandSender sender, String key) {
        sender.sendMessage(Text.colorize(getRaw(key)));
    }

    public void sendPrefixedPlaceholder(CommandSender sender) {
        sender.sendMessage(Text.colorize(get("placeholder")));
    }
}
