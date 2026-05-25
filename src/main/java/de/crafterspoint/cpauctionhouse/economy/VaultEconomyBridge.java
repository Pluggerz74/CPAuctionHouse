package de.crafterspoint.cpauctionhouse.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;
import java.util.UUID;

/**
 * Vault-backed economy bridge. Only loaded when Vault and an economy
 * provider are present at runtime.
 */
public final class VaultEconomyBridge implements EconomyBridge {

    private final Economy economy;

    private VaultEconomyBridge(Economy economy) {
        this.economy = economy;
    }

    public static VaultEconomyBridge tryCreate(Plugin plugin) {
        try {
            RegisteredServiceProvider<Economy> registration =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration == null) {
                return null;
            }
            Economy provider = registration.getProvider();
            if (provider == null) {
                return null;
            }
            return new VaultEconomyBridge(provider);
        } catch (Throwable t) {
            plugin.getLogger().warning("Vault economy lookup failed: " + t.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return economy.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String providerName() {
        try {
            String name = economy.getName();
            if (name != null && name.trim().length() > 0) {
                return name;
            }
            return "Vault";
        } catch (Throwable t) {
            return "Vault";
        }
    }

    @Override
    public boolean hasBalance(UUID playerId, double amount) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return economy.has(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public double getBalance(UUID playerId) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return economy.getBalance(player);
        } catch (Throwable t) {
            return 0.0D;
        }
    }

    @Override
    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (Throwable t) {
            return String.format(Locale.ROOT, "%.2f", amount);
        }
    }
}
