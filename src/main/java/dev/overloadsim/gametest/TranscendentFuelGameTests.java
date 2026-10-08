package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class TranscendentFuelGameTests {
    private static final long FE_PER_PARTICLE = 2_048_000_000_000L;
    private static LaserMachineBlockEntity receiver(GameTestHelper h) {
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("overload_sim:transcendent_crystal")).orElseThrow();
        var receiver = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8,2,8)), LaserKind.RECEIVER, Direction.NORTH);
        receiver.checkStructure();
        h.assertTrue(receiver.fuelInput().insertItem(0, new ItemStack(item,2), false).isEmpty(), "crystal is accepted by the existing sealed fuel input");
        return receiver;
    }

    @GameTest(template="laser_empty")
    public static void crystalPaysOneItemForOneHundredEffectiveParticleTicks(GameTestHelper h) {
        var receiver = receiver(h);
        try {
            receiver.particles().load(1250);
            for (int tick=0;tick<100;tick++) ParticleConversion.tick(receiver);
            h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==1 && receiver.fuelRemaining()==0,
                    "one crystal funds exactly 100 effective ticks");
            h.assertTrue(receiver.particles().stored()==30 && receiver.energy().stored()==1220*FE_PER_PARTICLE
                    && receiver.fuelProcessingCredit()==7_200_000_000_000L,
                    "100 effective ticks at 0.125P per twenty-tick batch convert 1220 whole particles and preserve the fraction");
            ParticleConversion.tick(receiver);
            h.assertTrue(receiver.fuelInput().getStackInSlot(0).isEmpty() && receiver.fuelRemaining()==99
                    && receiver.particles().stored()==18 && receiver.energy().stored()==1232*FE_PER_PARTICLE,
                    "tick 101 pays for the next crystal window once");
        } finally { receiver.particles().load(0); receiver.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void crystalWindowPausesAndSurvivesReloadWithoutRenewal(GameTestHelper h) {
        var receiver = receiver(h);
        try {
            ParticleConversion.tick(receiver);
            h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==2 && receiver.fuelRemaining()==0,
                    "missing particles consume no crystal");
            receiver.particles().load(125);
            receiver.energy().load(receiver.energy().capacity()-FE_PER_PARTICLE+1);
            ParticleConversion.tick(receiver);
            h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==2 && receiver.particles().stored()==125,
                    "insufficient FE space consumes neither crystal nor particle");
            receiver.energy().load(0);
            ParticleConversion.tick(receiver);
            var saved = receiver.saveMachine(h.getLevel().registryAccess());
            receiver.loadMachine(saved,h.getLevel().registryAccess()); receiver.checkStructure();
            ParticleConversion.tick(receiver);
            h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==1 && receiver.fuelRemaining()==98
                    && receiver.particles().stored()==101 && receiver.energy().stored()==24*FE_PER_PARTICLE
                    && receiver.fuelProcessingCredit()==4_240_000_000_000L,
                    "reload retains the paid crystal window without charging or resetting it");
        } finally { receiver.particles().load(0); receiver.setEnabled(false); }
        h.succeed();
    }
}
