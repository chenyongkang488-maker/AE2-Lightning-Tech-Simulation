package dev.overloadsim.output.tool;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

/** Pure connector authority and addition rules; Minecraft adapters validate world state separately. */
public final class OutputToolRules {
    private OutputToolRules() {}
    public static int cycle(int current, int steps) {
        if (!validSourceSlot(current) || steps == 0 || steps < -8 || steps > 8)
            throw new IllegalArgumentException("Invalid source slot or wheel steps");
        return Math.floorMod(current + steps, 36);
    }
    public static boolean validSourceSlot(int slot) { return slot >= 0 && slot < 36; }
    public static boolean canCycle(boolean alive, boolean spectator, boolean shift, boolean inventoryMenu,
            int sentSlot, int heldSlot, int steps) {
        return alive && !spectator && shift && inventoryMenu && sentSlot >= 0 && sentSlot <= 8
                && sentSlot == heldSlot && steps != 0 && steps >= -8 && steps <= 8;
    }
    public record AddPlan<T>(List<T> add, int existing, int full, int rejected) {}
    public static <T> AddPlan<T> planAdd(Collection<T> current, Collection<T> requested, int maximum, Predicate<T> allowed) {
        var seen = new LinkedHashSet<>(current);
        var add = new ArrayList<T>();
        int existing = 0, full = 0, rejected = 0;
        for (T target : new LinkedHashSet<>(requested)) {
            if (seen.contains(target)) existing++;
            else if (seen.size() >= maximum) full++;
            else if (!allowed.test(target)) rejected++;
            else { seen.add(target); add.add(target); }
        }
        return new AddPlan<>(List.copyOf(add), existing, full, rejected);
    }
    public static <T> AddPlan<T> planAdd(Collection<T> current, Collection<T> requested, int maximum) {
        return planAdd(current, requested, maximum, target -> true);
    }
}
