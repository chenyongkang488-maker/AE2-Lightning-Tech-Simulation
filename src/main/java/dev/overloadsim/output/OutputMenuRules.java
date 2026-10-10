package dev.overloadsim.output;

/** Bounds shared by the menu's server transactions and its client controls. */
public final class OutputMenuRules {
    public static final int SLOTS = 36;
    public static final int SLOTS_PER_PAGE = 18;
    private OutputMenuRules() {}

    public static boolean visibleSlot(int slot, int page) {
        return slot >= 0 && slot < SLOTS && page >= 0 && page < 2 && slot / SLOTS_PER_PAGE == page;
    }

    /** A held stack may only receive matching items; it is never offered to the network. */
    public static long extractionLimit(boolean cursorEmpty, boolean sameKey, int carried,
                                       int maxStack, long available, boolean half) {
        if (maxStack <= 0 || available <= 0 || carried < 0 || carried > maxStack
                || !cursorEmpty && !sameKey) return 0;
        long amount = Math.min(available, maxStack - carried);
        return half && amount > 0 ? (amount + 1) / 2 : amount;
    }

    public static long parseAmount(String value, long maximum) {
        if (value == null || value.isEmpty() || value.length() > 19 || maximum < 1) return -1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) < '0' || value.charAt(i) > '9') return -1;
        try {
            long amount = Long.parseLong(value);
            return amount >= 1 && amount <= maximum ? amount : -1;
        } catch (NumberFormatException ignored) { return -1; }
    }
}
