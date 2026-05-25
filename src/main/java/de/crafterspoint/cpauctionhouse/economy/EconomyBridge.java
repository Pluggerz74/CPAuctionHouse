package de.crafterspoint.cpauctionhouse.economy;

import java.util.UUID;

/**
 * Economy abstraction for CPAuctionHouse. Auction features will depend on
 * this interface instead of Vault directly.
 */
public interface EconomyBridge {

    boolean isAvailable();

    String providerName();

    boolean hasBalance(UUID playerId, double amount);

    double getBalance(UUID playerId);

    EconomyTransactionResult withdraw(UUID playerId, double amount, String reason);

    EconomyTransactionResult deposit(UUID playerId, double amount, String reason);

    String format(double amount);
}
