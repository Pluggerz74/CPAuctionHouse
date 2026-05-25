package de.crafterspoint.cpauctionhouse.auction;

/**
 * Wraps storage-layer failures (SQL errors, I/O errors, serialisation errors).
 */
public final class AuctionStorageException extends Exception {

    public AuctionStorageException(String message) {
        super(message);
    }

    public AuctionStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
