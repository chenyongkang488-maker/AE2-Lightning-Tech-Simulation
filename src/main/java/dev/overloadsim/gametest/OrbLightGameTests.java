package dev.overloadsim.gametest;

import dev.overloadsim.*;
import dev.overloadsim.multiblock.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class OrbLightGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void orbLightReachesFiveBlocksThroughGlass(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),7);c.checkStructure();
        c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();var pos=center(c);
        h.succeedWhen(()->{
            h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos.offset(5,0,0))==1,"real block light reaches distance5 through formed glass");
            h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos.offset(6,0,0))==0,"unobstructed light stops beyond distance5");
        });
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void lightOwnershipSurvivesReloadAndDefersUnloadedOwners(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),4);c.checkStructure();var pos=center(c);c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();
        var light=(SimulationLightBlockEntity)h.getLevel().getBlockEntity(pos);var saved=light.saveWithFullMetadata(h.getLevel().registryAccess());light.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(light.ownedBy(c),"owner UUID and position persist");light.recover();h.assertTrue(h.getLevel().getBlockEntity(pos)==light,"recovery retains valid source");
        var far=new BlockPos(30_000_000,100,30_000_000);saved.putLong("Owner",far.asLong());light.loadWithComponents(saved,h.getLevel().registryAccess());light.recover();
        h.assertTrue(h.getLevel().getBlockEntity(pos)==light&&!h.getLevel().hasChunkAt(far),"unloaded owner retained without loading chunks");
        light.bind(c);var forged=light.saveWithFullMetadata(h.getLevel().registryAccess());forged.putUUID("OwnerId",java.util.UUID.randomUUID());light.loadWithComponents(forged,h.getLevel().registryAccess());c.checkStructure();
        h.assertTrue(c.structure()==null&&h.getLevel().getBlockEntity(pos)==light,"foreign center source fails validation and is not deleted by controller");light.recover();h.assertTrue(h.getLevel().isEmptyBlock(pos),"orphan source removes only itself");h.succeed();
    }
    private static BlockPos center(SimulationControllerBlockEntity c){var s=c.structure();return s.min().offset((s.size()-1)/2,(s.size()-1)/2,(s.size()-1)/2);}
    @GameTest(template="empty")
    public static void idleOrbAndOwnedLightFollowActiveCapacity(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),3);c.checkStructure();var light=center(c);
        c.crystals().setStackInSlot(48,MultiblockGameTests.ironCrystal());c.tick();h.assertTrue(c.visualFlags()==0&&h.getLevel().isEmptyBlock(light),"recovery-only crystal does not create orb/light");
        c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();h.assertTrue((c.visualFlags()&1)!=0&&(c.visualFlags()&8)==0,"offline idle orb remains visible without working arcs");
        h.assertTrue(h.getLevel().getBlockState(light).getLightEmission(h.getLevel(),light)==6,"orb has real light emission6");c.checkStructure();h.assertTrue(c.structure()!=null,"owned center light allowed in validator");
        c.crystals().extractItem(0,1,false);c.tick();h.assertTrue(c.visualFlags()==0&&h.getLevel().isEmptyBlock(light),"last active crystal removes light despite recovery inventory");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void orbSurvivesCompletionAndBlockedOutput(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();h.assertTrue((c.visualFlags()&9)==9,"running job has orb and activity");
            for(int i=0;i<179;i++)c.tick();h.assertTrue((c.visualFlags()&1)!=0&&!c.busy(),"completion does not hide orb");c.tick();
            c.outputs().insert(new ItemStack(Items.STONE),128L*1024,false);for(int i=0;i<179;i++)c.tick();
            h.assertTrue((c.visualFlags()&1)!=0&&(c.visualFlags()&8)==0,"blocked completed job retains orb without working arcs");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void lightCleanupNeverDeletesReplacementBlocks(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),3);c.checkStructure();var light=center(c);c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.tick();
        h.assertTrue(h.getLevel().getBlockState(light).getLightEmission(h.getLevel(),light)==6,"owned light created");
        h.getLevel().setBlockAndUpdate(light,Blocks.STONE.defaultBlockState());c.checkStructure();h.assertTrue(c.structure()==null&&h.getLevel().getBlockState(light).is(Blocks.STONE),"invalidating structure preserves replacement");
        h.getLevel().setBlockAndUpdate(light,Blocks.AIR.defaultBlockState());c.checkStructure();c.tick();h.getLevel().setBlockAndUpdate(c.getBlockPos(),Blocks.AIR.defaultBlockState());
        h.assertTrue(h.getLevel().isEmptyBlock(light),"breaking controller cleans owned light");h.succeed();
    }
}
