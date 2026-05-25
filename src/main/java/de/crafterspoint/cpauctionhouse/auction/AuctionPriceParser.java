package de.crafterspoint.cpauctionhouse.auction;

import java.util.Locale;

/**
 * Lenient price input parser for auction sell commands.
 */
public final class AuctionPriceParser {

    private AuctionPriceParser() {
    }

    public static Result parse(String raw) {
        return parseInternal(raw, false);
    }

    public static Result parseStrictPositive(String raw) {
        return parseInternal(raw, true);
    }

    private static Result parseInternal(String raw, boolean rejectZero) {
        if (raw == null) {
            return Result.invalid();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Result.invalid();
        }
        String normalized = normalizePriceInput(trimmed);
        if (normalized == null) {
            return Result.invalid();
        }

        double value;
        try {
            value = Double.parseDouble(normalized.toLowerCase(Locale.ROOT));
        } catch (NumberFormatException ex) {
            return Result.invalid();
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            return Result.invalid();
        }
        if (rejectZero && value <= 0.0D) {
            return Result.invalid();
        }
        double rounded = Math.round(value * 100.0D) / 100.0D;
        if (rejectZero && rounded <= 0.0D) {
            return Result.invalid();
        }
        return Result.ok(rounded);
    }

    static String normalizePriceInput(String trimmed) {
        String work = trimmed.replace(" ", "").replace("\u202F", "");
        if (work.contains(",")) {
            int lastComma = work.lastIndexOf(',');
            String afterComma = work.substring(lastComma + 1);
            boolean noDot = work.indexOf('.') < 0;
            if (noDot
                    && afterComma.length() <= 2
                    && allDigits(afterComma)) {
                return work.replace(',', '.');
            }
            return work.replace(",", "");
        }
        return work;
    }

    private static boolean allDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static final class Result {

        private final boolean ok;
        private final double value;

        private Result(boolean ok, double value) {
            this.ok = ok;
            this.value = value;
        }

        public boolean ok() {
            return ok;
        }

        public double value() {
            return value;
        }

        public static Result ok(double value) {
            return new Result(true, value);
        }

        public static Result invalid() {
            return new Result(false, 0.0D);
        }
    }
}
