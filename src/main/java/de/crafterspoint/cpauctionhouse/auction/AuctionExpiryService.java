package de.crafterspoint.cpauctionhouse.auction;

import de.crafterspoint.cpauctionhouse.CPAuctionHousePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodic task that moves expired ACTIVE listings into collect storage.
 */
public final class AuctionExpiryService {

    private final CPAuctionHousePlugin plugin;
    private final AuctionHouseManager manager;

    private BukkitTask task;

    public AuctionExpiryService(CPAuctionHousePlugin plugin, AuctionHouseManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public synchronized void start(long intervalSeconds) {
        if (task != null) {
            return;
        }
        long ticks = Math.max(20L, intervalSeconds * 20L);
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        runPass();
                    }
                },
                ticks,
                ticks);
        if (manager.isDebug()) {
            plugin.getLogger().info("[AH] Expiry service started (every " + intervalSeconds + "s)");
        }
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            if (manager.isDebug()) {
                plugin.getLogger().info("[AH] Expiry service stopped");
            }
        }
    }

    private void runPass() {
        if (!manager.isActive()) {
            return;
        }
        if (manager.dbExecutor() == null) {
            return;
        }
        manager.dbExecutor().submit(new Runnable() {
            @Override
            public void run() {
                int moved = manager.runExpiryPass();
                if (moved > 0 && manager.isDebug()) {
                    plugin.getLogger().info("[AH] Expiry pass moved "
                            + moved + " listing(s) into collect storage.");
                }
            }
        });
    }
}
