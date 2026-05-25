package de.crafterspoint.cpauctionhouse.storage;

/**
 * Auction persistence abstraction. Implementations will be wired to SQLite
 * or MySQL in a later extraction step.
 */
public interface AuctionStorage {

    StorageType getType();

    void initialize() throws Exception;

    void close();

    boolean isInitialized();
}
