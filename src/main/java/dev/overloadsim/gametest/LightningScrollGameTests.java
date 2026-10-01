package dev.overloadsim.gametest;

import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.tool.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class LightningScrollGameTests {
    private static ServerPlayer player(GameTestHelper h){return net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"lightning-scroll"));}

    @GameTest(template="empty")
    public static void creativePlayerRecordsFromActualArtificialStrike(GameTestHelper h){strike(h,false,4096);}

    @GameTest(template="empty")
    public static void canceledStrikeStillRecordsWithoutDamagingCreativePlayer(GameTestHelper h){strike(h,true,4096);}

    @GameTest(template="empty")
    public static void defaultChanceAcceptsRollBetweenTenAndThirtyThreePercent(GameTestHelper h){strike(h,false,6144);}

    private static void strike(GameTestHelper h,boolean cancel,long seed){
        var level=h.getLevel();var p=player(h);var pos=h.absolutePos(new BlockPos(5,2,5));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setGameMode(GameType.CREATIVE);level.addNewPlayer(p);
        var cow=h.spawn(EntityType.COW,new BlockPos(9,2,5));cow.setNoAi(true);
        p.setItemInHand(InteractionHand.OFF_HAND,ModContent.BLANK.toStack());float health=p.getHealth();
        var bolt=EntityType.LIGHTNING_BOLT.create(level);bolt.setPos(p.position());
        var seen=new java.util.concurrent.atomic.AtomicReference<EntityStruckByLightningEvent>();
        java.util.function.Consumer<EntityStruckByLightningEvent> listener=e->{if(e.getEntity()==p){seen.set(e);if(cancel)e.setCanceled(true);}};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,listener);
        try{
            level.random.setSeed(seed);double roll=level.random.nextDouble();
            h.assertTrue(roll<.33&&(seed!=6144||roll>=.1),"test roll must exercise requested probability: "+roll);level.random.setSeed(seed);
            bolt.tick();h.assertTrue(seen.get()!=null,"real lightning tick must deliver the player strike event");
            h.assertTrue(!cancel||seen.get().isCanceled(),"binding must not clear damage-protection cancellation");
            h.assertTrue(p.getOffhandItem().is(ModContent.BOUND.get()),"strike records even without damage; canceled="+cancel+", roll="+roll);
            var data=CrystalDataAccess.read(p.getOffhandItem()).orElseThrow();
            h.assertTrue(data.entityType().orElseThrow().toString().equals("minecraft:cow")&&data.strikes()==0,"record nearby cow, without cultivating on the binding strike");
            h.assertTrue(p.getHealth()==health,"creative player remains unharmed");
            bolt.tick();h.assertTrue(CrystalDataAccess.read(p.getOffhandItem()).orElseThrow().strikes()==0,"repeated ticks of one bolt cannot cultivate or reroll");
            var next=EntityType.LIGHTNING_BOLT.create(level);next.setPos(p.position());next.tick();next.discard();
            h.assertTrue(CrystalDataAccess.read(p.getOffhandItem()).orElseThrow().strikes()==1,"a distinct artificial bolt cultivates once");
        }finally{NeoForge.EVENT_BUS.unregister(listener);bolt.discard();cow.discard();p.discard();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void mobRuleDefaultsToThirtyThreeButHonorsPackOverride(GameTestHelper h){
        var empty=dev.overloadsim.data.SimulationRecipe.MobRule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();
        h.assertTrue(empty.probability()==.33&&dev.overloadsim.data.SimulationRecipe.MobRule.DEFAULT.probability()==.33,"omitted probability defaults to 33 percent");
        var override=new com.google.gson.JsonObject();override.addProperty("probability",.75);
        h.assertTrue(dev.overloadsim.data.SimulationRecipe.MobRule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,override).getOrThrow().probability()==.75,"pack authors retain explicit probability overrides");h.succeed();
    }

    private static ItemStack wrench(){var s=ModContent.COIL.toStack();CoilModules.setCore(s,com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack());CoilModules.install(s,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ModContent.id("wrench_module"))));CoilConfiguration.apply(s,5,1);s.set(ModContent.COIL_FE.get(),10000L);return s;}
    private static boolean cycle(ServerPlayer p,int slot,int step){return CoilWrench.cycleMode(p,slot,step);}
    @GameTest(template="empty")
    public static void shiftWheelCyclesBothDirectionsAndPreservesEnergyAndSettings(GameTestHelper h){
        var p=player(h);var s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);p.setShiftKeyDown(true);
        s.set(ModContent.COIL_SETTINGS.get(),new CoilSettings(false,true,4,3,false,true,0));var before=CoilSettings.read(s);long energy=CoilEnergy.read(s);
        h.assertTrue(cycle(p,p.getInventory().selected,1)&&CoilSettings.read(s).wrenchMode()==1,"up wheel selects Items");
        h.assertTrue(cycle(p,p.getInventory().selected,-1)&&CoilSettings.read(s).equals(before),"reverse wheel restores Wrench without altering other settings");
        h.assertTrue(cycle(p,p.getInventory().selected,-1)&&CoilSettings.read(s).wrenchMode()==7,"reverse wraps to Rotate");
        h.assertTrue(cycle(p,p.getInventory().selected,1)&&CoilSettings.read(s).wrenchMode()==0,"forward wraps to Wrench");
        h.assertTrue(CoilEnergy.read(s)==energy&&p.getInventory().selected==0,"cycling costs no FE and never switches the hotbar slot");h.succeed();
    }
    @GameTest(template="empty")
    public static void wheelRejectsMissingShiftInactiveWrenchAndStaleSlot(GameTestHelper h){
        var p=player(h);var s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);
        h.assertTrue(!cycle(p,0,1),"ordinary scroll is not a mode change");p.setShiftKeyDown(true);
        h.assertTrue(!cycle(p,1,1)&&!cycle(p,0,0)&&!cycle(p,0,9),"stale slot and invalid steps rejected");
        CoilConfiguration.apply(s,5,0);h.assertTrue(!cycle(p,0,1),"disabled wrench rejected");CoilConfiguration.apply(s,5,1);
        CoilModules.setCore(s,ItemStack.EMPTY);h.assertTrue(!cycle(p,0,1),"coreless coil rejected");
        s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);CoilModules.uninstall(s,"wrench_module");h.assertTrue(!cycle(p,0,1),"module removal takes effect immediately");
        s=wrench();p.setItemInHand(InteractionHand.OFF_HAND,s);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertTrue(!cycle(p,0,1),"only the held main-hand tool receives wheel changes");
        p.setItemInHand(InteractionHand.MAIN_HAND,s);p.containerMenu=new CoilMenu(2,p.getInventory(),0);h.assertTrue(!cycle(p,0,1),"open settings GUI cannot receive wheel packets");
        h.assertTrue(CoilSettings.read(s).wrenchMode()==0,"rejected commands leave mode unchanged");h.succeed();
    }
}
