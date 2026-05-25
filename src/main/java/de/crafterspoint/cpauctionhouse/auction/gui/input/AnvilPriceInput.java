package de.crafterspoint.cpauctionhouse.auction.gui.input;

import de.crafterspoint.cpauctionhouse.auction.AuctionConfig;
import de.crafterspoint.cpauctionhouse.auction.AuctionPriceParser;
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
 * Opens a WesJD AnvilGUI to collect a sell price. Does not touch storage or
 * player inventory — only validates text and invokes a callback on success.
 */
public final class AnvilPriceInput {

    private static final String HINT_EMPTY = "Preis?";
    private static final String HINT_INVALID = "Ungueltig";

    private final Plugin plugin;
    private final MessageService messages;
    private final Logger logger;

    public AnvilPriceInput(Plugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.logger = plugin.getLogger();
    }

    /**
     * Opens the anvil price GUI for {@code player}.
     *
     * @return {@code true} if the anvil was opened, {@code false} on failure
     */
    public boolean open(final Player player, final AuctionConfig config, final Callback callback) {
        if (player == null || !player.isOnline() || config == null || callback == null) {
            return false;
        }

        final String title = resolveTitle(config);
        final String initialText = resolveInitialText(config);
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
                            PriceInputResult validation = validate(state.getText(), config);
                            if (validation.getType() == PriceInputResult.Type.VALID) {
                                confirmed[0] = true;
                                final double price = validation.getPrice();
                                final Player clicker = state.getPlayer();
                                return Arrays.asList(
                                        AnvilGUI.ResponseAction.close(),
                                        AnvilGUI.ResponseAction.run(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (clicker.isOnline()) {
                                                    callback.onValidPrice(clicker, price);
                                                }
                                            }
                                        }));
                            }
                            messages.sendPrefixed(state.getPlayer(), "auction.gui.anvil-invalid-price");
                            return Collections.singletonList(
                                    AnvilGUI.ResponseAction.replaceInputText(validation.getHintText()));
                        }
                    })
                    .open(player);
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to open anvil price input for " + player.getName(), t);
            return false;
        }
    }

    static PriceInputResult validate(String raw, AuctionConfig config) {
        if (raw == null || raw.trim().isEmpty()) {
            return PriceInputResult.invalid(HINT_EMPTY);
        }
        AuctionPriceParser.Result parsed = AuctionPriceParser.parseStrictPositive(raw.trim());
        if (!parsed.ok()) {
            return PriceInputResult.invalid(HINT_INVALID);
        }
        double price = parsed.value();
        if (price < config.getMinPrice() || price > config.getMaxPrice()) {
            return PriceInputResult.invalid(HINT_INVALID);
        }
        return PriceInputResult.valid(price);
    }

    private String resolveTitle(AuctionConfig config) {
        String fromConfig = config.getGuiAnvilPriceTitle();
        if (fromConfig != null && fromConfig.trim().length() > 0) {
            return Text.colorize(fromConfig);
        }
        return Text.colorize(messages.message("auction.gui.anvil-price-title"));
    }

    private String resolveInitialText(AuctionConfig config) {
        String fromConfig = config.getGuiAnvilPriceInitialText();
        if (fromConfig != null && fromConfig.trim().length() > 0) {
            return fromConfig.trim();
        }
        return "100";
    }

    /**
     * Called on the main thread after a valid price was confirmed.
     */
    public interface Callback {
        void onValidPrice(Player player, double price);

        void onCancelled(Player player);
    }
}
