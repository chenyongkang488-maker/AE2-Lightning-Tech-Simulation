package dev.overloadsim.gametest;

import com.moakiee.ae2lt.menu.hub.*;
import dev.overloadsim.*;
import dev.overloadsim.tool.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class CoilHubGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){return net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"coil-hub-test"));}
    private static ItemStack coil(){var s=ModContent.COIL.toStack();CoilModules.setCore(s,com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack());s.set(ModContent.COIL_FE.get(),10000L);return s;}
    @GameTest(template="empty")
    public static void sharedHubUsesHeldWeaponSlotAndPreservesGun(GameTestHelper h){
        var p=player(h);var coil=coil();p.setItemInHand(InteractionHand.MAIN_HAND,coil);
        var menu=new DeviceHubMenu(1,p.getInventory(),DeviceHubMenu.TAB_RAILGUN);menu.setPlayer(p);menu.broadcastChanges();
        h.assertTrue((menu.getTabAvailability()&(1<<DeviceHubMenu.TAB_RAILGUN))!=0,"coil occupies the native weapon tab");
        var status=DeviceStatusModel.fromRailgunStack(coil,p,-1);h.assertTrue(status.displayName().equals(coil.getHoverName().getString())&&status.hasCore(),"coil status in original hub");
        var gun=com.moakiee.ae2lt.registry.ModItems.ELECTROMAGNETIC_RAILGUN.toStack();p.setItemInHand(InteractionHand.MAIN_HAND,gun);p.setItemInHand(InteractionHand.OFF_HAND,coil);menu.broadcastChanges();
        h.assertTrue(dev.overloadsim.compat.CoilHubAccess.weapon(p)==gun,"main-hand gun wins over off-hand coil");
        p.setItemInHand(InteractionHand.MAIN_HAND,coil);p.setItemInHand(InteractionHand.OFF_HAND,gun);
        h.assertTrue(dev.overloadsim.compat.CoilHubAccess.weapon(p)==coil,"main-hand coil wins over off-hand gun");
        h.assertTrue(DeviceStatusModel.fromRailgunStack(gun,p,-1).displayName().equals(gun.getHoverName().getString()),"native gun status unchanged");h.succeed();
    }
    @GameTest(template="empty")
    public static void wrenchModuleGatesNativeWrenchAndStopsCharging(GameTestHelper h){
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("overload_sim:wrench_module"));h.assertTrue(item!=Items.AIR,"wrench module is registered");
        var s=coil();h.assertTrue(CoilModules.install(s,new ItemStack(item)),"wrench installs without mimic prerequisite");var p=player(h);p.getInventory().setItem(0,s);
        var menu=new CoilMenu(1,p.getInventory(),0);h.assertTrue(menu.configure(p,5,1),"G can enable wrench mode");
        h.assertTrue(appeng.util.InteractionUtil.canWrenchRotate(s)&&appeng.util.InteractionUtil.canWrenchDisassemble(s),"enabled module exposes native AE wrench behavior");
        h.assertTrue(s.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()!=net.minecraft.world.InteractionResult.CONSUME&&!p.isUsingItem(),"wrench air use cannot start a lightning charge");
        CoilModules.setCore(s,ItemStack.EMPTY);h.assertTrue(!appeng.util.InteractionUtil.canWrenchRotate(s),"removing core disables wrench");h.succeed();
    }
    private static ItemStack wrench(){var s=coil();CoilModules.install(s,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("overload_sim:wrench_module"))));CoilConfiguration.apply(s,5,1);return s;}
    @GameTest(template="empty")
    public static void nativeHubRejectsStalePacketsAndPreservesOldSettings(GameTestHelper h){
        var p=player(h);var s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);var menu=new DeviceHubMenu(3,p.getInventory(),4);menu.setPlayer(p);p.containerMenu=menu;
        var config=(dev.overloadsim.compat.CoilHubAccess.Configuration)menu;
        h.assertTrue(config.overloadSim$configure(p,6,7)&&CoilSettings.read(s).wrenchMode()==7,"native hub changes exact bound coil");
        h.assertTrue(!config.overloadSim$configure(p,6,8)&&!config.overloadSim$configure(p,0,1),"invalid mode and absent module rejected");
        var copy=ItemStack.parse(h.getLevel().registryAccess(),s.save(h.getLevel().registryAccess())).orElseThrow();h.assertTrue(CoilSettings.read(copy).wrenchMode()==7&&CoilWrench.active(copy),"wrench settings survive item save");
        menu.toggleRailgunPvp();menu.toggleRailgunSound();h.assertTrue(CoilSettings.read(s).wrenchMode()==7,"gun commands cannot alter coil configuration");
        p.setItemInHand(InteractionHand.MAIN_HAND,copy);h.assertTrue(!config.overloadSim$configure(p,5,0)&&CoilWrench.active(copy),"replacement coil rejects late packet");
        var old=CoilSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"extreme\":true,\"efficiency\":4,\"fortune\":3,\"silk\":false}")).getOrThrow();
        h.assertTrue(old.extreme()&&old.efficiency()==4&&!old.wrench()&&old.wrenchMode()==0,"alpha6 settings keep values and default wrench off");h.succeed();
    }
    @GameTest(template="empty")
    public static void wrenchSwitchAndRemovalSuppressAllLightningPaths(GameTestHelper h){
        var p=player(h);var s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);long before=CoilEnergy.read(s);
        h.assertTrue(CoilLightning.fire(p,s,true)==CoilLightning.Result.DISABLED,"even direct server lightning entry rejects wrench mode");
        p.startUsingItem(InteractionHand.MAIN_HAND);s.releaseUsing(h.getLevel(),p,71960);
        h.assertTrue(CoilEnergy.read(s)==before&&h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class,p.getBoundingBox().inflate(4)).isEmpty(),"late charged release creates no lightning or energy cost");p.stopUsingItem();
        CoilConfiguration.apply(s,5,0);h.assertTrue(!appeng.util.InteractionUtil.canWrenchRotate(s),"off toggle removes AE wrench ability");
        CoilConfiguration.apply(s,5,1);CoilModules.uninstall(s,"wrench_module");h.assertTrue(!CoilWrench.active(s)&&!appeng.util.InteractionUtil.canWrenchDisassemble(s),"uninstall removes ability immediately");h.succeed();
    }
    @GameTest(template="empty")
    public static void aeWrenchRotatesAndDisassemblesDriveWithContents(GameTestHelper h){
        var p=player(h);var s=wrench();p.setItemInHand(InteractionHand.MAIN_HAND,s);var pos=h.absolutePos(new net.minecraft.core.BlockPos(3,1,3));var level=h.getLevel();
        level.setBlockAndUpdate(pos,appeng.core.definitions.AEBlocks.DRIVE.block().defaultBlockState());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)level.getBlockEntity(pos);var initial=drive.getFront();var side=initial==net.minecraft.core.Direction.EAST?net.minecraft.core.Direction.WEST:net.minecraft.core.Direction.EAST;
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),side,pos,false);appeng.hooks.WrenchHook.onPlayerUseBlock(p,level,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(drive.getFront()!=initial,"native AE hook rotates real drive");
        var cell=com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.toStack();drive.getInternalInventory().setItemDirect(0,cell);p.setShiftKeyDown(true);
        appeng.hooks.WrenchHook.onPlayerUseBlock(p,level,InteractionHand.MAIN_HAND,hit);h.assertTrue(level.isEmptyBlock(pos),"native AE sneak wrench dismantles drive");
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        h.assertTrue(p.getInventory().items.stream().anyMatch(item->item.getItem()==cell.getItem())||drops.stream().anyMatch(e->e.getItem().getItem()==cell.getItem()),"stored cell is returned by native disassembly");h.succeed();
    }
    @GameTest(template="empty")
    public static void optionalMekanismConfiguratorUsesNativeModesAndSecurity(GameTestHelper h){
        if(CoilWrench.mekanism())MekanismCoilChecks.run(h,player(h),wrench());
        else h.succeed();
    }
}
