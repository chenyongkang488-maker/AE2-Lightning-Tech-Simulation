package dev.overloadsim.core;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** One finite flight for every real produced particle; repeated snapshots never restart it. */
public final class ParticleFlightVisuals {
    public static final int DURATION = 8;
    public static final int MAX_BATCH = (int) (LaserMath.MAX_BEAM / LaserMath.PARTICLE_COST + 1);
    private ParticleFlightVisuals() {}

    public static int visibleCount(long particles, long launch, double time) {
        if (particles <= 0 || particles > MAX_BATCH || launch < 0 || launch > Long.MAX_VALUE - DURATION
                || !Double.isFinite(time) || time < launch || time >= launch + DURATION) return 0;
        return (int) particles;
    }

    public static double progress(long launch, double time) {
        if (launch < 0 || !Double.isFinite(time) || time < launch) return 0;
        return Math.clamp((time - launch) / DURATION, 0, 1);
    }

    /** Client-local bounded history survives empty snapshots and never restarts retired flights. */
    public static final class Clock {
        private final Map<Long,Boolean> flights = new LinkedHashMap<>();
        private double previousTime = Double.NaN;

        public Clock() {}

        public void beginFrame(double time, long[] launches) {
            if (launches.length > 128) throw new IllegalArgumentException("particle flight snapshot exceeds segment budget");
            boolean rewound = Double.isFinite(previousTime) && Double.isFinite(time) && time < previousTime;
            var current = new HashSet<Long>();
            for (long launch : launches) {
                if (launch < 0 || launch > Long.MAX_VALUE - DURATION) continue;
                current.add(launch);
            }
            for (var entry : flights.entrySet())
                if (rewound || !current.contains(entry.getKey())) entry.setValue(true);
            for (long launch : launches)
                if (current.contains(launch)) flights.putIfAbsent(launch,false);
            while (flights.size()>128) flights.remove(flights.keySet().iterator().next());
            if (Double.isFinite(time)) previousTime = time;
        }

        public int visibleCount(long particles, long launch, double time) {
            return Boolean.TRUE.equals(flights.get(launch)) ? 0 : ParticleFlightVisuals.visibleCount(particles, launch, time);
        }
    }
}
