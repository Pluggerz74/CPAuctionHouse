package de.crafterspoint.cpauctionhouse.storage;

/**
 * Supported auction storage backends.
 */
public enum StorageType {

    SQLITE("sqlite"),
    MYSQL("mysql");

    private final String configKey;

    StorageType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static StorageType fromConfig(String value) {
        if (value == null) {
            return SQLITE;
        }
        String normalized = value.trim().toLowerCase();
        for (StorageType type : values()) {
            if (type.configKey.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
