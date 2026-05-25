package de.crafterspoint.cpauctionhouse.storage.mysql;

import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import org.bukkit.plugin.Plugin;

/**
 * MySQL storage placeholder. Real JDBC implementation will be added later.
 */
public final class MySqlAuctionStorage {

    private final Plugin plugin;
    private final PluginConfig config;

    public MySqlAuctionStorage(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void logPlaceholder() {
        plugin.getLogger().info("MySQL storage is not implemented yet ("
                + config.getMysqlHost() + ":" + config.getMysqlPort()
                + "/" + config.getMysqlDatabase() + "). Use storage.type: sqlite.");
    }
}
