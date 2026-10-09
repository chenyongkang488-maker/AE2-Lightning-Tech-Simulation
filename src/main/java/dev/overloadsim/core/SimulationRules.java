package dev.overloadsim.core;

/** Pure rules shared by gameplay and pack-facing validation. */
public final class SimulationRules {
    private SimulationRules() {}
    public static int parallel(int matrices) {
        if (matrices < 0 || matrices > 32) throw new IllegalArgumentException("matrix count must be 0..32");
        return Math.max(1, matrices * 4);
    }
    public static int duration(int ticks, int cards) {
        if (ticks < 1 || cards < 0 || cards > 4) throw new IllegalArgumentException("invalid duration/cards");
        return Math.max(1, (int)(((long)ticks + (1L << cards) - 1) / (1L << cards)));
    }
    public static long batchEnergy(long perOperation, int parallel) {
        if (perOperation < 0 || parallel < 1 || parallel > 256) throw new IllegalArgumentException("invalid cost/parallel");
        return Math.multiplyExact(perOperation, parallel);
    }
    /** Ceil a nonnegative percentage without overflowing intermediate multiplication. */
    public static long scalePercent(long amount,int percent){
        if(amount<0||percent<0||percent>20000)throw new IllegalArgumentException("invalid amount/percentage");
        return Math.addExact(Math.multiplyExact(amount/100,percent),((amount%100)*percent+99)/100);
    }
    public static boolean withinRadius(double distanceSquared, double radius) {
        return distanceSquared >= 0 && radius >= 0 && distanceSquared <= radius * radius;
    }
    public static boolean chance(double roll, double probability) {
        if (probability < 0 || probability > 1 || roll < 0 || roll >= 1) throw new IllegalArgumentException("invalid probability");
        return roll < probability;
    }
}
