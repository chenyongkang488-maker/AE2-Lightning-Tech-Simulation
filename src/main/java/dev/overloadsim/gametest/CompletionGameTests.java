package dev.overloadsim.gametest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.multiblock.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class CompletionGameTests {
    @GameTest(template="empty")
    public static void automationCannotExtractTemplates(GameTestHelper h) {
        var c=MultiblockGameTests.build(h,h.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),3);c.checkStructure();
        c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());
        c.outputs().insert(new ItemStack(Items.RAW_IRON),70,false);
        h.assertTrue(c.automation().extractItem(0,1,true).isEmpty(),"simulated wireless export cannot offer a template");
        h.assertTrue(c.automation().extractItem(0,1,false).isEmpty()&&!c.crystals().getStackInSlot(0).isEmpty(),"external extraction leaves template");
        h.assertTrue(c.automation().extractItem(49,64,false).getCount()==64&&c.outputs().count(0)==6,"external extraction still returns outputs");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void workingCrystalEditAbortsWithoutPayment(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(2_000_000));
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();
            h.assertTrue(c.batch()!=null&&c.batch().remaining==179,"job progresses");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1000&&c.energy().getEnergyStored()==2_000_000,"progress reserves no payment");
            var removed=c.crystals().extractItem(0,1,false);
            h.assertTrue(!removed.isEmpty()&&c.batch()==null&&c.outputs().count(0)==0,"successful crystal edit aborts without output");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1000,"cancelled work costs no lightning");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void insertingCrystalDuringWorkAbortsWholeBatch(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();
            var remainder=c.crystals().insertItem(1,MultiblockGameTests.ironCrystal(),false);
            h.assertTrue(remainder.isEmpty()&&c.batch()==null&&!c.crystals().getStackInSlot(1).isEmpty(),"insert accepted during work and cancels whole batch");
            h.assertTrue(c.outputs().count(0)==0&&c.bridge().extract(false,Long.MAX_VALUE,true)==1000,"aborted batch neither outputs nor charges");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void simulatedAndFailedEditsKeepProgress(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();var job=c.batch();int remaining=job.remaining;
            c.crystals().extractItem(0,1,true);c.crystals().insertItem(1,MultiblockGameTests.ironCrystal(),true);
            c.crystals().insertItem(0,MultiblockGameTests.ironCrystal(),false);c.crystals().insertItem(1,new ItemStack(Items.STONE),false);
            h.assertTrue(c.batch()==job&&job.remaining==remaining&&c.crystals().getStackInSlot(1).isEmpty(),"queries/rejected edits cannot reset work");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void completionDebitsFeesExactlyOnce(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(2_000_000));c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());
            for(int i=0;i<179;i++)c.tick();
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1000&&c.outputs().count(0)==0,"incomplete work has no debit or output");
            c.tick();h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==999&&c.energy().getEnergyStored()==1_999_000&&c.outputs().count(0)==1,"completion commits exact FE and HV once");
            c.tick();h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==999&&c.outputs().count(0)==1,"next job does not recharge prior batch");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void crystalQuickMoveDuringWork(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();
            var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"crystal-shift"));player.setPos(c.getBlockPos().getCenter());
            var menu=new MultiblockSimulationMenu(1,player.getInventory(),c);player.containerMenu=menu;menu.broadcastChanges();
            menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(c.crystals().getStackInSlot(0).isEmpty()&&c.batch()==null,"Shift removes crystal during work");
            player.getInventory().clearContent();player.getInventory().setItem(0,MultiblockGameTests.ironCrystal());
            menu.clicked(menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).getFirst().index,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(!c.crystals().getStackInSlot(0).isEmpty()&&player.getInventory().getItem(0).isEmpty(),"Shift puts crystal into active input");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void beforeCommitInputChangePreventsStaleOutput(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());for(int i=0;i<179;i++)c.tick();
            java.util.function.Consumer<dev.overloadsim.api.MultiblockSimulationEvents.BeforeBatchCommit> edit=e->{if(e.controller==c)c.crystals().extractItem(0,1,false);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(edit);
            try{c.tick();}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(edit);}
            h.assertTrue(c.batch()==null&&c.crystals().getStackInSlot(0).isEmpty()&&c.outputs().count(0)==0,"commit callback edit cancels stale output");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1000,"cancel in callback costs no lightning");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void cancelledCommitCanResumeWithoutCharging(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());for(int i=0;i<179;i++)c.tick();
            java.util.function.Consumer<dev.overloadsim.api.MultiblockSimulationEvents.BeforeBatchCommit> veto=e->{if(e.controller==c)e.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(veto);
            try{c.tick();}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(veto);}
            h.assertTrue(c.batch()!=null&&c.batch().remaining==0&&c.outputs().count(0)==0&&c.bridge().extract(false,Long.MAX_VALUE,true)==1000,"veto retains outcome without debit");
            c.tick();h.assertTrue(c.outputs().count(0)==1&&c.bridge().extract(false,Long.MAX_VALUE,true)==999,"retry commits once");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void legacyPaidEditRefundsIncludingFullFEBuffer(GameTestHelper h) {
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(2_000_000));
            var job=new SimulationBatch(List.of(new SimulationBatch.Output(new ItemStack(Items.RAW_IRON),1)),Map.of(0,MultiblockGameTests.ironCrystal()),180,new MultiblockRules.Costs(1000,1,0),false,false,"legacy");job.paid=true;job.remaining=90;
            var tag=c.saveMachine(h.getLevel().registryAccess());var savedJob=job.save(h.getLevel().registryAccess());savedJob.remove("SettlementVersion");tag.put("Batch",savedJob);c.loadMachine(tag,h.getLevel().registryAccess());
            h.assertTrue(!c.crystals().extractItem(0,1,false).isEmpty()&&c.batch()==null,"legacy paid crystal may be removed");
            var refunded=c.saveMachine(h.getLevel().registryAccess());
            h.assertTrue(refunded.getLong("FeCredit")==1000&&refunded.getLong("RefundHv")==1,"full FE and HV refund survives in journal");
            c.loadMachine(refunded,h.getLevel().registryAccess());c.tick();
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1001&&c.outputs().count(0)==0,"refund delivered without stale production");h.succeed();
        });
    }
}
