package de.crafterspoint.cpauctionhouse.economy;

import java.util.Locale;
import java.util.UUID;

/**
 * Null-object economy bridge used when Vault or an economy provider is unavailable.
 */
public final class NoEconomyBridge implements EconomyBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String providerName() {
        return "None";
    }

    @Override
    public boolean hasBalance(UUID playerId, double amount) {
        return false;
    }

    @Override
    public double getBalance(UUID playerId) {
        return 0.0D;
    }

    @Override
    public EconomyTransactionResult withdraw(UUID playerId, double amount, String reason) {
        return EconomyTransactionResult.failure("economy.unavailable", null);
    }

    @Override
    public EconomyTransactionResult deposit(UUID playerId, double amount, String reason) {
        return EconomyTransactionResult.failure("economy.unavailable", null);
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
