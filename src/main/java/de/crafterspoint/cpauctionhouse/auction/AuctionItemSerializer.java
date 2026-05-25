package de.crafterspoint.cpauctionhouse.auction;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Safe round-trip serializer for {@link ItemStack} values stored in SQLite.
 */
public final class AuctionItemSerializer {

    private static final Method SERIALIZE_AS_BYTES;
    private static final Method DESERIALIZE_BYTES;

    static {
        Method serialise = null;
        Method deserialise = null;
        try {
            serialise = ItemStack.class.getMethod("serializeAsBytes");
            deserialise = ItemStack.class.getMethod("deserializeBytes", byte[].class);
        } catch (NoSuchMethodException ignored) {
            // Pre-Paper API; fall back to BukkitObjectOutputStream.
        }
        SERIALIZE_AS_BYTES = serialise;
        DESERIALIZE_BYTES = deserialise;
    }

    private AuctionItemSerializer() {
    }

    public static String serialize(ItemStack item) throws IOException {
        byte[] bytes = serialiseBytes(item);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ItemStack deserialize(String base64) throws IOException, ClassNotFoundException {
        if (base64 == null || base64.isEmpty()) {
            throw new IOException("empty payload");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IOException("invalid base64 payload", ex);
        }
        return deserialiseBytes(raw);
    }

    private static byte[] serialiseBytes(ItemStack item) throws IOException {
        if (SERIALIZE_AS_BYTES != null) {
            try {
                Object result = SERIALIZE_AS_BYTES.invoke(item);
                if (result instanceof byte[]) {
                    return (byte[]) result;
                }
                throw new IOException("ItemStack#serializeAsBytes returned non-byte[]");
            } catch (ReflectiveOperationException ex) {
                throw new IOException("ItemStack#serializeAsBytes failed: " + ex.getMessage(), ex);
            }
        }
        return legacySerialise(item);
    }

    private static ItemStack deserialiseBytes(byte[] raw) throws IOException, ClassNotFoundException {
        if (DESERIALIZE_BYTES != null) {
            try {
                Object result = DESERIALIZE_BYTES.invoke(null, (Object) raw);
                if (result instanceof ItemStack) {
                    return (ItemStack) result;
                }
                throw new IOException("ItemStack#deserializeBytes returned " + result);
            } catch (ReflectiveOperationException ex) {
                throw new IOException("ItemStack#deserializeBytes failed: " + ex.getMessage(), ex);
            }
        }
        return legacyDeserialise(raw);
    }

    @SuppressWarnings("deprecation")
    private static byte[] legacySerialise(ItemStack item) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos);
        try {
            out.writeObject(item);
        } finally {
            out.close();
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("deprecation")
    private static ItemStack legacyDeserialise(byte[] raw) throws IOException, ClassNotFoundException {
        BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(raw));
        try {
            Object obj = in.readObject();
            if (!(obj instanceof ItemStack)) {
                throw new IOException("payload does not contain an ItemStack");
            }
            return (ItemStack) obj;
        } finally {
            in.close();
        }
    }
}
