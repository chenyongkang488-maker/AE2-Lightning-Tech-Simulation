package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.construction.AutomaticConstruction;
import dev.overloadsim.laser.*;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class AutomaticConstructionGameTests {
    private static LaserMachineBlockEntity host(GameTestHelper h) {
        return LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,4,8)),LaserKind.CORE,net.minecraft.core.Direction.NORTH);
    }
    private static LaserMachineBlockEntity networkHost(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(8,4,8));
        h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(LaserKind.EMITTER).defaultBlockState());
        return (LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static ServerPlayer player(GameTestHelper h,LaserMachineBlockEntity host) {
        var p=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"construction-test"));
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setPos(host.getBlockPos().getCenter());return p;
    }
    private static AutomaticConstruction.Plan plan(LaserMachineBlockEntity host,int count) {
        var cells=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        cells.put(host.getBlockPos(),host.getBlockState());
        for(int i=1;i<=count;i++)cells.put(host.getBlockPos().north(i),LaserContent.FRAME.get().defaultBlockState());
        return new AutomaticConstruction.Plan(cells,()->true);
    }
    @GameTest(template="laser_empty") public static void buildsOnlyMissingPartsAndRepeatedClicksAreFree(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),4));
        h.getLevel().setBlockAndUpdate(host.getBlockPos().north(),LaserContent.FRAME.get().defaultBlockState());
        var result=AutomaticConstruction.build(p,host,plan(host,3),null);
        h.assertTrue(result.successful()&&result.placed()==2&&p.getInventory().getItem(0).getCount()==2,"only two absent frames are placed and paid");
        var again=AutomaticConstruction.build(p,host,plan(host,3),null);
        h.assertTrue(again.successful()&&again.placed()==0&&p.getInventory().getItem(0).getCount()==2,"repeat build is an exact no-op");h.succeed();
    }
    @GameTest(template="laser_empty") public static void materialShortageDoesNotStartOrDebit(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),1));
        var result=AutomaticConstruction.build(p,host,plan(host,2),null);
        h.assertTrue(!result.successful()&&result.reason().equals("materials")&&result.missing().getFirst().getCount()==1,"exact missing count is reported");
        h.assertTrue(p.getInventory().getItem(0).getCount()==1&&h.getLevel().getBlockState(host.getBlockPos().north()).isAir(),"shortage does not consume or partially build");h.succeed();
    }
    @GameTest(template="laser_empty") public static void obstacleAndFluidAreNeverReplaced(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),2));
        var target=host.getBlockPos().north(2);h.getLevel().setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        h.assertTrue(!AutomaticConstruction.build(p,host,plan(host,2),null).successful()&&h.getLevel().getBlockState(target).is(Blocks.STONE),"solid obstruction retained");
        h.getLevel().setBlockAndUpdate(target,Blocks.WATER.defaultBlockState());
        h.assertTrue(!AutomaticConstruction.build(p,host,plan(host,2),null).successful()&&h.getLevel().getBlockState(target).is(Blocks.WATER)&&p.getInventory().getItem(0).getCount()==2,"fluid retained without debit");h.succeed();
    }
    @GameTest(template="laser_empty") public static void canceledSecondPlacementRestoresAllPartsAndNamedItems(GameTestHelper h) {
        var host=host(h);var p=player(h,host);var named=new ItemStack(LaserContent.FRAME.get(),2);named.set(DataComponents.CUSTOM_NAME,Component.literal("reserve"));p.getInventory().setItem(0,named.copy());
        int[] calls={0};java.util.function.Consumer<BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getEntity()==p&&++calls[0]==2)e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result=AutomaticConstruction.build(p,host,plan(host,2),null);
            h.assertTrue(!result.successful()&&calls[0]==2,"normal placement protection events are honored");
            h.assertTrue(h.getLevel().getBlockState(host.getBlockPos().north()).isAir()&&h.getLevel().getBlockState(host.getBlockPos().north(2)).isAir(),"all earlier placements restored");
            h.assertTrue(ItemStack.matches(p.getInventory().getItem(0),named),"exact renamed material and count refunded");
        } finally {NeoForge.EVENT_BUS.unregister(cancel);}h.succeed();
    }
    @GameTest(template="laser_empty") public static void reservedAirAndForeignOwnershipArePrechecked(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),2));
        var cells=new LinkedHashMap<>(plan(host,2).cells());cells.put(host.getBlockPos().south(2),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(host.getBlockPos().south(2),Blocks.DIRT.defaultBlockState());
        h.assertTrue(!AutomaticConstruction.build(p,host,new AutomaticConstruction.Plan(cells,()->true),null).successful(),"reserved interior cannot contain dirt");
        h.assertTrue(!AutomaticConstruction.build(p,host,new AutomaticConstruction.Plan(plan(host,2).cells(),()->false),null).successful()&&p.getInventory().getItem(0).getCount()==2,"foreign ownership rejected before debit");h.succeed();
    }
    @GameTest(template="laser_empty") public static void staleHostAndRemoteOrSpectatorPlayerCannotBuild(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),2));
        p.setPos(host.getBlockPos().getCenter().add(20,0,0));h.assertTrue(!AutomaticConstruction.build(p,host,plan(host,2),null).successful(),"remote request denied");
        p.setPos(host.getBlockPos().getCenter());p.setGameMode(GameType.SPECTATOR);h.assertTrue(!AutomaticConstruction.build(p,host,plan(host,2),null).successful(),"spectator denied");
        p.setGameMode(GameType.SURVIVAL);h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());h.assertTrue(!AutomaticConstruction.build(p,host,plan(host,2),null).successful()&&p.getInventory().getItem(0).getCount()==2,"removed host denied");h.succeed();
    }
    @GameTest(template="laser_empty") public static void creativeBuildDoesNotWithdrawMaterials(GameTestHelper h) {
        var host=host(h);var p=player(h,host);p.setGameMode(GameType.CREATIVE);
        h.assertTrue(AutomaticConstruction.build(p,host,plan(host,2),null).successful()&&p.getInventory().isEmpty(),"creative build needs no materials");h.succeed();
    }
    private static void connect(GameTestHelper h,LaserMachineBlockEntity host) {
        h.getLevel().setBlockAndUpdate(host.getBlockPos().west(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }
    private static final class Storage implements MEStorage {
        long count;boolean shortDraw,rejectRefund;final AEItemKey key=AEItemKey.of(LaserContent.FRAME.get());
        Storage(long count){this.count=count;}
        public Component getDescription(){return Component.literal("construction fixture");}
        public void getAvailableStacks(KeyCounter out){out.add(key,count);}
        public long extract(AEKey what,long amount,Actionable action,IActionSource source){if(!key.equals(what))return 0;long draw=Math.min(count,action==Actionable.MODULATE&&shortDraw?1:amount);if(action==Actionable.MODULATE)count-=draw;return draw;}
        public long insert(AEKey what,long amount,Actionable action,IActionSource source){if(!key.equals(what)||rejectRefund)return 0;if(action==Actionable.MODULATE)count+=amount;return amount;}
    }
    @GameTest(template="laser_empty",timeoutTicks=120) public static void activeAeSuppliesOnlyBackpackShortfall(GameTestHelper h) {
        var host=networkHost(h);var p=player(h,host);connect(h,host);p.getInventory().setItem(0,new ItemStack(LaserContent.FRAME.get(),1));
        h.runAtTickTime(80,()->{
            var grid=host.getMainNode().getGrid();h.assertTrue(grid!=null&&host.getMainNode().isActive(),"fixture AE network active");var storage=new Storage(9);grid.getStorageService().addGlobalStorageProvider(mounts->mounts.mount(storage,0));
            var result=AutomaticConstruction.build(p,host,plan(host,3),grid);
            h.assertTrue(result.successful()&&storage.count==7&&p.getInventory().getItem(0).isEmpty(),"backpack one and AE two, not AE three");h.succeed();
        });
    }
    @GameTest(template="laser_empty",timeoutTicks=120) public static void aeShortExtractionRefundsAndDoesNotPlace(GameTestHelper h) {
        var host=networkHost(h);var p=player(h,host);connect(h,host);
        h.runAtTickTime(80,()->{
            var grid=host.getMainNode().getGrid();var storage=new Storage(3);storage.shortDraw=true;grid.getStorageService().addGlobalStorageProvider(mounts->mounts.mount(storage,0));
            var result=AutomaticConstruction.build(p,host,plan(host,3),grid);
            h.assertTrue(!result.successful()&&storage.count==3&&h.getLevel().getBlockState(host.getBlockPos().north()).isAir(),"short modulated draw fully refunded with no partial build");h.succeed();
        });
    }
    @GameTest(template="laser_empty",timeoutTicks=120) public static void rejectedAeRefundWithFullInventoryDropsExactRemainder(GameTestHelper h) {
        var host=networkHost(h);var p=player(h,host);connect(h,host);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
        h.runAtTickTime(80,()->{
            var grid=host.getMainNode().getGrid();var storage=new Storage(3);storage.shortDraw=true;storage.rejectRefund=true;grid.getStorageService().addGlobalStorageProvider(mounts->mounts.mount(storage,0));
            var result=AutomaticConstruction.build(p,host,plan(host,3),grid);
            int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3)).stream().mapToInt(e->e.getItem().is(LaserContent.FRAME.get().asItem())?e.getItem().getCount():0).sum();
            h.assertTrue(!result.successful()&&storage.count+drops==3&&drops==1,"refund conserves the item even when AE and backpack reject it");h.succeed();
        });
    }
}
