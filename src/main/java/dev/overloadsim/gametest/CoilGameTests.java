package dev.overloadsim.gametest;

import com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity;
import com.moakiee.ae2lt.blockentity.workbench.DeviceWorkbenchAdapters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import dev.overloadsim.OverloadSimulation;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class CoilGameTests {
    @GameTest(template="empty")
    public static void moduleDependenciesPersistAndCapacityClamps(GameTestHelper h) {
        var coil=new ItemStack(dev.overloadsim.ModContent.COIL.get());
        var a=DeviceWorkbenchAdapters.get(coil).orElseThrow();var r=h.getLevel().registryAccess();
        var mimic=module(dev.overloadsim.tool.CoilModuleItem.Type.MIMIC);
        var efficiency=module(dev.overloadsim.tool.CoilModuleItem.Type.EFFICIENCY);
        h.assertTrue(!a.installOne(coil,r,efficiency),"reject efficiency without mimic");
        h.assertTrue(a.installOne(coil,r,mimic)&&a.installOne(coil,r,efficiency),"install prerequisites then extension");
        h.assertTrue(!a.installOne(coil,r,mimic),"duplicate modules rejected");
        h.assertTrue(a.uninstallOne(coil,r,"mimic_tool_module").isEmpty(),"cannot remove prerequisite under children");
        var energy=com.moakiee.ae2lt.registry.ModItems.ENERGY_MODULE_T3.toStack();
        h.assertTrue(a.installOne(coil,r,energy)&&a.energyBuffer().capacity(coil)==20_000_000_000L,"T3 capacity uses long");
        a.energyBuffer().receiveFe(coil,Integer.MAX_VALUE,false);
        var copy=ItemStack.parse(r,coil.save(r)).orElseThrow();
        h.assertTrue(a.listModuleEntries(copy,r).size()==3&&a.energyBuffer().stored(copy)==Integer.MAX_VALUE,"loadout and FE survive serialization");
        h.assertTrue(!a.uninstallOne(copy,r,"energy").isEmpty()&&a.energyBuffer().stored(copy)==10_000_000L,"module removal clamps FE");
        h.assertTrue(!a.uninstallOne(copy,r,"efficiency_module").isEmpty()&&!a.uninstallOne(copy,r,"mimic_tool_module").isEmpty(),"remove children then parent without losing items");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void configurationRejectsOutOfBoundsAbsentModulesAndReplacedStack(GameTestHelper h){
        var player=player(h);var coil=ready(h);player.getInventory().setItem(0,coil);
        var menu=new dev.overloadsim.tool.CoilMenu(1,player.getInventory(),0);
        h.assertTrue(!menu.configure(player,0,1),"EHV cannot enable before module installation");
        dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.EFFICIENCY));
        h.assertTrue(!menu.configure(player,2,11)&&!menu.configure(player,2,-1),"reject invalid efficiency levels");
        h.assertTrue(menu.configure(player,2,4)&&dev.overloadsim.tool.CoilSettings.read(coil).efficiency()==4,"valid level stored on item");
        h.assertTrue(menu.configure(player,1,0)&&coil.getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())==9,"disabled efficiency changes actual mining speed");
        player.getInventory().selected=1;h.assertTrue(!menu.stillValid(player)&&!menu.configure(player,2,8),"switching held slot invalidates configuration");player.getInventory().selected=0;
        player.getInventory().setItem(0,ready(h));h.assertTrue(!menu.stillValid(player)&&!menu.configure(player,2,8),"stale menu cannot configure a replacement item");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void releaseThresholdSpendsOnceAndNoTargetDoesNotPay(GameTestHelper h){
        var bench=network(h);var coil=equipped(h,bench);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);var pos=bench.getBlockPos();player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+3.5);player.setXRot(-90);
        for(int y=1;y<40;y++)h.getLevel().setBlockAndUpdate(player.blockPosition().above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.runAtTickTime(80,()->{
            var store=bench.getGrid().getStorageService().getInventory();var source=appeng.api.networking.security.IActionSource.empty();var key=com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE;
            coil.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            coil.getItem().releaseUsing(coil,h.getLevel(),player,72000-29);
            h.assertTrue(store.extract(key,100,appeng.api.config.Actionable.SIMULATE,source)==40&&bolts(h,player).isEmpty(),"29-tick air release does not self-strike or pay");
            coil.getItem().releaseUsing(coil,h.getLevel(),player,72000-30);
            h.assertTrue(store.extract(key,100,appeng.api.config.Actionable.SIMULATE,source)==30&&bolts(h,player).size()==1,"30-tick release strikes self once");
            coil.getItem().releaseUsing(coil,h.getLevel(),player,72000-31);
            h.assertTrue(store.extract(key,100,appeng.api.config.Actionable.SIMULATE,source)==30,"cooldown blocks duplicate release");bolts(h,player).forEach(net.minecraft.world.entity.Entity::discard);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void linkedToolChargesFeFromAeAndEmptyBufferCannotBreak(GameTestHelper h){
        var bench=network(h);var coil=equipped(h,bench);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);coil.set(dev.overloadsim.ModContent.COIL_FE.get(),0L);
        var pos=bench.getBlockPos().south(2);h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState());
        h.assertTrue(!player.gameMode.destroyBlock(pos)&&!h.getLevel().isEmptyBlock(pos),"empty FE buffer cancels normal mining");
        h.runAtTickTime(80,()->{bench.getDeviceInventory().setItemDirect(0,ItemStack.EMPTY);coil.set(dev.overloadsim.ModContent.COIL_FE.get(),0L);
            dev.overloadsim.tool.CoilEnergy.INSTANCE.refill(coil,player);h.assertTrue(CoilEnergyValue(coil)==10000,"linked tool charges at configured rate");
            h.assertTrue(dev.overloadsim.tool.CoilEnergy.INSTANCE.asEnergyStorage(coil).receiveEnergy(1000,false)==1000&&CoilEnergyValue(coil)==11000,"external item FE charging supported");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void closedWorkbenchContinuouslyChargesOnlyCoil(GameTestHelper h){
        var bench=network(h);var coil=equipped(h,bench);coil.set(dev.overloadsim.ModContent.COIL_FE.get(),0L);
        h.runAtTickTime(80,()->{h.assertTrue(CoilEnergyValue(coil)>0,"online workbench charges without an open menu");long before=CoilEnergyValue(coil);
            h.runAfterDelay(2,()->{h.assertTrue(CoilEnergyValue(coil)-before==20000,"workbench keeps charging each tick");h.succeed();});});
    }
    @GameTest(template="empty")
    public static void terrainCancellationRestoresBlockAndEnergy(GameTestHelper h){
        var coil=ready(h);var player=player(h);var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);player.setShiftKeyDown(true);
        int[] placements={0};java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getEntity()==player){placements[0]++;e.setCanceled(true);}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{for(var block:new net.minecraft.world.level.block.Block[]{net.minecraft.world.level.block.Blocks.DIRT,net.minecraft.world.level.block.Blocks.OAK_LOG,net.minecraft.world.level.block.Blocks.PUMPKIN}){
            level.setBlockAndUpdate(pos,block.defaultBlockState());level.setBlockAndUpdate(pos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
            player.gameMode.useItemOn(player,level,coil,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(level.getBlockState(pos).is(block)&&CoilEnergyValue(coil)==10000,"canceled terrain action preserves block and FE: "+block+"; state="+level.getBlockState(pos)+" FE="+CoilEnergyValue(coil)+" placements="+placements[0]);
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(),"canceled carving creates no seeds");
        }}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}h.succeed();
    }
    @GameTest(template="empty")
    public static void regeneratedOreStillPaysForSuccessfulRemoval(GameTestHelper h){
        var coil=ready(h);var player=player(h);var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));var state=net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);level.setBlockAndUpdate(pos,state);
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockDropsEvent> regenerate=e->{if(e.getLevel()==level&&e.getPos().equals(pos))level.setBlockAndUpdate(pos,state);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(regenerate);
        try{h.assertTrue(player.gameMode.destroyBlock(pos)&&level.getBlockState(pos).equals(state)&&CoilEnergyValue(coil)==9800,"successful harvest pays even if the same ore immediately regenerates");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(regenerate);}h.succeed();
    }
    @GameTest(template="empty")
    public static void miningCallbackDoesNotSpendBeforeActualRemoval(GameTestHelper h){
        var coil=ready(h);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);var pos=h.absolutePos(new BlockPos(5,1,5));var state=net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        coil.mineBlock(h.getLevel(),state,pos,player);h.assertTrue(CoilEnergyValue(coil)==10000,"vanilla callback alone is not a successful removal");
        h.assertTrue(player.gameMode.destroyBlock(pos)&&h.getLevel().isEmptyBlock(pos)&&CoilEnergyValue(coil)==9800,"actual successful removal commits one payment");h.succeed();
    }
    @GameTest(template="empty")
    public static void coreGatesUseAndMimicHarvestsAtNetheriteTier(GameTestHelper h) {
        var coil=new ItemStack(dev.overloadsim.ModContent.COIL.get());var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);
        h.assertTrue(coil.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"no core prevents use");
        dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.MIMIC));
        h.assertTrue(!coil.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState()),"mimic without core inactive");
        dev.overloadsim.tool.CoilModules.setCore(coil,com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack());
        coil.set(dev.overloadsim.ModContent.COIL_FE.get(),10000L);
        h.assertTrue(coil.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState()),"netherite mimic harvests diamond");
        h.assertTrue(coil.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.HOE_TILL)&&coil.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP),"all mining-tool abilities available");
        dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.EFFICIENCY));
        h.assertTrue(coil.getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())==110,"efficiency ten speed");
        dev.overloadsim.tool.CoilModules.setCore(coil,ItemStack.EMPTY);
        h.assertTrue(coil.getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())==1,"removing core removes speed");h.succeed();
    }
    @GameTest(template="empty")
    public static void terrainActionsRequireAndSpendEnergy(GameTestHelper h){
        var coil=ready(h);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);var pos=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.PUMPKIN.defaultBlockState());
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.NORTH,pos,false);player.setShiftKeyDown(true);
        coil.set(dev.overloadsim.ModContent.COIL_FE.get(),0L);h.assertTrue(!coil.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_CARVE),"empty buffer cannot bypass tool energy through native block action");
        player.gameMode.useItemOn(player,level,coil,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.PUMPKIN),"zero energy leaves pumpkin intact");
        coil.set(dev.overloadsim.ModContent.COIL_FE.get(),200L);var result=player.gameMode.useItemOn(player,level,coil,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result.consumesAction()&&level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN)&&CoilEnergyValue(coil)==0,"successful carving spends 200 FE");
        h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).stream().mapToInt(e->e.getItem().is(net.minecraft.world.item.Items.PUMPKIN_SEEDS)?e.getItem().getCount():0).sum()==4,"accepted carving drops exactly four seeds");h.succeed();
    }
    private static ItemStack module(dev.overloadsim.tool.CoilModuleItem.Type type){return dev.overloadsim.ModContent.COIL_UPGRADES.get(type).toStack();}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){return net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"coil-test"));}
    @GameTest(template="empty",timeoutTicks=130)
    public static void lightningDebitsOnlySelectedVoltageAndSetsNaturalMarker(GameTestHelper h){
        var bench=network(h);var coil=equipped(h,bench);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);player.setPos(bench.getBlockPos().getX()+.5,bench.getBlockPos().getY()+2,bench.getBlockPos().getZ()+3);
        h.runAtTickTime(80,()->{
            var store=bench.getGrid().getStorageService().getInventory();var source=appeng.api.networking.security.IActionSource.empty();var hv=com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE;var ehv=com.moakiee.ae2lt.me.key.LightningKey.EXTREME_HIGH_VOLTAGE;
            h.assertTrue(dev.overloadsim.tool.CoilLightning.fire(player,coil,true)==dev.overloadsim.tool.CoilLightning.Result.SUCCESS,"artificial self strike succeeds");
            h.assertTrue(store.extract(hv,100,appeng.api.config.Actionable.SIMULATE,source)==30&&store.extract(ehv,100,appeng.api.config.Actionable.SIMULATE,source)==40,"exactly 10 HV; EHV untouched");
            var first=bolts(h,player);h.assertTrue(first.size()==1&&!first.getFirst().getPersistentData().getBoolean("ae2lt.natural_weather_lightning"),"one artificial bolt");first.forEach(net.minecraft.world.entity.Entity::discard);
            player.getCooldowns().removeCooldown(coil.getItem());dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.EXTREME));
            coil.set(dev.overloadsim.ModContent.COIL_SETTINGS.get(),new dev.overloadsim.tool.CoilSettings(true,true,10,5,true));
            h.assertTrue(dev.overloadsim.tool.CoilLightning.fire(player,coil,true)==dev.overloadsim.tool.CoilLightning.Result.SUCCESS,"natural mode succeeds");
            h.assertTrue(store.extract(hv,100,appeng.api.config.Actionable.SIMULATE,source)==30&&store.extract(ehv,100,appeng.api.config.Actionable.SIMULATE,source)==30,"exactly 10 EHV; HV untouched");
            var second=bolts(h,player);h.assertTrue(second.size()==1&&second.getFirst().getPersistentData().getBoolean("ae2lt.natural_weather_lightning"),"natural classification before spawn");second.forEach(net.minecraft.world.entity.Entity::discard);
            player.getCooldowns().removeCooldown(coil.getItem());store.extract(ehv,30,appeng.api.config.Actionable.MODULATE,source);
            h.assertTrue(dev.overloadsim.tool.CoilLightning.fire(player,coil,true)==dev.overloadsim.tool.CoilLightning.Result.NO_LIGHTNING&&store.extract(hv,100,appeng.api.config.Actionable.SIMULATE,source)==30,"no voltage substitution or partial debit");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void canceledLightningSpawnRefundsItsPayment(GameTestHelper h){
        var bench=network(h);var coil=equipped(h,bench);var player=player(h);player.setPos(bench.getBlockPos().getX()+.5,bench.getBlockPos().getY()+2,bench.getBlockPos().getZ()+3);
        h.runAtTickTime(80,()->{java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=e->{if(e.getEntity() instanceof net.minecraft.world.entity.LightningBolt&&e.getEntity().getPersistentData().getBoolean("overload_sim.coil_lightning"))e.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try{h.assertTrue(dev.overloadsim.tool.CoilLightning.fire(player,coil,true)==dev.overloadsim.tool.CoilLightning.Result.SPAWN_FAILED,"cancel respected");
                h.assertTrue(bench.getGrid().getStorageService().getInventory().extract(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,100,appeng.api.config.Actionable.SIMULATE,appeng.api.networking.security.IActionSource.empty())==40,"failed spawn refunds all ten");
            }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}h.succeed();});
    }
    @GameTest(template="empty")
    public static void raycastStopsAtWallAndChargeReleaseSelectsSelf(GameTestHelper h){
        var player=player(h);var origin=h.absolutePos(new BlockPos(5,1,2));player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setYRot(0);player.setXRot(0);
        player.setXRot(-90);for(int y=1;y<=40;y++)h.getLevel().setBlockAndUpdate(origin.above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());h.assertTrue(dev.overloadsim.tool.CoilLightning.target(player,false).isEmpty(),"cleared sky has no target");player.setXRot(0);
        var wall=origin.offset(0,1,3);h.getLevel().setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());zombie.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+5.5);h.getLevel().addFreshEntity(zombie);
        h.assertTrue(dev.overloadsim.tool.CoilLightning.target(player,false).orElseThrow().equals(wall.above().getBottomCenter()),"wall blocks entity targeting");
        zombie.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+2.5);
        h.assertTrue(dev.overloadsim.tool.CoilLightning.target(player,false).orElseThrow().equals(zombie.position()),"nearest unobstructed mob targeted");
        h.assertTrue(dev.overloadsim.tool.CoilLightning.target(player,true).orElseThrow().equals(player.position()),"charged target is caster");zombie.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void silkDropsOreAndFortuneIsDisabledUntilSilkIsOff(GameTestHelper h){
        var coil=ready(h);dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.SILK));dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.FORTUNE));
        var lookup=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        h.assertTrue(coil.getEnchantmentLevel(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE))==0,"silk wins over fortune");
        var pos=h.absolutePos(new BlockPos(5,1,5));var state=net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState();
        var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,null,player(h),coil);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.DIAMOND_ORE),"real loot evaluation uses silk");
        coil.set(dev.overloadsim.ModContent.COIL_SETTINGS.get(),new dev.overloadsim.tool.CoilSettings(false,true,10,5,false));
        h.assertTrue(coil.getEnchantmentLevel(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE))==5&&coil.getAllEnchantments(lookup).getLevel(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH))==0,"fortune five effective after switching off silk");h.succeed();
    }
    @GameTest(template="empty")
    public static void ultimateBreaksBedrockButHonorsCanceledBreakAndCoreRemoval(GameTestHelper h){
        var coil=ready(h);var player=player(h);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,coil);var pos=h.absolutePos(new BlockPos(5,1,5));player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5);var level=h.getLevel();
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
        h.assertTrue(!dev.overloadsim.tool.CoilMining.breakUnbreakable(player,pos),"mimic alone cannot break bedrock");
        dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.ULTIMATE));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> cancel=e->{if(e.getPos().equals(pos))e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{h.assertTrue(!dev.overloadsim.tool.CoilMining.breakUnbreakable(player,pos)&&CoilEnergyValue(coil)==10000,"cancel does not break or spend");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(dev.overloadsim.tool.CoilMining.breakUnbreakable(player,pos)&&level.isEmptyBlock(pos)&&CoilEnergyValue(coil)==9800,"ultimate breaks bedrock for 200 FE");
        level.setBlockAndUpdate(pos,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:firmament_conversion_core")).defaultBlockState());
        h.assertTrue(dev.overloadsim.tool.CoilMining.breakUnbreakable(player,pos)&&level.isEmptyBlock(pos),"also breaks original firmament core");
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());dev.overloadsim.tool.CoilModules.setCore(coil,ItemStack.EMPTY);
        h.assertTrue(!dev.overloadsim.tool.CoilMining.breakUnbreakable(player,pos)&&!level.isEmptyBlock(pos),"removed core gates ultimate");h.succeed();
    }
    private static long CoilEnergyValue(ItemStack s){return dev.overloadsim.tool.CoilEnergy.read(s);}
    private static ItemStack ready(GameTestHelper h){var coil=new ItemStack(dev.overloadsim.ModContent.COIL.get());dev.overloadsim.tool.CoilModules.setCore(coil,com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack());dev.overloadsim.tool.CoilModules.install(coil,module(dev.overloadsim.tool.CoilModuleItem.Type.MIMIC));coil.set(dev.overloadsim.ModContent.COIL_FE.get(),10000L);return coil;}
    private static java.util.List<net.minecraft.world.entity.LightningBolt> bolts(GameTestHelper h,net.minecraft.server.level.ServerPlayer p){return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class,p.getBoundingBox().inflate(2),b->b.getPersistentData().getBoolean("overload_sim.coil_lightning"));}
    private static ItemStack equipped(GameTestHelper h,OverloadDeviceWorkbenchBlockEntity bench){var coil=ready(h);bench.getDeviceInventory().setItemDirect(0,coil);return coil;}
    private static OverloadDeviceWorkbenchBlockEntity network(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        level.setBlockAndUpdate(pos,com.moakiee.ae2lt.registry.ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get().defaultBlockState());
        level.setBlockAndUpdate(pos.west(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(pos.east(),appeng.core.definitions.AEBlocks.DRIVE.block().defaultBlockState());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)level.getBlockEntity(pos.east());
        var cell=com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.toStack();drive.getInternalInventory().setItemDirect(0,cell);
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);
        storage.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,40,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());
        storage.insert(com.moakiee.ae2lt.me.key.LightningKey.EXTREME_HIGH_VOLTAGE,40,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());storage.persist();
        return (OverloadDeviceWorkbenchBlockEntity)level.getBlockEntity(pos);
    }
    @GameTest(template="empty")
    public static void coilIsAcceptedByOriginalWorkbenchAndKeepsGunAdapter(GameTestHelper h) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("overload_sim:resonance_coil"));
        var stack=new ItemStack(item);
        h.assertTrue(DeviceWorkbenchAdapters.get(stack).isPresent(),"new coil must have its own workbench adapter");
        var pos=h.absolutePos(new BlockPos(5,1,5));
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:overload_device_workbench"));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var bench=(OverloadDeviceWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);
        bench.getDeviceInventory().setItemDirect(0,stack);
        h.assertTrue(bench.hasInstalledDevice()&&bench.currentAdapter()!=null,"real workbench accepts coil");
        h.assertTrue(!bench.getStructuralSlots().isEmpty(),"core slot available");
        var gun=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2lt:electromagnetic_railgun")));
        h.assertTrue(DeviceWorkbenchAdapters.get(gun).orElseThrow().getClass().getSimpleName().equals("RailgunWorkbenchAdapter"),"existing gun adapter preserved");
        h.succeed();
    }
}
