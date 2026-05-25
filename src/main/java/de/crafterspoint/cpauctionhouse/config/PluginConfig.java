package de.crafterspoint.cpauctionhouse.config;

import de.crafterspoint.cpauctionhouse.storage.StorageType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Typed view over config.yml.
 */
public final class PluginConfig {

    private final Plugin plugin;
    private StorageType storageType;
    private boolean fallbackToSqliteOnError;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlPoolSize;
    private boolean mysqlUseSsl;
    private long mysqlConnectionTimeoutMs;
    private long mysqlMaxLifetimeMs;
    private long mysqlIdleTimeoutMs;
    private String mysqlParameters;
    private boolean requireVault;
    private String language;
    private boolean debug;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        String rawStorageType = config.getString("storage.type", "sqlite");
        StorageType parsed = StorageType.fromConfig(rawStorageType);
        if (parsed == null) {
            logger.warning("[CPAuctionHouse] Unknown storage.type '" + rawStorageType
                    + "'; falling back to sqlite.");
            storageType = StorageType.SQLITE;
        } else {
            storageType = parsed;
        }
        fallbackToSqliteOnError = config.getBoolean("storage.fallback-to-sqlite-on-error", false);

        sqliteFile = config.getString("sqlite.file", "auctionhouse.db");

        mysqlHost = config.getString("mysql.host", "localhost");
        mysqlPort = config.getInt("mysql.port", 3306);
        mysqlDatabase = config.getString("mysql.database", "cpauctionhouse");
        mysqlUsername = config.getString("mysql.username", "root");
        mysqlPassword = config.getString("mysql.password", "");
        mysqlPoolSize = Math.max(1, config.getInt("mysql.pool-size", 10));
        mysqlUseSsl = config.getBoolean("mysql.use-ssl", false);
        mysqlConnectionTimeoutMs = Math.max(1000L, config.getLong("mysql.connection-timeout-ms", 10000L));
        mysqlMaxLifetimeMs = Math.max(30000L, config.getLong("mysql.max-lifetime-ms", 1800000L));
        mysqlIdleTimeoutMs = Math.max(10000L, config.getLong("mysql.idle-timeout-ms", 600000L));
        mysqlParameters = config.getString("mysql.parameters", "");

        requireVault = config.getBoolean("economy.require-vault", true);
        language = config.getString("settings.language", "de");
        debug = config.getBoolean("settings.debug", false);
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public boolean isFallbackToSqliteOnError() {
        return fallbackToSqliteOnError;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public int getMysqlPoolSize() {
        return mysqlPoolSize;
    }

    public boolean isMysqlUseSsl() {
        return mysqlUseSsl;
    }

    public long getMysqlConnectionTimeoutMs() {
        return mysqlConnectionTimeoutMs;
    }

    public long getMysqlMaxLifetimeMs() {
        return mysqlMaxLifetimeMs;
    }

    public long getMysqlIdleTimeoutMs() {
        return mysqlIdleTimeoutMs;
    }

    public String getMysqlParameters() {
        return mysqlParameters;
    }

    public boolean isRequireVault() {
        return requireVault;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDebug() {
        return debug;
    }
}
