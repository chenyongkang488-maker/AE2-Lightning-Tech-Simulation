package dev.overloadsim.gametest;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.MultiblockSimulationMenu;
import dev.overloadsim.multiblock.SimulationAutoConstruction;
import dev.overloadsim.multiblock.SimulationControllerBlock;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.SimulationMemberBlockEntity;
import dev.overloadsim.multiblock.SimulationStructureIndex;
import dev.overloadsim.multiblock.SimulationStructureValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class ChamberAutoConstructionGameTests {
    @GameTest(template = "empty")
    public static void everySizeAndFacingUsesValidBottomFrontController(GameTestHelper h) {
        // Hand-counted: external size, frames (controller excluded), glass, air including upgrade floor.
        int[][] cases = {{3,20,4,2},{4,35,16,12},{5,52,36,36},{6,71,64,80},{7,92,100,150}};
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));
        for (int[] test : cases) for (Direction facing : Direction.Plane.HORIZONTAL) {
            clear(h,min,7);
            var host = controller(h,min,test[0],facing);
            var plan = SimulationAutoConstruction.plan(host,test[0]);
            h.assertTrue(plan.cells().size() == test[0]*test[0]*test[0], "plan reserves entire chamber including its interior");
            h.assertTrue(plan.cells().values().stream().filter(s -> s.is(MultiblockContent.FRAME.get())).count() == test[1], "correct frame count for size "+test[0]);
            h.assertTrue(plan.cells().values().stream().filter(s -> s.is(glass())).count() == test[2], "correct glass count for size "+test[0]);
            h.assertTrue(plan.cells().values().stream().filter(s -> s.isAir()).count() == test[3], "correct empty interior for size "+test[0]);
            for (int x=1;x<test[0]-1;x++) for (int z=1;z<test[0]-1;z++)
                h.assertTrue(plan.cells().get(min.offset(x,0,z)).isAir(), "bottom interior is reserved for manual upgrades");
            h.assertTrue(plan.cells().get(min).is(MultiblockContent.FRAME.get()), "bottom corners remain structural frames");
            h.assertTrue(plan.available().getAsBoolean(), "unowned chamber is available");
            plan.cells().forEach((pos,state) -> { if (!pos.equals(host.getBlockPos())) h.getLevel().setBlockAndUpdate(pos,state); });
            var validation = SimulationStructureValidator.validate(host,min,test[0]);
            h.assertTrue(validation.valid(), "planned chamber validates at the expected bounds for "+facing+": "+validation.error());
            h.assertTrue(host.getBlockState().getValue(SimulationControllerBlock.FACING) == facing, "controller facing is preserved");
        }
        clear(h,min,7);h.succeed();
    }

    @GameTest(template = "empty")
    public static void partialBuildPreservesUpgradesAndSingleInterface(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));
        var host = controller(h,min,5,Direction.NORTH);
        var module = min.offset(1,0,1);var port = min.offset(0,0,2);var existingFrame = min.offset(4,4,4);
        h.getLevel().setBlockAndUpdate(module,MultiblockContent.T3.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(port,overloadedInterface().defaultBlockState());
        h.getLevel().setBlockAndUpdate(existingFrame,MultiblockContent.FRAME.get().defaultBlockState());
        var moduleEntity = h.getLevel().getBlockEntity(module);var portEntity = h.getLevel().getBlockEntity(port);
        var player = player(h,host);supply(player,frameItem(),64);supply(player,glass().asItem(),40);
        var result = SimulationAutoConstruction.build(player,host,5);
        h.assertTrue(result.successful() && result.placed() == 86, "only missing 50 frames and 36 glass are placed: "+result.reason());
        h.assertTrue(count(player,frameItem()) == 14 && count(player,glass().asItem()) == 4, "optional floor slots consume no frames and existing shell parts reduce material requirements");
        h.assertTrue(h.getLevel().getBlockEntity(module) == moduleEntity && h.getLevel().getBlockEntity(port) == portEntity, "existing upgrades and interface retain their entities");
        h.assertTrue(host.structure() != null && host.structure().t3() == 1 && host.structure().networkInterface().equals(port), "preserved optional parts participate in the formed chamber");
        var again = SimulationAutoConstruction.build(player,host,7);
        h.assertTrue(again.successful() && again.placed() == 0 && host.structure().size() == 5, "formed chamber is a no-op even if a different size is requested");
        h.assertTrue(count(player,frameItem()) == 14 && count(player,glass().asItem()) == 4, "repeat build consumes nothing");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyUpgradeFloorFormsThenAcceptsAndRemovesManualUpgrades(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,5,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),52);supply(player,glass().asItem(),36);
        var result = SimulationAutoConstruction.build(player,host,5);
        h.assertTrue(result.successful() && result.placed() == 88 && host.structure() != null,
                "shell forms with exactly 52 frames and 36 glass, without filling upgrade slots: "+result.reason());
        for (int x=1;x<4;x++) for (int z=1;z<4;z++)
            h.assertTrue(h.getLevel().getBlockState(min.offset(x,0,z)).isAir(), "formed floor remains open for upgrades");
        var upgrade = min.offset(2,0,2);
        h.getLevel().setBlockAndUpdate(upgrade,MultiblockContent.T3.get().defaultBlockState());host.checkStructure();
        h.assertTrue(host.structure() != null && host.structure().t3() == 1, "manually installed upgrade participates in the formed chamber");
        h.getLevel().setBlockAndUpdate(upgrade,Blocks.AIR.defaultBlockState());host.checkStructure();
        h.assertTrue(host.structure() != null && host.structure().t3() == 0, "removing an optional upgrade leaves the shell valid");
        h.assertTrue(count(player,frameItem()) == 0 && count(player,glass().asItem()) == 0, "build spends only the structural shell materials");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void openFloorStillRequiresBottomBorderAndRoof(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        SimulationAutoConstruction.plan(host,3).cells().forEach((pos,state) -> { if (!pos.equals(host.getBlockPos())) h.getLevel().setBlockAndUpdate(pos,state); });
        var floor = min.offset(1,0,1);h.getLevel().setBlockAndUpdate(floor,Blocks.AIR.defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(host,min,3).valid(), "empty bottom interior is a legal upgrade slot");
        h.getLevel().setBlockAndUpdate(min,Blocks.AIR.defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(host,min,3).error().equals("edge"), "empty floor does not waive bottom corners");
        h.getLevel().setBlockAndUpdate(min,MultiblockContent.FRAME.get().defaultBlockState());
        var border = min.offset(0,0,1);h.getLevel().setBlockAndUpdate(border,Blocks.AIR.defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(host,min,3).error().equals("edge"), "bottom border remains structural");
        h.getLevel().setBlockAndUpdate(border,MultiblockContent.FRAME.get().defaultBlockState());
        var roof = min.offset(1,2,1);h.getLevel().setBlockAndUpdate(roof,Blocks.AIR.defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(host,min,3).error().equals("roof"), "roof interior still requires frames");
        h.getLevel().setBlockAndUpdate(roof,MultiblockContent.FRAME.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(host,min,3).error().equals("floor"), "air floor support does not accept unrelated solid blocks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void existingBottomFramesAndEveryUpgradeKindRemainIntact(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,5,Direction.NORTH);
        Block[] parts = {MultiblockContent.FRAME.get(),MultiblockContent.T1.get(),MultiblockContent.T2.get(),
                MultiblockContent.T3.get(),MultiblockContent.FORTUNE.get(),MultiblockContent.OVERLOAD.get(),MultiblockContent.SMELTING.get()};
        var positions = new java.util.ArrayList<BlockPos>();var entities = new java.util.ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
        for (int i=0;i<parts.length;i++) {
            var pos = min.offset(1+i%3,0,1+i/3);positions.add(pos);
            h.getLevel().setBlockAndUpdate(pos,parts[i].defaultBlockState());entities.add(h.getLevel().getBlockEntity(pos));
        }
        var player = player(h,host);supply(player,frameItem(),52);supply(player,glass().asItem(),36);
        var result = SimulationAutoConstruction.build(player,host,5);
        h.assertTrue(result.successful() && result.placed() == 88, "existing legal floor parts require no placement: "+result.reason());
        for (int i=0;i<parts.length;i++) {
            h.assertTrue(h.getLevel().getBlockState(positions.get(i)).is(parts[i]) && h.getLevel().getBlockEntity(positions.get(i)) == entities.get(i),
                    "existing floor blocks and their data survive automatic construction");
        }
        var structure = host.structure();
        h.assertTrue(structure != null && structure.t1() == 1 && structure.t2() == 1 && structure.t3() == 1
                && structure.fortune() == 1 && structure.overload() && structure.smelting(), "all preserved upgrades contribute their original effects");
        h.assertTrue(h.getLevel().getBlockState(min.offset(2,0,3)).isAir() && h.getLevel().getBlockState(min.offset(3,0,3)).isAir(), "unused floor slots stay empty");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void unrecognizedBottomInteriorBlockStillRejectsBeforeDebit(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var floor = min.offset(1,0,1);h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        var player = player(h,host);supply(player,frameItem(),20);supply(player,glass().asItem(),4);
        var result = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!result.successful() && count(player,frameItem()) == 20 && count(player,glass().asItem()) == 4,
                "invalid floor obstruction spends no resources");
        h.assertTrue(h.getLevel().getBlockState(floor).is(Blocks.STONE) && h.getLevel().getBlockState(min).isAir(), "floor obstruction is preserved without a partial shell");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void obstructionAndShortMaterialsKeepTheWholeInventory(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),20);supply(player,glass().asItem(),4);
        var obstacle = min.offset(1,1,1);h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        var blocked = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!blocked.successful() && count(player,frameItem()) == 20 && count(player,glass().asItem()) == 4, "interior obstruction spends nothing");
        h.assertTrue(h.getLevel().getBlockState(obstacle).is(Blocks.STONE) && h.getLevel().getBlockState(min).isAir(), "obstacle and all missing cells stay unchanged");
        h.getLevel().setBlockAndUpdate(obstacle,Blocks.AIR.defaultBlockState());
        player.getInventory().clearContent();supply(player,frameItem(),19);supply(player,glass().asItem(),4);
        var missing = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!missing.successful() && count(player,frameItem()) == 19 && count(player,glass().asItem()) == 4, "one missing frame aborts the entire build");
        h.assertTrue(h.getLevel().getBlockState(min).isAir() && host.structure() == null, "missing materials place no partial structure");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void duplicateSpecialModulesAndInterfacesRejectBeforeDebit(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));
        for (Block special : new Block[]{MultiblockContent.OVERLOAD.get(),MultiblockContent.SMELTING.get(),overloadedInterface()}) {
            clear(h,min,4);var host = controller(h,min,4,Direction.NORTH);var player = player(h,host);
            supply(player,frameItem(),64);supply(player,glass().asItem(),32);
            var a = special == overloadedInterface() ? min.offset(0,0,1) : min.offset(1,0,1);
            var b = special == overloadedInterface() ? min.offset(3,0,1) : min.offset(2,0,1);
            h.getLevel().setBlockAndUpdate(a,special.defaultBlockState());h.getLevel().setBlockAndUpdate(b,special.defaultBlockState());
            var result = SimulationAutoConstruction.build(player,host,4);
            h.assertTrue(!result.successful() && result.reason().equals("duplicates"), "duplicate special parts reject with a useful reason");
            h.assertTrue(count(player,frameItem()) == 64 && count(player,glass().asItem()) == 32 && h.getLevel().getBlockState(min).isAir(), "duplicates cannot debit or place");
            h.assertTrue(h.getLevel().getBlockState(a).is(special) && h.getLevel().getBlockState(b).is(special), "duplicate modules are preserved for manual correction");
            player.discard();
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignOwnershipAndControllerCannotBeClaimed(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var foreignPos = min.offset(4,0,0);h.getLevel().setBlockAndUpdate(foreignPos,MultiblockContent.CONTROLLER.get().defaultBlockState());
        var foreign = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(foreignPos);
        var reserved = min.offset(1,1,1);h.getLevel().setBlockAndUpdate(reserved,MultiblockContent.FRAME.get().defaultBlockState());
        ((SimulationMemberBlockEntity)h.getLevel().getBlockEntity(reserved)).bind(foreign,null);
        var player = player(h,host);supply(player,frameItem(),21);supply(player,glass().asItem(),4);
        var result = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!result.successful() && result.reason().equals("owned"), "foreign member ownership is rejected even inside reserved air");
        h.getLevel().setBlockAndUpdate(reserved,Blocks.AIR.defaultBlockState());
        var otherController = min.offset(0,0,1);h.getLevel().setBlockAndUpdate(otherController,MultiblockContent.CONTROLLER.get().defaultBlockState());
        result = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!result.successful() && count(player,frameItem()) == 21 && count(player,glass().asItem()) == 4, "another controller is never overwritten or charged");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedOwnershipInvalidatesAnAlreadyPreparedPlan(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var plan = SimulationAutoConstruction.plan(host,3);
        var neighborMin = min.offset(-2,0,1);
        for (int x=0;x<3;x++) for (int y=0;y<3;y++) for (int z=0;z<3;z++) {
            int boundaries = (x==0||x==2?1:0)+(y==0||y==2?1:0)+(z==0||z==2?1:0);
            var block = boundaries>=2||y==0||y==2 ? MultiblockContent.FRAME.get() : boundaries==1 ? glass() : Blocks.AIR;
            h.getLevel().setBlockAndUpdate(neighborMin.offset(x,y,z),block.defaultBlockState());
        }
        var foreign = controller(h,neighborMin,3,Direction.NORTH);foreign.checkStructure();
        h.assertTrue(foreign.structure() != null, "neighboring chamber really forms before claiming the prepared plan");
        try { h.assertTrue(!plan.available().getAsBoolean(), "ownership must be rechecked immediately before resource extraction and placement"); }
        finally { foreign.invalidateStructure(); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledFormationReportsTheUnformedChamber(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),21);supply(player,glass().asItem(),4);
        java.util.function.Consumer<dev.overloadsim.api.MultiblockSimulationEvents.BeforeStructureForm> cancel = event -> { if (event.controller == host) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result = SimulationAutoConstruction.build(player,host,3);
            h.assertTrue(!result.successful() && result.reason().equals("formation_cancelled") && result.placed() == 24, "completed placement reports when chamber formation is vetoed");
            h.assertTrue(host.structure() == null && SimulationStructureValidator.validate(host,min,3).valid(), "valid paid blocks remain available to form later");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void menuBuildUsesSelectedSizeAndRejectsClosedMenu(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,7,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),128);supply(player,glass().asItem(),128);
        var menu = new MultiblockSimulationMenu(1,player.getInventory(),host);player.containerMenu = menu;
        menu.receiveClientAction("simulationSize","7");player.containerMenu = player.inventoryMenu;
        menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure() == null && count(player,frameItem()) == 128, "closed menu cannot build");
        player.containerMenu = menu;menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure() != null && host.structure().size() == 7, "server uses selected external size");
        h.assertTrue(count(player,frameItem()) == 36 && count(player,glass().asItem()) == 28, "menu construction spends exact shell materials without filling upgrade slots");
        menu.receiveClientAction("simulationSize","3");menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure().size() == 7 && count(player,frameItem()) == 36, "formed menu clicks never resize the chamber");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleMenuIdentityAndDistanceRejectBuildAndSizeActions(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),64);supply(player,glass().asItem(),64);
        var menu = new MultiblockSimulationMenu(1,player.getInventory(),host);player.containerMenu = menu;
        player.setPos(host.getBlockPos().getCenter().add(9,0,0));menu.receiveClientAction("simulationSize","7");menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure() == null && count(player,frameItem()) == 64, "out-of-range menu actions spend nothing");
        player.setPos(host.getBlockPos().getCenter());menu.receiveClientAction("simulationSize","2");menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure() != null && host.structure().size() == 3, "invalid and out-of-range size packets keep the default size of three");
        host.invalidateStructure();var tag = host.saveMachine(h.getLevel().registryAccess());tag.putUUID("Identity",UUID.randomUUID());host.loadMachine(tag,h.getLevel().registryAccess());
        h.assertTrue(!menu.stillValid(player), "menu identity cannot survive controller replacement data");
        player.discard();h.succeed();
    }

    private static SimulationControllerBlockEntity controller(GameTestHelper h,BlockPos min,int size,Direction facing) {
        int middle = (size-1)/2;
        BlockPos pos = switch (facing) { case NORTH -> min.offset(middle,0,0);case SOUTH -> min.offset(middle,0,size-1);case WEST -> min.offset(0,0,middle);case EAST -> min.offset(size-1,0,middle);default -> throw new IllegalArgumentException(); };
        h.getLevel().setBlockAndUpdate(pos,MultiblockContent.CONTROLLER.get().defaultBlockState().setValue(SimulationControllerBlock.FACING,facing));
        return (SimulationControllerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void clear(GameTestHelper h,BlockPos min,int size) {
        for (int x=0;x<size;x++) for (int y=0;y<size;y++) for (int z=0;z<size;z++) h.getLevel().setBlockAndUpdate(min.offset(x,y,z),Blocks.AIR.defaultBlockState());
    }
    private static Block glass() { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:quartz_vibrant_glass")); }
    private static Block overloadedInterface() { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:overloaded_interface")); }
    private static Item frameItem() { return MultiblockContent.FRAME.get().asItem(); }
    private static ServerPlayer player(GameTestHelper h,SimulationControllerBlockEntity host) {
        var player = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"chamber-build"));player.setPos(host.getBlockPos().getCenter());return player;
    }
    private static void supply(ServerPlayer player,Item item,int count) {
        while (count>0) { int n=Math.min(64,count);player.getInventory().add(new ItemStack(item,n));count-=n; }
    }
    private static int count(ServerPlayer player,Item item) {
        int count=0;for (int i=0;i<player.getInventory().getContainerSize();i++) { var stack=player.getInventory().getItem(i);if (stack.is(item)) count+=stack.getCount(); }return count;
    }
}
