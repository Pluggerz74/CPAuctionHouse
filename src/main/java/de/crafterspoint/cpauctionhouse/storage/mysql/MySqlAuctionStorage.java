package de.crafterspoint.cpauctionhouse.storage.mysql;

import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import de.crafterspoint.cpauctionhouse.storage.AuctionStorage;
import de.crafterspoint.cpauctionhouse.storage.StorageType;
import org.bukkit.plugin.Plugin;

/**
 * MySQL storage placeholder. Connection pooling and schema migration will
 * be added in a later step.
 */
public final class MySqlAuctionStorage implements AuctionStorage {

    private final Plugin plugin;
    private final PluginConfig config;
    private boolean initialized;

    public MySqlAuctionStorage(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public StorageType getType() {
        return StorageType.MYSQL;
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("MySQL storage scaffold ready ("
                + config.getMysqlHost() + ":" + config.getMysqlPort()
                + "/" + config.getMysqlDatabase() + ").");
        initialized = true;
    }

    @Override
    public void close() {
        initialized = false;
        plugin.getLogger().info("MySQL storage scaffold closed.");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}
