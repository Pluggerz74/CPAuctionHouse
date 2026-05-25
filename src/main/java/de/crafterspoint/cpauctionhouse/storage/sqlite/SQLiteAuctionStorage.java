package de.crafterspoint.cpauctionhouse.storage.sqlite;

import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import de.crafterspoint.cpauctionhouse.storage.AuctionStorage;
import de.crafterspoint.cpauctionhouse.storage.StorageType;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * SQLite storage placeholder. JDBC wiring will be added when auction data
 * persistence is extracted from the reference project.
 */
public final class SQLiteAuctionStorage implements AuctionStorage {

    private final Plugin plugin;
    private final PluginConfig config;
    private boolean initialized;

    public SQLiteAuctionStorage(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public StorageType getType() {
        return StorageType.SQLITE;
    }

    @Override
    public void initialize() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }

        File databaseFile = new File(dataFolder, config.getSqliteFile());
        plugin.getLogger().info("SQLite storage scaffold ready (file: " + databaseFile.getName() + ").");
        initialized = true;
    }

    @Override
    public void close() {
        initialized = false;
        plugin.getLogger().info("SQLite storage scaffold closed.");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}
