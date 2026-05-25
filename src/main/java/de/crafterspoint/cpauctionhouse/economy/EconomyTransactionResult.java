package de.crafterspoint.cpauctionhouse.economy;

/**
 * Outcome of a {@link EconomyBridge#withdraw} or {@link EconomyBridge#deposit} call.
 */
public final class EconomyTransactionResult {

    private final boolean success;
    private final String reasonKey;
    private final Double balanceBefore;
    private final Double balanceAfter;
    private final String providerMessage;

    public EconomyTransactionResult(boolean success,
                                    String reasonKey,
                                    Double balanceBefore,
                                    Double balanceAfter,
                                    String providerMessage) {
        this.success = success;
        this.reasonKey = reasonKey;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.providerMessage = providerMessage;
    }

    public boolean success() {
        return success;
    }

    public String reasonKey() {
        return reasonKey;
    }

    public Double balanceBefore() {
        return balanceBefore;
    }

    public Double balanceAfter() {
        return balanceAfter;
    }

    public String providerMessage() {
        return providerMessage;
    }

    public static EconomyTransactionResult ok(double balanceBefore, double balanceAfter) {
        return new EconomyTransactionResult(
                true,
                "economy.transaction-ok",
                balanceBefore,
                balanceAfter,
                null);
    }

    public static EconomyTransactionResult failure(String reasonKey, String providerMessage) {
        return new EconomyTransactionResult(
                false,
                reasonKey,
                null,
                null,
                providerMessage);
    }
}
