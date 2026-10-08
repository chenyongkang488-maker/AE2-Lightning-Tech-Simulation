package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class ParticleThroughputGameTests {
    private static LaserMachineBlockEntity receiver(GameTestHelper h) {
        var receiver=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,2,8)),LaserKind.RECEIVER,Direction.NORTH);
        receiver.checkStructure(); receiver.particles().load(100);
        receiver.fuelInput().insertItem(0,new ItemStack(ModContent.TRANSCENDENT.get(),2),false);
        return receiver;
    }

    @GameTest(template="laser_empty")
    public static void cancellationPreservesFullBatchAndFractionalQuota(GameTestHelper h) {
        var receiver=receiver(h);
        java.util.function.Consumer<LaserEvents.BeforeParticleConversion> cancel=e->{if(e.position.equals(receiver.getBlockPos())) e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            ParticleConversion.tick(receiver);
            h.assertTrue(receiver.particles().stored()==100&&receiver.energy().stored()==0&&receiver.fuelProcessingCredit()==0
                &&receiver.fuelRemaining()==0&&receiver.fuelInput().getStackInSlot(0).getCount()==2,"cancelled throughput consumes no item, particle or fractional quota");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        ParticleConversion.tick(receiver);
        h.assertTrue(receiver.particles().stored()==88&&receiver.energy().stored()==24_576_000_000_000L
            &&receiver.fuelRemaining()==99&&receiver.fuelProcessingCredit()==2_120_000_000_000L,"retry settles precisely one twelve-particle batch");
        receiver.particles().load(0);receiver.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void conversionCallbackCannotReenterThePaidClock(GameTestHelper h) {
        var receiver=receiver(h); var callbacks=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<LaserEvents.BeforeParticleConversion> reenter=e->{
            if(e.position.equals(receiver.getBlockPos())&&callbacks.incrementAndGet()==1)ParticleConversion.tick(receiver);
        };
        NeoForge.EVENT_BUS.addListener(reenter);
        try { ParticleConversion.tick(receiver); } finally { NeoForge.EVENT_BUS.unregister(reenter); }
        h.assertTrue(callbacks.get()==1&&receiver.particles().stored()==88&&receiver.energy().stored()==24_576_000_000_000L
            &&receiver.fuelRemaining()==99&&receiver.fuelInput().getStackInSlot(0).getCount()==1,"one event and one paid conversion despite callback reentry");
        receiver.particles().load(0);receiver.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void reducedParticleBatchCannotKeepTheOriginalBatchYield(GameTestHelper h) {
        var receiver=receiver(h);
        java.util.function.Consumer<LaserEvents.BeforeParticleConversion> reduce=e->{if(e.position.equals(receiver.getBlockPos()))e.setParticles(1);};
        NeoForge.EVENT_BUS.addListener(reduce);
        try { ParticleConversion.tick(receiver); } finally { NeoForge.EVENT_BUS.unregister(reduce); }
        h.assertTrue(receiver.particles().stored()==99&&receiver.energy().stored()==2_048_000_000_000L,"reducing a twelve-particle transaction to one also caps its yield to one particle");
        receiver.particles().load(0);receiver.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void oldPaidIntervalWindowUsesNewRateWithoutRenewingItsItem(GameTestHelper h) {
        var receiver=receiver(h);
        receiver.fuelInput().setStackInSlot(0,new ItemStack(Items.NETHERITE_BLOCK,2));
        var saved=receiver.saveMachine(h.getLevel().registryAccess());
        saved.putString("ActiveFuel","overload_sim:netherite_block");saved.putInt("FuelRemaining",72);saved.putInt("ParticleRemaining",12);
        saved.remove("FuelProcessingCredit");receiver.loadMachine(saved,h.getLevel().registryAccess());receiver.checkStructure();
        ParticleConversion.tick(receiver);
        h.assertTrue(receiver.fuelRemaining()==71&&receiver.particleRemaining()==639&&receiver.fuelProcessingCredit()==16_000_000_000L
            &&receiver.fuelInput().getStackInSlot(0).getCount()==2&&receiver.energy().stored()==0,"old paid duration survives migration and starts only the new exact rate credit");
        var next=receiver.saveMachine(h.getLevel().registryAccess());receiver.loadMachine(next,h.getLevel().registryAccess());receiver.checkStructure();
        ParticleConversion.tick(receiver);
        h.assertTrue(receiver.fuelRemaining()==70&&receiver.particleRemaining()==638&&receiver.fuelProcessingCredit()==32_000_000_000L,"save/reload keeps the fractional processing capacity and its countdown");
        receiver.particles().load(0);receiver.setEnabled(false);h.succeed();
    }
}
