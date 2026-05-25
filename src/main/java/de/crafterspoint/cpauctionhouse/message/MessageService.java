package de.crafterspoint.cpauctionhouse.message;

import de.crafterspoint.cpauctionhouse.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads messages.yml and resolves keys with optional placeholders.
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

    public String message(String key) {
        return applyPlaceholders(getRaw(key), null);
    }

    public String message(String key, Map<String, String> placeholders) {
        return applyPlaceholders(getRaw(key), placeholders);
    }

    public String get(String key) {
        return getPrefix() + message(key);
    }

    public String get(String key, Map<String, String> placeholders) {
        return getPrefix() + message(key, placeholders);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(Text.colorize(get(key)));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.colorize(get(key, placeholders)));
    }

    public void sendPrefixed(CommandSender sender, String key) {
        sender.sendMessage(Text.colorize(getPrefix() + message(key)));
    }

    public void sendPrefixed(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.colorize(getPrefix() + message(key, placeholders)));
    }

    public void sendRaw(CommandSender sender, String key) {
        sender.sendMessage(Text.colorize(message(key)));
    }

    public void sendRaw(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.colorize(message(key, placeholders)));
    }

    public void sendPrefixedPlaceholder(CommandSender sender) {
        sendPrefixed(sender, "placeholder");
    }

    private String getPrefix() {
        return getRaw("prefix");
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }
}
