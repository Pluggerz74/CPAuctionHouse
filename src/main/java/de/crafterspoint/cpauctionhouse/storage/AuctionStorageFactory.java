package de.crafterspoint.cpauctionhouse.storage;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import de.crafterspoint.cpauctionhouse.auction.AuctionConfig;
import de.crafterspoint.cpauctionhouse.auction.AuctionStorage;
import de.crafterspoint.cpauctionhouse.auction.AuctionStorageException;
import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import de.crafterspoint.cpauctionhouse.storage.mysql.MySqlAuctionStorage;
import de.crafterspoint.cpauctionhouse.storage.sqlite.SQLiteAuctionStorage;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and initialises the configured {@link AuctionStorage} backend.
 */
public final class AuctionStorageFactory {

    private AuctionStorageFactory() {
    }

    public static final class InitResult {
        private final AuctionStorage storage;
        private final String displayType;

        public InitResult(AuctionStorage storage, String displayType) {
            this.storage = storage;
            this.displayType = displayType;
        }

        public AuctionStorage getStorage() {
            return storage;
        }

        public String getDisplayType() {
            return displayType;
        }
    }

    public static InitResult create(CPAuctionHousePlugin plugin) throws AuctionStorageException {
        PluginConfig pluginConfig = plugin.getPluginConfig();
        AuctionConfig auctionConfig = new AuctionConfig(plugin.getConfig(), plugin.getLogger());
        Logger logger = plugin.getLogger();

        if (pluginConfig.getStorageType() == StorageType.MYSQL) {
            try {
                MySqlAuctionStorage mysql = new MySqlAuctionStorage(pluginConfig, logger, auctionConfig.isDebug());
                mysql.init();
                logger.info("[AH] MySQL/MariaDB storage connected.");
                return new InitResult(mysql, "MySQL/MariaDB");
            } catch (AuctionStorageException ex) {
                logger.log(Level.SEVERE,
                        "[AH] MySQL/MariaDB storage init failed: " + ex.getMessage(), ex);
                if (pluginConfig.isFallbackToSqliteOnError()) {
                    logger.warning("[AH] storage.fallback-to-sqlite-on-error is enabled; using SQLite.");
                    return createSqlite(plugin, auctionConfig, "SQLite (MySQL fallback)");
                }
                throw ex;
            }
        }
        return createSqlite(plugin, auctionConfig, "SQLite");
    }

    private static InitResult createSqlite(CPAuctionHousePlugin plugin,
                                           AuctionConfig auctionConfig,
                                           String displayType) throws AuctionStorageException {
        File dbFile = new File(plugin.getDataFolder(), auctionConfig.getStorageFile());
        SQLiteAuctionStorage sqlite = new SQLiteAuctionStorage(
                dbFile, plugin.getLogger(), auctionConfig.isDebug());
        sqlite.init();
        return new InitResult(sqlite, displayType);
    }
}
