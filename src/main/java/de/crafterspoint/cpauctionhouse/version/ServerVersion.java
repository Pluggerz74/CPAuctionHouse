package de.crafterspoint.cpauctionhouse.version;

import org.bukkit.Bukkit;

/**
 * Detects the running server version from the Bukkit version string.
 */
public final class ServerVersion {

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    public ServerVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
    }

    public static ServerVersion detect() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        String raw = bukkitVersion == null ? "unknown" : bukkitVersion;

        int major = 0;
        int minor = 0;
        int patch = 0;

        int dashIndex = raw.indexOf('-');
        String versionPart = dashIndex >= 0 ? raw.substring(0, dashIndex) : raw;
        String[] parts = versionPart.split("\\.");
        if (parts.length >= 1) {
            major = parseInt(parts[0]);
        }
        if (parts.length >= 2) {
            minor = parseInt(parts[1]);
        }
        if (parts.length >= 3) {
            patch = parseInt(parts[2]);
        }

        return new ServerVersion(major, minor, patch, raw);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getRaw() {
        return raw;
    }

    public boolean isAtLeast(int requiredMajor, int requiredMinor) {
        if (major > requiredMajor) {
            return true;
        }
        if (major < requiredMajor) {
            return false;
        }
        return minor >= requiredMinor;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + " (" + raw + ")";
    }
}
