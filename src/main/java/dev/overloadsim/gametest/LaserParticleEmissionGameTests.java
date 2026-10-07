package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserParticleEmissionGameTests {
    private record Chain(LaserMachineBlockEntity source, LaserMachineBlockEntity focus,
            LaserMachineBlockEntity core, LaserMachineBlockEntity receiver) {}

    private static Chain chain(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(3,5,10)), LaserKind.EMITTER, Direction.WEST);
        var focus = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(9,2,10)), LaserKind.FOCUS, Direction.WEST);
        var core = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(16,5,10)), LaserKind.CORE, Direction.WEST);
        var receiver = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(22,2,10)), LaserKind.RECEIVER, Direction.WEST);
        for (var machine : new LaserMachineBlockEntity[]{source, focus, core, receiver}) machine.checkStructure();
        source.energy().load(1_000_000);
        return new Chain(source, focus, core, receiver);
    }

    private static void batch(Chain chain, long output) {
        Consumer<LaserEvents.BeforeModifier> boost = event -> {
            if (event.position.equals(chain.focus().getBlockPos())) event.setOutputEnergy(output);
        };
        NeoForge.EVENT_BUS.addListener(boost);
        try { LaserNetwork.processEmitter(chain.source()); }
        finally { NeoForge.EVENT_BUS.unregister(boost); }
    }

    @GameTest(template="laser_empty")
    public static void subParticleAccumulationCreatesNoDeliverySegment(GameTestHelper h) {
        var chain = chain(h);
        try {
            batch(chain, 1_000_000_000L);
            h.assertTrue(chain.core().coreRemainder() == 1_000_000_000L, "eligible input accumulates exact FE before any particle exists");
            h.assertTrue(chain.receiver().particles().stored() == 0, "a sub-particle batch settles no stored particles");
            h.assertTrue(chain.source().segments().stream().noneMatch(LaserSegment::particle), "a sub-particle batch launches no delivery animation");
        } finally { chain.source().setEnabled(false); }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void zeroCountCannotActivateAReceiver(GameTestHelper h) {
        var chain = chain(h);
        try {
            chain.core().acceptCore(LaserMath.CORE_THRESHOLD, Direction.EAST, h.getLevel().getGameTime());
            var delivered = LaserNetwork.sendParticles(chain.core(), 0, Direction.EAST, h.getLevel().getGameTime());
            h.assertTrue(delivered == null && chain.receiver().currentPower() == 0, "zero particle output creates no receiver delivery lease");
            h.assertTrue(chain.receiver().particles().stored() == 0, "zero output never changes storage");
        } finally { chain.source().setEnabled(false); }
        h.succeed();
    }

    @GameTest(template="laser_empty",timeoutTicks=20)
    public static void cancelledCoreBatchCannotReusePreviousDeliveryVisuals(GameTestHelper h) {
        var chain = chain(h);
        batch(chain, LaserMath.PARTICLE_COST);
        h.assertTrue(chain.receiver().particles().stored() == 1, "a first real particle is received immediately");
        // Per-tick optical input claims must expire before a second production attempt.
        int delay = h.getLevel().getGameTime() % 20 == 19 ? 2 : 1;
        h.runAfterDelay(delay, () -> {
            int[] calls = {0};
            Consumer<LaserEvents.BeforeCoreEmission> cancel = event -> {
                if (event.position.equals(chain.core().getBlockPos())) { calls[0]++; event.setCanceled(true); }
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                batch(chain, LaserMath.PARTICLE_COST);
                h.assertTrue(calls[0] == 1, "the second batch actually reaches the cancellable core transaction");
                h.assertTrue(chain.receiver().particles().stored() == 1, "cancelled work creates no additional resource");
                h.assertTrue(chain.source().segments().stream().noneMatch(LaserSegment::particle), "a previous live core lease cannot invent a new delivery animation");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); chain.receiver().particles().load(0); chain.source().setEnabled(false); }
            h.succeed();
        });
    }

    @GameTest(template="laser_empty")
    public static void extensionZeroedParticleOutputCreatesNoDeliverySegment(GameTestHelper h) {
        var chain = chain(h);
        Consumer<LaserEvents.BeforeCoreEmission> zero = event -> {
            if (event.position.equals(chain.core().getBlockPos())) event.setParticles(0);
        };
        NeoForge.EVENT_BUS.addListener(zero);
        try {
            batch(chain, LaserMath.PARTICLE_COST);
            h.assertTrue(chain.receiver().particles().stored() == 0, "extension-zeroed production creates no stored resource");
            h.assertTrue(chain.source().segments().stream().noneMatch(LaserSegment::particle), "extension-zeroed production creates no launched visual");
        } finally { NeoForge.EVENT_BUS.unregister(zero); chain.source().setEnabled(false); }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void positiveProductionSettlesTheExactBatchImmediately(GameTestHelper h) {
        var chain = chain(h);
        try {
            batch(chain, 1_537_000_000_000L);
            h.assertTrue(chain.receiver().particles().stored() == 3 && chain.core().coreRemainder() == 1_000_000_000L,
                    "three particles settle immediately and the extra 1GFE remains in the core");
            h.assertTrue(chain.source().segments().stream().filter(LaserSegment::particle).count() == 1,
                    "one positive production batch owns one bounded delivery segment");
            var flight=chain.source().segments().stream().filter(LaserSegment::particle).findFirst().orElseThrow();
            h.assertTrue(flight.particleCount()==3&&flight.launchTick()==h.getLevel().getGameTime(),
                    "the visual snapshot carries the exact committed production count and original launch timestamp");
        } finally { chain.receiver().particles().load(0); chain.source().setEnabled(false); }
        h.succeed();
    }
}
