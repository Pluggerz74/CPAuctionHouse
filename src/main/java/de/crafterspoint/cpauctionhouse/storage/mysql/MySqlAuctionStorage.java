package de.crafterspoint.cpauctionhouse.storage.mysql;

import de.crafterspoint.cpauctionhouse.auction.AuctionBrowseSort;
import de.crafterspoint.cpauctionhouse.auction.AuctionCollectItem;
import de.crafterspoint.cpauctionhouse.auction.AuctionCollectReason;
import de.crafterspoint.cpauctionhouse.auction.AuctionItemSerializer;
import de.crafterspoint.cpauctionhouse.auction.AuctionListing;
import de.crafterspoint.cpauctionhouse.auction.AuctionListingStatus;
import de.crafterspoint.cpauctionhouse.auction.AuctionStorage;
import de.crafterspoint.cpauctionhouse.auction.AuctionStorageException;
import de.crafterspoint.cpauctionhouse.config.PluginConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL/MariaDB-backed {@link AuctionStorage} using HikariCP connection pooling.
 * Every call must run on the manager's DB executor.
 */
public final class MySqlAuctionStorage implements AuctionStorage {

    private static final String CREATE_LISTINGS =
            "CREATE TABLE IF NOT EXISTS auction_listings ("
                    + "listing_id BIGINT NOT NULL AUTO_INCREMENT, "
                    + "seller_uuid VARCHAR(36) NOT NULL, "
                    + "seller_name VARCHAR(16) NOT NULL, "
                    + "item_data LONGTEXT NOT NULL, "
                    + "price DOUBLE NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "expires_at BIGINT NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, "
                    + "buyer_uuid VARCHAR(36) NULL, "
                    + "buyer_name VARCHAR(16) NULL, "
                    + "PRIMARY KEY (listing_id), "
                    + "INDEX idx_listings_seller_status (seller_uuid, status), "
                    + "INDEX idx_listings_status_expires (status, expires_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_COLLECT =
            "CREATE TABLE IF NOT EXISTS auction_collect_items ("
                    + "collect_id BIGINT NOT NULL AUTO_INCREMENT, "
                    + "owner_uuid VARCHAR(36) NOT NULL, "
                    + "item_data LONGTEXT NOT NULL, "
                    + "reason VARCHAR(32) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "source_listing_id BIGINT NULL, "
                    + "PRIMARY KEY (collect_id), "
                    + "INDEX idx_collect_owner (owner_uuid)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private final PluginConfig config;
    private final Logger logger;
    private final boolean debug;

    private HikariDataSource dataSource;

    public MySqlAuctionStorage(PluginConfig config, Logger logger, boolean debug) {
        this.config = config;
        this.logger = logger;
        this.debug = debug;
    }

    @Override
    public void init() throws AuctionStorageException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new AuctionStorageException(
                    "MySQL JDBC driver not available: " + ex.getMessage(), ex);
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(buildJdbcUrl());
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword() != null ? config.getMysqlPassword() : "");
        hikari.setMaximumPoolSize(Math.max(1, config.getMysqlPoolSize()));
        hikari.setConnectionTimeout(config.getMysqlConnectionTimeoutMs());
        hikari.setMaxLifetime(config.getMysqlMaxLifetimeMs());
        hikari.setIdleTimeout(config.getMysqlIdleTimeoutMs());
        hikari.setPoolName("CPAuctionHouse-MySQL");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");

        try {
            this.dataSource = new HikariDataSource(hikari);
        } catch (Throwable t) {
            throw new AuctionStorageException(
                    "Failed to create MySQL connection pool: " + t.getMessage(), t);
        }

        Connection test = null;
        try {
            test = dataSource.getConnection();
            createSchema(test);
            if (debug) {
                logger.info("[AH] MySQL storage ready ("
                        + config.getMysqlHost() + ":" + config.getMysqlPort()
                        + "/" + config.getMysqlDatabase() + ")");
            }
        } catch (SQLException ex) {
            closeDataSourceQuietly();
            throw new AuctionStorageException(
                    "Failed to connect to MySQL/MariaDB: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(test);
        }
    }

    @Override
    public void close() {
        closeDataSourceQuietly();
    }

    @Override
    public long insertListing(UUID sellerUuid,
                              String sellerName,
                              ItemStack item,
                              double price,
                              long createdAt,
                              long expiresAt) throws AuctionStorageException {
        String sql = "INSERT INTO auction_listings("
                + "seller_uuid, seller_name, item_data, price, "
                + "created_at, expires_at, status, buyer_uuid, buyer_name) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL)";
        String payload = serialiseOrThrow(item);
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            try {
                ps.setString(1, sellerUuid.toString());
                ps.setString(2, sellerName);
                ps.setString(3, payload);
                ps.setDouble(4, price);
                ps.setLong(5, createdAt);
                ps.setLong(6, expiresAt);
                ps.setString(7, AuctionListingStatus.ACTIVE.name());
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new AuctionStorageException("insertListing: unexpected affected rows " + affected);
                }
                ResultSet keys = ps.getGeneratedKeys();
                try {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                    throw new AuctionStorageException("insertListing: no generated key returned");
                } finally {
                    keys.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("insertListing failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Optional<AuctionListing> getListing(long listingId) throws AuctionStorageException {
        String sql = "SELECT * FROM auction_listings WHERE listing_id = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setLong(1, listingId);
                ResultSet rs = ps.executeQuery();
                try {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    AuctionListing listing = readListing(rs);
                    if (listing == null) {
                        return Optional.empty();
                    }
                    return Optional.of(listing);
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getListing failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<AuctionListing> getListingsBySellerAndStatus(UUID seller,
                                                             AuctionListingStatus status)
            throws AuctionStorageException {
        String sql = "SELECT * FROM auction_listings "
                + "WHERE seller_uuid = ? AND status = ? ORDER BY created_at ASC";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, seller.toString());
                ps.setString(2, status.name());
                return collectListings(ps);
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getListingsBySellerAndStatus failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<AuctionListing> getExpiredActiveListings(long now, int limit) throws AuctionStorageException {
        String sql = "SELECT * FROM auction_listings "
                + "WHERE status = ? AND expires_at <= ? ORDER BY expires_at ASC LIMIT ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.ACTIVE.name());
                ps.setLong(2, now);
                ps.setInt(3, Math.max(1, limit));
                return collectListings(ps);
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getExpiredActiveListings failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<AuctionListing> getActiveBrowsePage(long now,
                                                    int offset,
                                                    int limit,
                                                    AuctionBrowseSort sort) throws AuctionStorageException {
        String orderBy = orderByClause(sort);
        String sql = "SELECT * FROM auction_listings WHERE status = ? AND expires_at > ? ORDER BY "
                + orderBy + " LIMIT ? OFFSET ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.ACTIVE.name());
                ps.setLong(2, now);
                ps.setInt(3, Math.max(1, limit));
                ps.setInt(4, Math.max(0, offset));
                return collectListings(ps);
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getActiveBrowsePage failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<AuctionListing> getAllActiveBrowseListings(long now) throws AuctionStorageException {
        String sql = "SELECT * FROM auction_listings "
                + "WHERE status = ? AND expires_at > ? ORDER BY listing_id ASC";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.ACTIVE.name());
                ps.setLong(2, now);
                return collectListings(ps);
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getAllActiveBrowseListings failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public int countActiveBrowse(long now) throws AuctionStorageException {
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE status = ? AND expires_at > ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.ACTIVE.name());
                ps.setLong(2, now);
                ResultSet rs = ps.executeQuery();
                try {
                    return rs.next() ? rs.getInt(1) : 0;
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("countActiveBrowse failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public int deleteTerminalListingsOlderThan(long createdBeforeMillis) throws AuctionStorageException {
        String sql = "DELETE FROM auction_listings "
                + "WHERE status IN (?, ?, ?, ?) AND created_at < ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.SOLD.name());
                ps.setString(2, AuctionListingStatus.EXPIRED.name());
                ps.setString(3, AuctionListingStatus.CANCELLED.name());
                ps.setString(4, AuctionListingStatus.REMOVED.name());
                ps.setLong(5, createdBeforeMillis);
                return ps.executeUpdate();
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("deleteTerminalListingsOlderThan failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean transitionListingStatus(long listingId,
                                           AuctionListingStatus fromStatus,
                                           AuctionListingStatus toStatus)
            throws AuctionStorageException {
        String sql = "UPDATE auction_listings SET status = ? WHERE listing_id = ? AND status = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, toStatus.name());
                ps.setLong(2, listingId);
                ps.setString(3, fromStatus.name());
                return ps.executeUpdate() == 1;
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("transitionListingStatus failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean markSoldIfActive(long listingId,
                                    UUID buyerUuid,
                                    String buyerName,
                                    long now) throws AuctionStorageException {
        String sql = "UPDATE auction_listings "
                + "SET status = ?, buyer_uuid = ?, buyer_name = ? "
                + "WHERE listing_id = ? AND status = ? AND expires_at > ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.SOLD.name());
                ps.setString(2, buyerUuid.toString());
                ps.setString(3, buyerName);
                ps.setLong(4, listingId);
                ps.setString(5, AuctionListingStatus.ACTIVE.name());
                ps.setLong(6, now);
                return ps.executeUpdate() == 1;
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("markSoldIfActive failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean revertSoldIfBuyer(long listingId, UUID buyerUuid) throws AuctionStorageException {
        String sql = "UPDATE auction_listings "
                + "SET status = ?, buyer_uuid = NULL, buyer_name = NULL "
                + "WHERE listing_id = ? AND status = ? AND buyer_uuid = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, AuctionListingStatus.ACTIVE.name());
                ps.setLong(2, listingId);
                ps.setString(3, AuctionListingStatus.SOLD.name());
                ps.setString(4, buyerUuid.toString());
                return ps.executeUpdate() == 1;
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("revertSoldIfBuyer failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public int countListingsByStatus(AuctionListingStatus status) throws AuctionStorageException {
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE status = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, status.name());
                ResultSet rs = ps.executeQuery();
                try {
                    return rs.next() ? rs.getInt(1) : 0;
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("countListingsByStatus failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public int countListingsBySellerAndStatus(UUID seller,
                                              AuctionListingStatus status) throws AuctionStorageException {
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid = ? AND status = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, seller.toString());
                ps.setString(2, status.name());
                ResultSet rs = ps.executeQuery();
                try {
                    return rs.next() ? rs.getInt(1) : 0;
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("countListingsBySellerAndStatus failed: "
                    + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public long insertCollectItem(UUID ownerUuid,
                                  ItemStack item,
                                  AuctionCollectReason reason,
                                  long createdAt,
                                  Long sourceListingId) throws AuctionStorageException {
        String sql = "INSERT INTO auction_collect_items("
                + "owner_uuid, item_data, reason, created_at, source_listing_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        String payload = serialiseOrThrow(item);
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            try {
                ps.setString(1, ownerUuid.toString());
                ps.setString(2, payload);
                ps.setString(3, reason.name());
                ps.setLong(4, createdAt);
                if (sourceListingId == null) {
                    ps.setNull(5, Types.BIGINT);
                } else {
                    ps.setLong(5, sourceListingId);
                }
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new AuctionStorageException("insertCollectItem: unexpected affected rows " + affected);
                }
                ResultSet keys = ps.getGeneratedKeys();
                try {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                    throw new AuctionStorageException("insertCollectItem: no generated key returned");
                } finally {
                    keys.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("insertCollectItem failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<AuctionCollectItem> getCollectItemsForOwner(UUID owner) throws AuctionStorageException {
        String sql = "SELECT * FROM auction_collect_items WHERE owner_uuid = ? ORDER BY created_at ASC";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, owner.toString());
                ResultSet rs = ps.executeQuery();
                try {
                    List<AuctionCollectItem> out = new ArrayList<AuctionCollectItem>();
                    while (rs.next()) {
                        AuctionCollectItem item = readCollectItem(rs);
                        if (item != null) {
                            out.add(item);
                        }
                    }
                    return out;
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("getCollectItemsForOwner failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean deleteCollectItem(long collectId) throws AuctionStorageException {
        String sql = "DELETE FROM auction_collect_items WHERE collect_id = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setLong(1, collectId);
                return ps.executeUpdate() == 1;
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("deleteCollectItem failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public boolean updateCollectItemStack(long collectId, ItemStack newItem) throws AuctionStorageException {
        String payload = serialiseOrThrow(newItem);
        String sql = "UPDATE auction_collect_items SET item_data = ? WHERE collect_id = ?";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, payload);
                ps.setLong(2, collectId);
                return ps.executeUpdate() == 1;
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("updateCollectItemStack failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public int countCollectItems() throws AuctionStorageException {
        String sql = "SELECT COUNT(*) FROM auction_collect_items";
        Connection conn = borrow();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ResultSet rs = ps.executeQuery();
                try {
                    return rs.next() ? rs.getInt(1) : 0;
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            throw new AuctionStorageException("countCollectItems failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    private String buildJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://");
        url.append(config.getMysqlHost());
        url.append(':');
        url.append(config.getMysqlPort());
        url.append('/');
        url.append(config.getMysqlDatabase());
        url.append("?useSSL=");
        url.append(config.isMysqlUseSsl());
        url.append("&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String parameters = config.getMysqlParameters();
        if (parameters != null && parameters.trim().length() > 0) {
            String extra = parameters.trim();
            if (extra.startsWith("?")) {
                extra = extra.substring(1);
            } else if (extra.startsWith("&")) {
                extra = extra.substring(1);
            }
            url.append('&').append(extra);
        }
        return url.toString();
    }

    private void createSchema(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        try {
            st.execute(CREATE_LISTINGS);
            st.execute(CREATE_COLLECT);
        } finally {
            st.close();
        }
    }

    private Connection borrow() throws AuctionStorageException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new AuctionStorageException("MySQL connection pool is not initialised");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new AuctionStorageException("MySQL getConnection failed: " + ex.getMessage(), ex);
        }
    }

    private void closeDataSourceQuietly() {
        if (dataSource == null) {
            return;
        }
        try {
            dataSource.close();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "[AH] Failed to close MySQL connection pool", ex);
        } finally {
            dataSource = null;
        }
    }

    private static void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
            // return to pool failure is non-fatal on shutdown
        }
    }

    private static String orderByClause(AuctionBrowseSort sort) {
        AuctionBrowseSort effective = sort == null ? AuctionBrowseSort.NEWEST : sort;
        switch (effective) {
            case OLDEST:
                return "created_at ASC, listing_id ASC";
            case PRICE_ASC:
                return "price ASC, listing_id ASC";
            case PRICE_DESC:
                return "price DESC, listing_id DESC";
            case EXPIRING_SOON:
                return "expires_at ASC, listing_id ASC";
            case NEWEST:
            default:
                return "created_at DESC, listing_id DESC";
        }
    }

    private List<AuctionListing> collectListings(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        try {
            List<AuctionListing> out = new ArrayList<AuctionListing>();
            while (rs.next()) {
                AuctionListing listing = readListing(rs);
                if (listing != null) {
                    out.add(listing);
                }
            }
            return out;
        } finally {
            rs.close();
        }
    }

    private AuctionListing readListing(ResultSet rs) throws SQLException {
        long id = rs.getLong("listing_id");
        UUID sellerUuid = parseUuidOrNull(rs.getString("seller_uuid"));
        if (sellerUuid == null) {
            logger.warning("[AH] Listing #" + id + " has invalid seller_uuid; skipping.");
            return null;
        }
        ItemStack item;
        try {
            item = AuctionItemSerializer.deserialize(rs.getString("item_data"));
        } catch (IOException ex) {
            logger.warning("[AH] Listing #" + id + " has unreadable item_data; skipping. ("
                    + ex.getMessage() + ")");
            return null;
        } catch (ClassNotFoundException ex) {
            logger.warning("[AH] Listing #" + id + " has unreadable item_data; skipping. ("
                    + ex.getMessage() + ")");
            return null;
        }
        AuctionListingStatus status;
        try {
            status = AuctionListingStatus.valueOf(rs.getString("status"));
        } catch (IllegalArgumentException ex) {
            logger.warning("[AH] Listing #" + id + " has unknown status; skipping.");
            return null;
        }
        UUID buyerUuid = parseUuidOrNull(rs.getString("buyer_uuid"));
        String buyerName = rs.getString("buyer_name");
        return new AuctionListing(
                id,
                sellerUuid,
                rs.getString("seller_name"),
                item,
                rs.getDouble("price"),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                status,
                buyerUuid,
                buyerName);
    }

    private AuctionCollectItem readCollectItem(ResultSet rs) throws SQLException {
        long id = rs.getLong("collect_id");
        UUID ownerUuid = parseUuidOrNull(rs.getString("owner_uuid"));
        if (ownerUuid == null) {
            logger.warning("[AH] Collect #" + id + " has invalid owner_uuid; skipping.");
            return null;
        }
        ItemStack item;
        try {
            item = AuctionItemSerializer.deserialize(rs.getString("item_data"));
        } catch (IOException ex) {
            logger.warning("[AH] Collect #" + id + " has unreadable item_data; skipping. ("
                    + ex.getMessage() + ")");
            return null;
        } catch (ClassNotFoundException ex) {
            logger.warning("[AH] Collect #" + id + " has unreadable item_data; skipping. ("
                    + ex.getMessage() + ")");
            return null;
        }
        AuctionCollectReason reason = AuctionCollectReason.fromStringOrDefault(rs.getString("reason"));
        Long sourceId = rs.getLong("source_listing_id");
        if (rs.wasNull()) {
            sourceId = null;
        }
        return new AuctionCollectItem(
                id,
                ownerUuid,
                item,
                reason,
                rs.getLong("created_at"),
                sourceId);
    }

    private String serialiseOrThrow(ItemStack item) throws AuctionStorageException {
        try {
            return AuctionItemSerializer.serialize(item);
        } catch (IOException ex) {
            throw new AuctionStorageException("ItemStack serialisation failed: " + ex.getMessage(), ex);
        }
    }

    private static UUID parseUuidOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
