package de.crafterspoint.cpauctionhouse.auction.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

/**
 * Cancels all item movement in CPAuctionHouse GUIs and dispatches slot actions.
 */
public final class AuctionGuiClickListener implements Listener {

    private final AuctionGuiManager manager;

    public AuctionGuiClickListener(AuctionGuiManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        manager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        AuctionGuiSession session = resolveSession(player, view, top);
        if (session == null) {
            return;
        }

        event.setCancelled(true);

        ClickType type = event.getClick();
        if (isBlockedClick(type, event.getAction())) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }

        if (type != ClickType.LEFT && type != ClickType.RIGHT) {
            return;
        }

        manager.handleClick(player, session, rawSlot);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        AuctionGuiSession session = resolveSession(player, view, top);
        if (session == null) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Inventory closed = event.getInventory();
        InventoryHolder holder = closed.getHolder();
        if (holder instanceof AuctionGuiSession) {
            manager.handleSessionClose(player, (AuctionGuiSession) holder);
        }
    }

    private AuctionGuiSession resolveSession(Player player, InventoryView view, Inventory top) {
        AuctionGuiSession session = manager.getSession(player.getUniqueId());
        if (session == null) {
            return null;
        }
        InventoryHolder holder = top.getHolder();
        if (holder instanceof AuctionGuiSession) {
            return (AuctionGuiSession) holder;
        }
        Inventory current = session.getCurrentInventory();
        if (current != null && current.equals(top)) {
            return session;
        }
        String expected = session.getInventoryTitle();
        if (expected != null && expected.length() > 0) {
            String viewTitle = view.getTitle();
            if (viewTitle != null && viewTitle.equals(expected)) {
                return session;
            }
        }
        return null;
    }

    private static boolean isBlockedClick(ClickType type, InventoryAction action) {
        if (type == ClickType.SHIFT_LEFT
                || type == ClickType.SHIFT_RIGHT
                || type == ClickType.NUMBER_KEY
                || type == ClickType.DOUBLE_CLICK
                || type == ClickType.DROP
                || type == ClickType.CONTROL_DROP
                || type == ClickType.UNKNOWN) {
            return true;
        }
        return action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.CLONE_STACK;
    }
}
