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
        // Hand-counted: external size, frame items (controller excluded), glass, empty interior.
        int[][] cases = {{3,21,4,1},{4,39,16,8},{5,61,36,27},{6,87,64,64},{7,117,100,125}};
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));
        for (int[] test : cases) for (Direction facing : Direction.Plane.HORIZONTAL) {
            clear(h,min,7);
            var host = controller(h,min,test[0],facing);
            var plan = SimulationAutoConstruction.plan(host,test[0]);
            h.assertTrue(plan.cells().size() == test[0]*test[0]*test[0], "plan reserves entire chamber including its interior");
            h.assertTrue(plan.cells().values().stream().filter(s -> s.is(MultiblockContent.FRAME.get())).count() == test[1], "correct frame count for size "+test[0]);
            h.assertTrue(plan.cells().values().stream().filter(s -> s.is(glass())).count() == test[2], "correct glass count for size "+test[0]);
            h.assertTrue(plan.cells().values().stream().filter(s -> s.isAir()).count() == test[3], "correct empty interior for size "+test[0]);
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
        h.assertTrue(result.successful() && result.placed() == 94, "only missing 58 frames and 36 glass are placed: "+result.reason());
        h.assertTrue(count(player,frameItem()) == 6 && count(player,glass().asItem()) == 4, "existing controller, module, interface and frame are deducted from material requirements");
        h.assertTrue(h.getLevel().getBlockEntity(module) == moduleEntity && h.getLevel().getBlockEntity(port) == portEntity, "existing upgrades and interface retain their entities");
        h.assertTrue(host.structure() != null && host.structure().t3() == 1 && host.structure().networkInterface().equals(port), "preserved optional parts participate in the formed chamber");
        var again = SimulationAutoConstruction.build(player,host,7);
        h.assertTrue(again.successful() && again.placed() == 0 && host.structure().size() == 5, "formed chamber is a no-op even if a different size is requested");
        h.assertTrue(count(player,frameItem()) == 6 && count(player,glass().asItem()) == 4, "repeat build consumes nothing");
        player.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void obstructionAndShortMaterialsKeepTheWholeInventory(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2,1,2));var host = controller(h,min,3,Direction.NORTH);
        var player = player(h,host);supply(player,frameItem(),21);supply(player,glass().asItem(),4);
        var obstacle = min.offset(1,1,1);h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        var blocked = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!blocked.successful() && count(player,frameItem()) == 21 && count(player,glass().asItem()) == 4, "interior obstruction spends nothing");
        h.assertTrue(h.getLevel().getBlockState(obstacle).is(Blocks.STONE) && h.getLevel().getBlockState(min).isAir(), "obstacle and all missing cells stay unchanged");
        h.getLevel().setBlockAndUpdate(obstacle,Blocks.AIR.defaultBlockState());
        player.getInventory().clearContent();supply(player,frameItem(),20);supply(player,glass().asItem(),4);
        var missing = SimulationAutoConstruction.build(player,host,3);
        h.assertTrue(!missing.successful() && count(player,frameItem()) == 20 && count(player,glass().asItem()) == 4, "one missing frame aborts the entire build");
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
            h.assertTrue(!result.successful() && result.reason().equals("formation_cancelled") && result.placed() == 25, "completed placement reports when chamber formation is vetoed");
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
        h.assertTrue(count(player,frameItem()) == 11 && count(player,glass().asItem()) == 28, "menu construction spends exact materials");
        menu.receiveClientAction("simulationSize","3");menu.receiveClientAction("simulationBuild","null");
        h.assertTrue(host.structure().size() == 7 && count(player,frameItem()) == 11, "formed menu clicks never resize the chamber");
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
