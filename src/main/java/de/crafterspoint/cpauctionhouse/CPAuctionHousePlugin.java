package de.crafterspoint.cpauctionhouse;

import de.crafterspoint.cpauctionhouse.command.CPAuctionHouseCommand;
import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import de.crafterspoint.cpauctionhouse.economy.EconomyBridge;
import de.crafterspoint.cpauctionhouse.economy.NoEconomyBridge;
import de.crafterspoint.cpauctionhouse.economy.VaultEconomyBridge;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import de.crafterspoint.cpauctionhouse.storage.AuctionStorage;
import de.crafterspoint.cpauctionhouse.storage.StorageType;
import de.crafterspoint.cpauctionhouse.storage.mysql.MySqlAuctionStorage;
import de.crafterspoint.cpauctionhouse.storage.sqlite.SQLiteAuctionStorage;
import de.crafterspoint.cpauctionhouse.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for CPAuctionHouse.
 */
public final class CPAuctionHousePlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessageService messageService;
    private ServerVersion serverVersion;
    private EconomyBridge economyBridge;
    private AuctionStorage auctionStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messageService = new MessageService(this);
        messageService.saveDefault();
        messageService.load();

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        serverVersion = ServerVersion.detect();
        economyBridge = initializeEconomy();
        auctionStorage = initializeStorage();

        registerCommands();

        getLogger().info("CPAuctionHouse enabled.");
        getLogger().info("Server: " + serverVersion);
        getLogger().info("Storage: " + auctionStorage.getType().getConfigKey()
                + " (" + (auctionStorage.isInitialized() ? "ready" : "not ready") + ")");
        getLogger().info("Economy: " + economyBridge.providerName()
                + " (" + (economyBridge.isAvailable() ? "available" : "unavailable") + ")");
    }

    @Override
    public void onDisable() {
        if (auctionStorage != null) {
            auctionStorage.close();
        }
        getLogger().info("CPAuctionHouse disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.load();
        messageService.reload();
        economyBridge = initializeEconomy();
        closeStorageQuietly();
        auctionStorage = initializeStorage();
    }

    private EconomyBridge initializeEconomy() {
        if (isVaultPresent()) {
            VaultEconomyBridge vaultBridge = VaultEconomyBridge.tryCreate(this);
            if (vaultBridge != null && vaultBridge.isAvailable()) {
                return vaultBridge;
            }
        }

        if (pluginConfig.isRequireVault()) {
            getLogger().warning("No Vault economy provider found. Auction features will be unavailable.");
        }
        return new NoEconomyBridge();
    }

    private AuctionStorage initializeStorage() {
        AuctionStorage storage;
        if (pluginConfig.getStorageType() == StorageType.MYSQL) {
            storage = new MySqlAuctionStorage(this, pluginConfig);
        } else {
            storage = new SQLiteAuctionStorage(this, pluginConfig);
        }

        try {
            storage.initialize();
        } catch (Exception ex) {
            getLogger().severe("Storage initialization failed: " + ex.getMessage());
            if (pluginConfig.isDebug()) {
                ex.printStackTrace();
            }
        }
        return storage;
    }

    private void closeStorageQuietly() {
        if (auctionStorage != null) {
            auctionStorage.close();
        }
    }

    private void registerCommands() {
        CPAuctionHouseCommand executor = new CPAuctionHouseCommand(this, messageService);

        PluginCommand cpAuctionHouse = getCommand("cpauctionhouse");
        if (cpAuctionHouse != null) {
            cpAuctionHouse.setExecutor(executor);
            cpAuctionHouse.setTabCompleter(executor);
        }

        PluginCommand auctionHouse = getCommand("auctionhouse");
        if (auctionHouse != null) {
            auctionHouse.setExecutor(executor);
        }
    }

    private boolean isVaultPresent() {
        return Bukkit.getPluginManager().getPlugin("Vault") != null;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    public EconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public AuctionStorage getAuctionStorage() {
        return auctionStorage;
    }
}
