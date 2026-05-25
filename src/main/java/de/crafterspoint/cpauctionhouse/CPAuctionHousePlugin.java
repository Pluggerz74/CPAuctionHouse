package de.crafterspoint.cpauctionhouse;

import de.crafterspoint.cpauctionhouse.auction.AuctionCommand;
import de.crafterspoint.cpauctionhouse.auction.AuctionHouseManager;
import de.crafterspoint.cpauctionhouse.auction.gui.AuctionGuiClickListener;
import de.crafterspoint.cpauctionhouse.auction.gui.AuctionGuiManager;
import de.crafterspoint.cpauctionhouse.command.CPAuctionHouseCommand;
import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import de.crafterspoint.cpauctionhouse.economy.EconomyBridge;
import de.crafterspoint.cpauctionhouse.economy.NoEconomyBridge;
import de.crafterspoint.cpauctionhouse.economy.VaultEconomyBridge;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import de.crafterspoint.cpauctionhouse.storage.StorageType;
import de.crafterspoint.cpauctionhouse.storage.mysql.MySqlAuctionStorage;
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
    private AuctionHouseManager auctionHouseManager;
    private AuctionGuiManager auctionGuiManager;
    private AuctionGuiClickListener auctionGuiClickListener;

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

        if (pluginConfig.getStorageType() == StorageType.MYSQL) {
            new MySqlAuctionStorage(this, pluginConfig).logPlaceholder();
        }

        auctionHouseManager = new AuctionHouseManager(this);
        auctionHouseManager.enable();

        auctionGuiManager = new AuctionGuiManager(this, auctionHouseManager);
        if (auctionGuiClickListener == null) {
            auctionGuiClickListener = new AuctionGuiClickListener(auctionGuiManager);
            getServer().getPluginManager().registerEvents(auctionGuiClickListener, this);
        }

        registerCommands();

        getLogger().info("CPAuctionHouse enabled.");
        getLogger().info("Server: " + serverVersion);
        getLogger().info("Auction backend: "
                + (auctionHouseManager.isActive() ? "active" : "inactive ("
                + auctionHouseManager.getInactiveReason() + ")"));
        getLogger().info("Economy: " + economyBridge.providerName()
                + " (" + (economyBridge.isAvailable() ? "available" : "unavailable") + ")");
    }

    @Override
    public void onDisable() {
        if (auctionGuiManager != null) {
            auctionGuiManager.closeAll();
        }
        if (auctionHouseManager != null) {
            auctionHouseManager.disable();
        }
        getLogger().info("CPAuctionHouse disabled.");
    }

    /**
     * Reloads config, messages, economy bridge, and the auction backend without
     * re-registering commands or listeners.
     */
    public void reloadEverything() {
        reloadPlugin();
    }

    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.load();
        messageService.reload();
        economyBridge = initializeEconomy();
        if (auctionHouseManager != null) {
            auctionHouseManager.reload();
        }
        if (auctionGuiManager != null) {
            auctionGuiManager.reload();
        }
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

    private void registerCommands() {
        CPAuctionHouseCommand cpCommand = new CPAuctionHouseCommand(this, messageService);
        PluginCommand cpAuctionHouse = getCommand("cpauctionhouse");
        if (cpAuctionHouse != null) {
            cpAuctionHouse.setExecutor(cpCommand);
            cpAuctionHouse.setTabCompleter(cpCommand);
        }

        AuctionCommand auctionCommand = new AuctionCommand(this, auctionHouseManager);
        PluginCommand auctionHouse = getCommand("auctionhouse");
        if (auctionHouse != null) {
            auctionHouse.setExecutor(auctionCommand);
            auctionHouse.setTabCompleter(auctionCommand);
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

    public AuctionHouseManager getAuctionHouseManager() {
        return auctionHouseManager;
    }

    public AuctionGuiManager getAuctionGuiManager() {
        return auctionGuiManager;
    }
}
