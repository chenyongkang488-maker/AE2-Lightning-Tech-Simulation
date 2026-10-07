package dev.overloadsim.gametest;

import appeng.menu.AEBaseMenu;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import dev.overloadsim.multiblock.*;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class FrameMenuAccessGameTests {
    @GameTest(template="laser_pedestal_empty")
    public static void allSevenLaserFrameClicksOpenTheirOwner(GameTestHelper h) {
        var controller = h.absolutePos(new BlockPos(12,10,12));
        var player = player(h);
        for (var kind : LaserKind.values()) {
            if (kind == LaserKind.CORE) continue;
            for (var facing : Direction.values()) {
                var host = laser(h,controller,kind,facing);
                var frame = host.structureTemplate().entrySet().stream()
                        .filter(e -> e.getValue() == LaserStructureGeometry.Part.FRAME)
                        .map(e -> controller.offset(e.getKey()))
                        .max(java.util.Comparator.comparingDouble(p -> p.distSqr(controller))).orElseThrow();
                click(h,player,frame);
                h.assertTrue(player.containerMenu instanceof LaserMenu,
                        kind+" frame click opens laser control menu in "+facing);
                h.assertTrue(((AEBaseMenu)player.containerMenu).getLocator().locate(player,LaserMachineBlockEntity.class) == host,
                        "frame menu locator resolves the owning controller");
                h.assertTrue(player.containerMenu.stillValid(player),"frame-opened menu remains valid near clicked frame");
                player.containerMenu = player.inventoryMenu;
                var cells = LaserAutoConstruction.plan(host).cells().keySet();
                host.invalidateStructure();
                for (var cell : cells) h.getLevel().setBlockAndUpdate(cell,Blocks.AIR.defaultBlockState());
            }
        }
        player.discard();h.succeed();
    }

    @GameTest(template="empty")
    public static void largestChamberFarCornerClickOpensAndStaysValid(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(2,1,2));
        var host = chamber(h,min,7);var player = player(h);var frame = min.offset(6,6,6);
        click(h,player,frame);
        h.assertTrue(player.distanceToSqr(host.getBlockPos().getCenter()) > 64,
                "far corner player is outside the old controller-only menu radius");
        h.assertTrue(player.containerMenu instanceof MultiblockSimulationMenu,"far chamber frame opens its control menu");
        var menu = (AEBaseMenu)player.containerMenu;
        h.assertTrue(menu.getLocator().locate(player,SimulationControllerBlockEntity.class) == host,"chamber locator targets owner");
        h.assertTrue(menu.stillValid(player),"far frame-opened chamber menu stays valid");
        player.setPos(frame.getCenter().add(9,0,0));
        h.assertTrue(!menu.stillValid(player),"walking away from clicked frame closes menu");
        player.setPos(frame.getCenter());
        h.getLevel().setBlockAndUpdate(min.offset(0,0,0),Blocks.AIR.defaultBlockState());
        h.assertTrue(!menu.stillValid(player),"shell dismantling invalidates active frame session immediately");
        player.discard();h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void laserFrameOpensWithOrdinaryItemButPreservesShiftAndWrench(GameTestHelper h) {
        var host = laser(h,h.absolutePos(new BlockPos(12,10,12)),LaserKind.CONSTRAINT,Direction.NORTH);
        var frame = firstFrame(host);var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
        click(h,player,frame);
        h.assertTrue(player.containerMenu instanceof LaserMenu,"holding a normal block still opens frame controls");
        player.containerMenu = player.inventoryMenu;player.setShiftKeyDown(true);
        h.getLevel().getBlockState(frame).useWithoutItem(h.getLevel(),player,hit(frame));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"shift leaves normal item interaction available");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND,appeng.core.definitions.AEItems.CERTUS_QUARTZ_WRENCH.stack());
        h.getLevel().getBlockState(frame).useWithoutItem(h.getLevel(),player,hit(frame));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"AE wrench is not intercepted by frame menu");
        player.discard();h.succeed();
    }

    @GameTest(template="empty")
    public static void chamberFrameOpensWithOrdinaryItemButPreservesShiftAndWrench(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(2,1,2));chamber(h,min,3);var frame = min;var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND));
        click(h,player,frame);
        h.assertTrue(player.containerMenu instanceof MultiblockSimulationMenu,"ordinary held item opens chamber frame controls");
        player.containerMenu = player.inventoryMenu;player.setShiftKeyDown(true);
        h.getLevel().getBlockState(frame).useWithoutItem(h.getLevel(),player,hit(frame));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"shift skips chamber frame menu");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND,appeng.core.definitions.AEItems.CERTUS_QUARTZ_WRENCH.stack());
        h.getLevel().getBlockState(frame).useWithoutItem(h.getLevel(),player,hit(frame));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"wrench skips chamber frame menu");
        player.discard();h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void laserFrameRejectsUnformedRemovedAndReplacedOwners(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(12,10,12));var player = player(h);
        var host = laser(h,pos,LaserKind.CONSTRAINT,Direction.NORTH);var frame = firstFrame(host);
        host.invalidateStructure();click(h,player,frame);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"unchanged frame of unformed laser has no menu");
        host.checkStructure();h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());click(h,player,frame);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"removed laser owner cannot open a stale frame");
        h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(LaserKind.CONSTRAINT).defaultBlockState());
        var replacement = (LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
        replacement.invalidateStructure();
        LaserStructureIndex.bind(host,new LaserStructure(pos,java.util.List.of(frame),null));
        click(h,player,frame);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"stale identity cannot route frame click to replacement controller");
        LaserStructureIndex.release(host);player.discard();h.succeed();
    }

    @GameTest(template="empty")
    public static void chamberFrameRejectsUnformedRemovedAndReplacedOwners(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(2,1,2));var host = chamber(h,min,3);var player = player(h);
        var oldStructure = host.structure();var controller = host.getBlockPos();
        host.invalidateStructure();click(h,player,min);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"unformed unchanged chamber frame has no menu");
        host.checkStructure();h.getLevel().setBlockAndUpdate(controller,Blocks.AIR.defaultBlockState());click(h,player,min);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"removed chamber controller cannot serve stale frames");
        h.getLevel().setBlockAndUpdate(controller,MultiblockContent.CONTROLLER.get().defaultBlockState());
        var replacement = (SimulationControllerBlockEntity)h.getLevel().getBlockEntity(controller);replacement.invalidateStructure();
        SimulationStructureIndex.bind(host,oldStructure);click(h,player,min);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"old chamber identity cannot open replacement controller");
        SimulationStructureIndex.release(host);player.discard();h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void frameClickRejectsDistanceAndFreshMissingShell(GameTestHelper h) {
        var host = laser(h,h.absolutePos(new BlockPos(12,10,12)),LaserKind.CONSTRAINT,Direction.NORTH);
        var frame = firstFrame(host);var player = player(h);
        player.setPos(frame.getCenter().add(20,0,0));
        h.getLevel().getBlockState(frame).useWithoutItem(h.getLevel(),player,hit(frame));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"far-away direct use cannot bypass reach validation");
        var missing = host.structureTemplate().entrySet().stream().filter(e -> e.getValue() == LaserStructureGeometry.Part.AIR)
                .map(e -> host.getBlockPos().offset(e.getKey())).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(missing,Blocks.STONE.defaultBlockState());
        click(h,player,frame);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"fresh aperture validation rejects invalid shell before tick");
        player.discard();h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void duplicateLiveLaserClaimsCannotBeResolvedByBindOrder(GameTestHelper h) {
        var host = laser(h,h.absolutePos(new BlockPos(12,10,12)),LaserKind.CONSTRAINT,Direction.NORTH);
        var other = laser(h,host.getBlockPos().offset(8,0,0),LaserKind.CORE,Direction.NORTH);
        var frame = firstFrame(host);var player = player(h);
        LaserStructureIndex.bind(other,new LaserStructure(other.center(),java.util.List.of(frame),null));
        LaserStructureIndex.bind(host,host.structure());
        click(h,player,frame);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"two live claims reject access even when actual owner binds last");
        LaserStructureIndex.bind(other,other.structure());
        click(h,player,frame);
        h.assertTrue(player.containerMenu instanceof LaserMenu,"removing duplicate claim restores unique-owner access");
        player.discard();h.succeed();
    }

    @GameTest(template="empty")
    public static void duplicateLiveChamberClaimsCannotBeResolvedByBindOrder(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(2,1,2));var host = chamber(h,min,3);var other = chamber(h,min.offset(8,0,0),3);
        var shape = other.structure();var player = player(h);
        SimulationStructureIndex.bind(other,new SimulationStructure(min,3,shape.members(),shape.glass(),shape.networkInterface(),0,0,0,0,false,false));
        SimulationStructureIndex.bind(host,host.structure());
        click(h,player,min);
        h.assertTrue(player.containerMenu == player.inventoryMenu,"duplicate live chamber claim does not select last writer");
        SimulationStructureIndex.bind(other,other.structure());
        click(h,player,min);
        h.assertTrue(player.containerMenu instanceof MultiblockSimulationMenu,"unique chamber claim restores menu access");
        player.discard();h.succeed();
    }

    @GameTest(template="empty")
    public static void foreignWorldAndUnloadedFrameUseCannotOpenOrLoadChunks(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(2,1,2));chamber(h,min,3);var player = player(h);
        var otherLevel = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(otherLevel != null,"test server provides another dimension");
        h.getLevel().getBlockState(min).useWithoutItem(otherLevel,player,hit(min));
        h.assertTrue(player.containerMenu == player.inventoryMenu,"foreign level use cannot resolve same-coordinate ownership");
        var remote = new BlockPos(1000000,100,1000000);player.setPos(remote.getCenter());
        h.assertTrue(!h.getLevel().hasChunkAt(remote),"remote chunk starts unloaded");
        MultiblockContent.FRAME.get().defaultBlockState().useWithoutItem(h.getLevel(),player,hit(remote));
        LaserContent.FRAME.get().defaultBlockState().useWithoutItem(h.getLevel(),player,hit(remote));
        h.assertTrue(!h.getLevel().hasChunkAt(remote) && player.containerMenu == player.inventoryMenu,
                "both frame entry points reject unloaded cells without forcing a load");
        player.discard();h.succeed();
    }

    private static LaserMachineBlockEntity laser(GameTestHelper h,BlockPos pos,LaserKind kind,Direction facing) {
        h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(kind).defaultBlockState().setValue(LaserMachineBlock.FACING,facing));
        var host = (LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
        LaserAutoConstruction.plan(host).cells().forEach((p,state) -> { if (!p.equals(pos)) h.getLevel().setBlockAndUpdate(p,state); });
        host.checkStructure();h.assertTrue(host.formed(),"laser fixture forms: "+kind+" "+facing+" "+host.status());return host;
    }
    private static BlockPos firstFrame(LaserMachineBlockEntity host) {
        return host.structureTemplate().entrySet().stream().filter(e -> e.getValue() == LaserStructureGeometry.Part.FRAME)
                .map(e -> host.getBlockPos().offset(e.getKey())).findFirst().orElseThrow();
    }
    private static SimulationControllerBlockEntity chamber(GameTestHelper h,BlockPos min,int size) {
        var pos = min.offset((size-1)/2,0,0);
        h.getLevel().setBlockAndUpdate(pos,MultiblockContent.CONTROLLER.get().defaultBlockState().setValue(SimulationControllerBlock.FACING,Direction.NORTH));
        var host = (SimulationControllerBlockEntity)h.getLevel().getBlockEntity(pos);
        SimulationAutoConstruction.plan(host,size).cells().forEach((p,state) -> { if (!p.equals(pos)) h.getLevel().setBlockAndUpdate(p,state); });
        host.checkStructure();h.assertTrue(host.structure() != null,"chamber fixture forms: "+host.error());return host;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(pos.getCenter(),Direction.UP,pos,false); }
    private static void click(GameTestHelper h,ServerPlayer player,BlockPos pos) {
        player.setPos(pos.getCenter());
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit(pos));
    }
    private static ServerPlayer player(GameTestHelper h) {
        // FakePlayer normally suppresses opening; use the actual MenuOpener provider and real menu factory.
        var player = new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"frame-menu")) {
            @Override public OptionalInt openMenu(MenuProvider provider,Consumer<RegistryFriendlyByteBuf> writer) {
                if (provider == null) return OptionalInt.empty();
                containerMenu = provider.createMenu(1,getInventory(),this);return OptionalInt.of(1);
            }
        };
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();return player;
    }
}
