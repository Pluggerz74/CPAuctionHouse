package de.crafterspoint.cpauctionhouse.auction.gui.input;

/**
 * Result of validating an anvil price text field.
 */
public final class PriceInputResult {

    public enum Type {
        VALID,
        INVALID
    }

    private final Type type;
    private final double price;
    private final String hintText;

    private PriceInputResult(Type type, double price, String hintText) {
        this.type = type;
        this.price = price;
        this.hintText = hintText;
    }

    public Type getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public String getHintText() {
        return hintText;
    }

    public static PriceInputResult valid(double price) {
        return new PriceInputResult(Type.VALID, price, "");
    }

    public static PriceInputResult invalid(String hintText) {
        return new PriceInputResult(Type.INVALID, 0.0D, hintText);
    }
}
