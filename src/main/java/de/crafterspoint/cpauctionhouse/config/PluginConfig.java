package de.crafterspoint.cpauctionhouse.config;

import de.crafterspoint.cpauctionhouse.storage.StorageType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Typed view over config.yml.
 */
public final class PluginConfig {

    private final Plugin plugin;
    private StorageType storageType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlPoolSize;
    private boolean mysqlUseSsl;
    private boolean requireVault;
    private String language;
    private boolean debug;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        storageType = StorageType.fromConfig(config.getString("storage.type", "sqlite"));
        sqliteFile = config.getString("sqlite.file", "auctionhouse.db");

        mysqlHost = config.getString("mysql.host", "localhost");
        mysqlPort = config.getInt("mysql.port", 3306);
        mysqlDatabase = config.getString("mysql.database", "cpauctionhouse");
        mysqlUsername = config.getString("mysql.username", "root");
        mysqlPassword = config.getString("mysql.password", "");
        mysqlPoolSize = config.getInt("mysql.pool-size", 10);
        mysqlUseSsl = config.getBoolean("mysql.use-ssl", false);

        requireVault = config.getBoolean("economy.require-vault", true);
        language = config.getString("settings.language", "de");
        debug = config.getBoolean("settings.debug", false);
    }

    public StorageType getStorageType() {
        return storageType;
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
