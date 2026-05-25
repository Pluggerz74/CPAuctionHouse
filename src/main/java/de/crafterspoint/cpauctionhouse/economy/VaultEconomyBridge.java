package de.crafterspoint.cpauctionhouse.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;
import java.util.UUID;

/**
 * Vault-backed economy bridge.
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
    public EconomyTransactionResult withdraw(UUID playerId, double amount, String reason) {
        if (amount < 0) {
            return EconomyTransactionResult.failure("economy.invalid-amount", null);
        }
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            double balanceBefore = economy.getBalance(player);
            if (!economy.has(player, amount)) {
                return EconomyTransactionResult.failure("economy.insufficient-funds", null);
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return toResult(response, balanceBefore);
        } catch (Throwable t) {
            return EconomyTransactionResult.failure("economy.transaction-failed", t.getMessage());
        }
    }

    @Override
    public EconomyTransactionResult deposit(UUID playerId, double amount, String reason) {
        if (amount < 0) {
            return EconomyTransactionResult.failure("economy.invalid-amount", null);
        }
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            double balanceBefore = economy.getBalance(player);
            EconomyResponse response = economy.depositPlayer(player, amount);
            return toResult(response, balanceBefore);
        } catch (Throwable t) {
            return EconomyTransactionResult.failure("economy.transaction-failed", t.getMessage());
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

    private EconomyTransactionResult toResult(EconomyResponse response, double balanceBefore) {
        if (response == null) {
            return EconomyTransactionResult.failure("economy.transaction-failed", null);
        }
        if (response.transactionSuccess()) {
            return EconomyTransactionResult.ok(balanceBefore, response.balance);
        }
        String reasonKey = response.errorMessage != null
                && response.errorMessage.toLowerCase(Locale.ROOT).contains("not enough")
                ? "economy.insufficient-funds"
                : "economy.transaction-failed";
        return EconomyTransactionResult.failure(reasonKey, response.errorMessage);
    }
}
