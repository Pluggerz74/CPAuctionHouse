package de.crafterspoint.cpauctionhouse.auction.gui.input;

import de.crafterspoint.cpauctionhouse.auction.AuctionConfig;
import de.crafterspoint.cpauctionhouse.message.MessageService;
import de.crafterspoint.cpauctionhouse.util.Text;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens a WesJD AnvilGUI to collect a browse search term. Does not touch storage.
 */
public final class AnvilSearchInput {

    private final Plugin plugin;
    private final MessageService messages;
    private final Logger logger;

    public AnvilSearchInput(Plugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.logger = plugin.getLogger();
    }

    /**
     * @return {@code true} if the anvil was opened
     */
    public boolean open(final Player player,
                        final AuctionConfig config,
                        final String currentSearch,
                        final Callback callback) {
        if (player == null || !player.isOnline() || config == null || callback == null) {
            return false;
        }

        final String title = resolveTitle(config);
        final String placeholder = resolveInitialText(config);
        final String initialText = resolvePrefill(currentSearch, placeholder);
        final boolean[] confirmed = new boolean[] {false};

        try {
            new AnvilGUI.Builder()
                    .plugin(plugin)
                    .title(title)
                    .text(initialText)
                    .itemLeft(new ItemStack(Material.PAPER))
                    .onClose(new java.util.function.Consumer<AnvilGUI.StateSnapshot>() {
                        @Override
                        public void accept(AnvilGUI.StateSnapshot state) {
                            if (!confirmed[0] && state.getPlayer().isOnline()) {
                                callback.onCancelled(state.getPlayer());
                            }
                        }
                    })
                    .onClick(new BiFunction<Integer, AnvilGUI.StateSnapshot, List<AnvilGUI.ResponseAction>>() {
                        @Override
                        public List<AnvilGUI.ResponseAction> apply(Integer slot,
                                                                   AnvilGUI.StateSnapshot state) {
                            if (slot == null || slot.intValue() != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            confirmed[0] = true;
                            final String result = normalizeResult(state.getText(), placeholder);
                            final Player clicker = state.getPlayer();
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (clicker.isOnline()) {
                                                callback.onSearch(clicker, result);
                                            }
                                        }
                                    }));
                        }
                    })
                    .open(player);
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to open anvil search input for " + player.getName(), t);
            return false;
        }
    }

    static String normalizeResult(String raw, String placeholder) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (placeholder != null && trimmed.equalsIgnoreCase(placeholder.trim())) {
            return "";
        }
        if ("Suche".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    private String resolvePrefill(String currentSearch, String placeholder) {
        if (currentSearch != null && currentSearch.trim().length() > 0) {
            return currentSearch.trim();
        }
        return placeholder;
    }

    private String resolveTitle(AuctionConfig config) {
        String fromConfig = config.getGuiAnvilSearchTitle();
        if (fromConfig != null && fromConfig.trim().length() > 0) {
            return Text.colorize(fromConfig);
        }
        return Text.colorize(messages.message("auction.gui.anvil-search-title"));
    }

    private String resolveInitialText(AuctionConfig config) {
        String fromConfig = config.getGuiAnvilSearchInitialText();
        if (fromConfig != null && fromConfig.trim().length() > 0) {
            return fromConfig.trim();
        }
        return "Suche";
    }

    public interface Callback {
        void onSearch(Player player, String searchTerm);

        void onCancelled(Player player);
    }
}
