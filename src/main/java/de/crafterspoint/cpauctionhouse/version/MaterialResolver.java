package de.crafterspoint.cpauctionhouse.version;

import org.bukkit.Material;

/**
 * Resolves materials by name with fallbacks for cross-version compatibility.
 */
public final class MaterialResolver {

    private MaterialResolver() {
    }

    public static Material resolve(String primary, String fallback) {
        Material material = match(primary);
        if (material != null) {
            return material;
        }
        material = match(fallback);
        if (material != null) {
            return material;
        }
        return Material.STONE;
    }

    public static Material resolve(String primary, String fallback, String lastResort) {
        Material material = match(primary);
        if (material != null) {
            return material;
        }
        material = match(fallback);
        if (material != null) {
            return material;
        }
        material = match(lastResort);
        if (material != null) {
            return material;
        }
        return Material.STONE;
    }

    private static Material match(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return Material.matchMaterial(name.trim());
    }
}
