package dev.overloadsim.core;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Exact server accounting for one beam batch. No floating-point resource values. */
public final class LaserMath {
    public static final long MAX_BEAM = 1_000_000_000_000_000L;
    public static final long PARTICLE_COST = 512_000_000_000L;
    public static final long CORE_THRESHOLD = 1_000_000_000L;
    private static final Pattern ENERGY = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([kGP]?)(?:FE)?");

    private LaserMath() {}

    public static long boost(long power, long add) {
        beam(power);
        nonnegative(add);
        if (add > MAX_BEAM - power) throw new IllegalArgumentException("beam exceeds 1PFE");
        return power + add;
    }

    public static long amplify(long power) {
        beam(power);
        if (power > MAX_BEAM / 2) throw new IllegalArgumentException("beam exceeds 1PFE");
        return power * 2;
    }

    /** The caller supplies its five eligible output faces in a stable order. */
    public static long[] split(long power, int branches) {
        beam(power);
        if (branches < 1 || branches > 5) throw new IllegalArgumentException("expected one to five outputs");
        var result = new long[branches];
        long share = power / branches;
        long extra = power % branches;
        for (int i = 0; i < branches; i++) result[i] = share + (i < extra ? 1 : 0);
        return result;
    }

    public record Accumulation(long particles, long remainder) {}

    public static Accumulation accumulate(long remainder, long energy) {
        if (remainder < 0 || remainder >= PARTICLE_COST) throw new IllegalArgumentException("invalid core remainder");
        nonnegative(energy);
        // Divide first, so even Long.MAX_VALUE input plus a paid remainder cannot overflow.
        long residual = remainder + energy % PARTICLE_COST;
        return new Accumulation(energy / PARTICLE_COST + residual / PARTICLE_COST, residual % PARTICLE_COST);
    }

    /** k=10^3, G=10^9, P=10^15; fractional units must resolve to whole FE. */
    public static long parseEnergy(String value) {
        if (value == null) throw new IllegalArgumentException("missing energy");
        var match = ENERGY.matcher(value.trim());
        if (!match.matches()) throw new IllegalArgumentException("invalid energy: " + value);
        int scale = switch (match.group(2)) { case "k" -> 3; case "G" -> 9; case "P" -> 15; default -> 0; };
        try {
            return new BigDecimal(match.group(1)).movePointRight(scale).longValueExact();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("energy must be an exact nonnegative long", failure);
        }
    }

    private static void beam(long value) {
        if (value < 0 || value > MAX_BEAM) throw new IllegalArgumentException("invalid beam energy");
    }

    static void nonnegative(long value) {
        if (value < 0) throw new IllegalArgumentException("negative resource amount");
    }
}
