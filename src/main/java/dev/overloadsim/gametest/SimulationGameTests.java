package dev.overloadsim.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import dev.overloadsim.*;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.api.*;
import java.util.Optional;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class SimulationGameTests {
    @GameTest(template="empty")
    public static void crystalDisplayUpdatesWithoutOpeningMachineMenu(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);var empty=machine.getUpdateTag(level.registryAccess());
        machine.inventory().setStackInSlot(0,CrystalDataAccess.perfect(new CrystalData(ModContent.id("iron"),Optional.empty(),0,1)));var filled=machine.getUpdateTag(level.registryAccess());
        h.assertTrue(!java.util.Arrays.equals(empty.getByteArray("#upd"),filled.getByteArray("#upd")),"world render packet must change on crystal insertion without opening GUI");
        var mirror=new dev.overloadsim.machine.SimulationChamberBlockEntity(pos,machine.getBlockState());mirror.setLevel(level);mirror.handleUpdateTag(filled,level.registryAccess());
        try{h.assertTrue((boolean)mirror.getClass().getMethod("displayHasCrystal").invoke(mirror),"client receives crystal presence");
            machine.inventory().setStackInSlot(0,ItemStack.EMPTY);var removed=machine.getUpdateTag(level.registryAccess());mirror.handleUpdateTag(removed,level.registryAccess());
            h.assertTrue(!(boolean)mirror.getClass().getMethod("displayHasCrystal").invoke(mirror),"removing the crystal removes the client visual");
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}h.succeed();
    }
    @GameTest(template="empty")
    public static void chamberGlassKeepsTransparentFacesAndSolidProtection(GameTestHelper h){
        var state=ModContent.CHAMBER.get().defaultBlockState();var pos=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();level.setBlockAndUpdate(pos,state);
        h.assertTrue(!state.canOcclude(),"glass frame must not cull adjacent block faces");
        var shape=state.getShape(level,pos);var collision=state.getCollisionShape(level,pos);
        var window=net.minecraft.world.phys.shapes.Shapes.box(.4,.4,0,.6,.6,.12);
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,window,net.minecraft.world.phys.shapes.BooleanOp.AND)&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,window,net.minecraft.world.phys.shapes.BooleanOp.AND),"front glass panel has selection and collision");
        for(var glassPanel:java.util.List.of(
            net.minecraft.world.phys.shapes.Shapes.box(0,.4,.4,.12,.6,.6),net.minecraft.world.phys.shapes.Shapes.box(.88,.4,.4,1,.6,.6),
            net.minecraft.world.phys.shapes.Shapes.box(.4,.4,.88,.6,.6,1),net.minecraft.world.phys.shapes.Shapes.box(.4,.95,.4,.6,1,.6)))
            h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,glassPanel,net.minecraft.world.phys.shapes.BooleanOp.AND),"side and top glass protect the crystal");
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,net.minecraft.world.phys.shapes.Shapes.box(.1,0,.1,.9,.1,.9),net.minecraft.world.phys.shapes.BooleanOp.AND),"base remains solid");h.succeed();
    }
    @GameTest(template="empty")
    public static void heldSpeedCardsFillFourSeparateSlots(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"upgrade-test"),net.minecraft.server.level.ClientInformation.createDefault());
        var speed=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ae2:speed_card"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(speed,6));
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().getCount()==2,"normal held-card click inserts four cards and preserves excess");
        var upgrades=((appeng.api.upgrades.IUpgradeableObject)machine).getUpgrades();
        h.assertTrue(upgrades.size()==4&&upgrades.getInstalledUpgrades(speed)==4,"four native upgrade slots installed");
        for(int i=0;i<4;i++)h.assertTrue(upgrades.getStackInSlot(i).getCount()==1&&upgrades.getSlotLimit(i)==1,"one card per slot");
        level.getBlockState(pos).useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().getCount()==2,"full panel preserves held cards");
        var drops=new java.util.ArrayList<ItemStack>();machine.addAdditionalDrops(level,pos,drops);
        h.assertTrue(drops.stream().filter(s->s.is(speed)).mapToInt(ItemStack::getCount).sum()==4,"breaking returns all four cards");
        machine.clearContent();player.getAbilities().instabuild=true;
        level.getBlockState(pos).useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().getCount()==2&&upgrades.getInstalledUpgrades(speed)==2,"creative insertion preserves held stack");
        player.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void oldStackedCardsMigrateAndPersistWithoutMovingOutputs(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);
        var speed=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ae2:speed_card"));
        var old=new net.neoforged.neoforge.items.ItemStackHandler(13);var cards=new ItemStack(speed,4);
        cards.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("legacy cards"));
        old.setStackInSlot(2,cards);old.setStackInSlot(3,new ItemStack(net.minecraft.world.item.Items.WHEAT,2));old.setStackInSlot(4,new ItemStack(net.minecraft.world.item.Items.DIAMOND,5));
        var tag=machine.saveWithoutMetadata(level.registryAccess());tag.remove("Upgrades");tag.put("Inventory",old.serializeNBT(level.registryAccess()));machine.loadTag(tag,level.registryAccess());
        var upgrades=((appeng.api.upgrades.IUpgradeableObject)machine).getUpgrades();
        for(int i=0;i<4;i++)h.assertTrue(upgrades.getStackInSlot(i).getCount()==1&&ItemStack.isSameItemSameComponents(cards,upgrades.getStackInSlot(i)),"migration preserves each card and components");
        var saved=machine.saveWithoutMetadata(level.registryAccess());machine.loadTag(saved,level.registryAccess());
        h.assertTrue(upgrades.getInstalledUpgrades(speed)==4&&machine.inventory().getStackInSlot(2).isEmpty(),"reload does not duplicate migrated cards");
        h.assertTrue(machine.inventory().getStackInSlot(3).getCount()==2&&machine.inventory().getStackInSlot(4).getCount()==5,"auxiliary and output slots stay unchanged");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=280)
    public static void idleMachineContinuouslyRefillsAndStopsAtCapacity(GameTestHelper h){
        var machine=capacityMachine(h);machine.inventory().setStackInSlot(0,ItemStack.EMPTY);machine.energy().deserializeNBT(h.getLevel().registryAccess(),net.minecraft.nbt.IntTag.valueOf(0));
        h.runAtTickTime(80,()->{h.assertTrue(machine.getMainNode().isActive(),"online idle machine");int before=machine.energy().getEnergyStored();h.assertTrue(before>0,"idle machine charges without a template");
            h.runAfterDelay(3,()->{h.assertTrue(machine.energy().getEnergyStored()-before==3*10000,"charge continuously at 10000 FE per tick");});});
        h.runAtTickTime(240,()->{h.assertTrue(machine.energy().getEnergyStored()==2_000_000,"stop at FE capacity");
            h.getLevel().setBlockAndUpdate(machine.getBlockPos().west(),Blocks.AIR.defaultBlockState());machine.energy().deserializeNBT(h.getLevel().registryAccess(),net.minecraft.nbt.IntTag.valueOf(0));
            h.runAfterDelay(20,()->{h.assertTrue(machine.energy().getEnergyStored()==0&&!machine.getMainNode().isActive(),"unpowered grid cannot create FE");h.succeed();});});
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void runningBatchChargesAndKeepsItsSpeedSnapshot(GameTestHelper h){
        var machine=capacityMachine(h);machine.getUpgrades().clear();
        h.runAtTickTime(80,()->{h.assertTrue(machine.busy()&&machine.totalTicks()==200,"batch snapshots zero speed cards");assertPaidBatch(h,machine,4000,4);
            int before=machine.energy().getEnergyStored();machine.getUpgrades().addItems(appeng.core.definitions.AEItems.SPEED_CARD.stack(4));
            h.runAfterDelay(3,()->{h.assertTrue(machine.energy().getEnergyStored()-before==30000,"running batch also charges every tick");h.assertTrue(machine.totalTicks()==200,"newly inserted cards apply to the next batch");h.succeed();});});
    }
    @GameTest(template="empty")
    public static void menuShiftClickUsesFourUpgradeSlotsInsteadOfAuxiliarySlot(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"menu-test"),net.minecraft.server.level.ClientInformation.createDefault());
        player.getInventory().setItem(9,appeng.core.definitions.AEItems.SPEED_CARD.stack(4));var menu=new dev.overloadsim.machine.SimulationMenu(1,player.getInventory(),machine);
        h.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.UPGRADE).size()==4,"native upgrade panel receives four slots");
        int source=menu.getSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY).stream().filter(s->s.getContainerSlot()==9).findFirst().orElseThrow().index;
        menu.quickMoveStack(player,source);h.assertTrue(machine.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)==4&&machine.inventory().getStackInSlot(3).isEmpty(),"shift-click speed cards target upgrades; installed="+machine.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)+" remaining="+player.getInventory().getItem(9).getCount());
        menu.quickMoveStack(player,12);h.assertTrue(machine.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)==3,"shift-click can retrieve an idle upgrade");
        int retained=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(appeng.core.definitions.AEItems.SPEED_CARD.asItem()))retained+=player.getInventory().getItem(i).getCount();
        h.assertTrue(retained==1,"retrieval returns exactly one card to the player");player.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void automationRespectsConfiguredCardLimitAndRejectsUnsupportedCards(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,1,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);int previous=SimulationConfig.CARD_LIMIT.get();
        try{SimulationConfig.CARD_LIMIT.set(2);var remainder=appeng.core.definitions.AEItems.SPEED_CARD.stack(6);var io=machine.automation();
            h.assertTrue(io.getSlots()==17&&!io.isItemValid(2,remainder),"old slot reserved; four appended upgrade slots exposed");
            for(int i=13;i<17;i++)remainder=io.insertItem(i,remainder,false);
            h.assertTrue(remainder.getCount()==4&&machine.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)==2,"all insertion paths honor the pack limit");
            h.assertTrue(io.extractItem(13,1,false).isEmpty(),"automation cannot steal installed upgrades");
            var unsupported=appeng.core.definitions.AEItems.CAPACITY_CARD.stack();h.assertTrue(machine.getUpgrades().addItems(unsupported).getCount()==1,"unsupported upgrade is not consumed");
        }finally{SimulationConfig.CARD_LIMIT.set(previous);}h.succeed();
    }
    @GameTest(template="empty")
    public static void mineralBindingConsumesExactly24(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(center.offset(x,0,z),Blocks.RAW_IRON_BLOCK.defaultBlockState());
        var result=CrystalBinding.bindStructure(level,center,new ItemStack(ModContent.BLANK.get()),true);
        h.assertTrue(result.is(ModContent.BOUND.get()),"must bind iron");
        h.assertTrue(CrystalDataAccess.read(result).orElseThrow().profile().equals(ModContent.id("iron")),"correct profile");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.assertTrue(level.isEmptyBlock(center.offset(x,0,z)),"24 materials consumed");h.succeed();
    }
    @GameTest(template="empty")
    public static void oneWrongCellDoesNotConsume(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(center.offset(x,0,z),Blocks.RAW_IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(center.offset(2,0,2),Blocks.RAW_COPPER_BLOCK.defaultBlockState());
        var result=CrystalBinding.bindStructure(level,center,new ItemStack(ModContent.BLANK.get()),true);
        h.assertTrue(result.is(ModContent.BLANK.get()),"mixed materials fail");
        h.assertTrue(level.getBlockState(center.offset(-2,0,-2)).is(Blocks.RAW_IRON_BLOCK),"no partial consumption");h.succeed();
    }
    @GameTest(template="empty")
    public static void dataPersistsAndPerfectIdentityIsStable(GameTestHelper h){
        var d=new CrystalData(ModContent.id("iron"),Optional.empty(),9,1);var stack=CrystalDataAccess.bound(d);
        var loaded=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(CrystalDataAccess.read(loaded).orElseThrow().equals(d),"save retains component");
        h.assertTrue(CrystalDataAccess.read(CrystalDataAccess.perfect(d)).orElseThrow().strikes()==0,"perfect has canonical identity");h.succeed();
    }
    @GameTest(template="empty")
    public static void unfinishedTaskCannotDropPreRolledProducts(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);
        var data=new CrystalData(ModContent.id("iron"),Optional.empty(),0,1);
        machine.inventory().setStackInSlot(0,CrystalDataAccess.perfect(data));
        var saved=machine.saveWithoutMetadata(level.registryAccess());var job=new net.minecraft.nbt.CompoundTag();
        job.putInt("Remaining",100);job.putInt("Duration",200);job.putInt("Parallel",1);job.putInt("Cards",0);job.putString("Recipe","overload_sim:production/iron");job.putLong("PaidFe",1000);job.putLong("PaidLightning",10);
        job.put("Crystal",CrystalData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,data).getOrThrow());var outputs=new net.minecraft.nbt.ListTag();outputs.add(new ItemStack(net.minecraft.world.item.Items.DIAMOND).save(level.registryAccess()));job.put("Outputs",outputs);saved.put("Job",job);
        machine.loadTag(saved,level.registryAccess());var drops=new java.util.ArrayList<ItemStack>();machine.addAdditionalDrops(level,pos,drops);
        h.assertTrue(drops.stream().noneMatch(s->s.is(net.minecraft.world.item.Items.DIAMOND)),"breaking unfinished job must not bypass duration");
        h.assertTrue(drops.stream().anyMatch(s->s.is(ModContent.PERFECT.get())),"reusable template drops");h.succeed();
    }
    @GameTest(template="empty")
    public static void cropBindingRetainsFarmland(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){level.setBlockAndUpdate(center.offset(x,0,z),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(center.offset(x,1,z),Blocks.WHEAT.defaultBlockState());}
        var result=CrystalBinding.bindStructure(level,center,new ItemStack(ModContent.BLANK.get()),true);
        h.assertTrue(CrystalDataAccess.read(result).orElseThrow().profile().equals(ModContent.id("wheat")),"wheat profile");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){h.assertTrue(level.getBlockState(center.offset(x,0,z)).is(Blocks.FARMLAND),"retain soil");h.assertTrue(level.isEmptyBlock(center.offset(x,1,z)),"consume crop");}h.succeed();
    }
    @GameTest(template="empty")
    public static void cultivationAcceptsArtificialAndCompletesAtTen(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(BlockPos.ZERO);var initial=CrystalDataAccess.bound(new CrystalData(ModContent.id("iron"),Optional.empty(),0,1));
        var current=initial;for(int i=0;i<9;i++)current=CrystalBinding.cultivate(level,pos,current,false);
        h.assertTrue(current.is(ModContent.BOUND.get())&&CrystalDataAccess.read(current).orElseThrow().strikes()==9,"nine artificial strikes advance cultivation");current=CrystalBinding.cultivate(level,pos,current,false);
        h.assertTrue(current.is(ModContent.PERFECT.get()),"tenth strike perfects");h.succeed();
    }
    @GameTest(template="empty")
    public static void collectorAcceptsAddonAndOriginalCrystals(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:lightning_collector"));level.setBlockAndUpdate(pos,block.defaultBlockState());
        var collector=(com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity)level.getBlockEntity(pos);
        h.assertTrue(collector.getInventory().isItemValid(0,new ItemStack(ModContent.BLANK.get())),"addon crystal insertion");
        h.assertTrue(collector.getInventory().isItemValid(0,new ItemStack(com.moakiee.ae2lt.registry.ModItems.ELECTRO_CHIME_CRYSTAL.get())),"upstream crystal insertion");h.succeed();
    }
    @GameTest(template="empty")
    public static void playerRecordsNearestMobAndDeduplicatesBolt(GameTestHelper h){
        var level=h.getLevel();var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"simulation-test"),net.minecraft.server.level.ClientInformation.createDefault());var p=h.absolutePos(new BlockPos(5,2,5));player.setPos(p.getX(),p.getY(),p.getZ());
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(6,2,5));cow.setNoAi(true);var sheep=h.spawn(net.minecraft.world.entity.EntityType.SHEEP,new BlockPos(8,2,5));sheep.setNoAi(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(ModContent.BLANK.get()));
        var bolt=net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        level.random.setSeed(4096);dev.overloadsim.binding.PlayerLightningHandler.process(player,bolt);
        var result=CrystalDataAccess.read(player.getOffhandItem()).orElseThrow();h.assertTrue(result.entityType().orElseThrow().toString().equals("minecraft:cow"),"nearest cow must be recorded");
        dev.overloadsim.binding.PlayerLightningHandler.process(player,bolt);h.assertTrue(CrystalDataAccess.read(player.getOffhandItem()).orElseThrow().strikes()==0,"one bolt cannot also cultivate");
        var second=net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);dev.overloadsim.binding.PlayerLightningHandler.process(player,second);
        h.assertTrue(CrystalDataAccess.read(player.getOffhandItem()).orElseThrow().strikes()==1,"distinct artificial bolt cultivates once");player.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public static void poweredNetworkProducesFourParallelAndPaysActualCosts(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,1,5));var registry=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        level.setBlockAndUpdate(p,ModContent.CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(p.west(),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
        level.setBlockAndUpdate(p.east(),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:drive")).defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(p);machine.toggleEject();
        var drive=(appeng.blockentity.storage.DriveBlockEntity)level.getBlockEntity(p.east());var cell=new ItemStack(com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.get());
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);
        h.assertTrue(storage!=null,"lightning-capable cell handler");
        long inserted=storage.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,4,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());h.assertTrue(inserted==4,"seed exactly four HV for four operations");storage.persist();drive.getInternalInventory().setItemDirect(0,cell);
        machine.inventory().setStackInSlot(0,CrystalDataAccess.perfect(new CrystalData(ModContent.id("iron"),Optional.empty(),0,1)));
        machine.inventory().setStackInSlot(1,new ItemStack(com.moakiee.ae2lt.registry.ModItems.LIGHTNING_COLLAPSE_MATRIX.get()));
        installFourCards(machine);machine.energy().receiveEnergy(10000,false);
        for(int slot=4;slot<13;slot++)machine.inventory().setStackInSlot(slot,new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
        h.succeedWhen(()->{h.assertTrue(machine.inventory().getStackInSlot(4).getCount()==5,"four outputs merge into existing partial slots; status="+machine.status()+" node="+machine.getMainNode().isActive()+" fe="+machine.energy().getEnergyStored());assertPaidBatch(h,machine,4000,4);h.assertTrue(machine.inventory().getStackInSlot(0).is(ModContent.PERFECT.get()),"template retained");});
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void aeNetworkSuppliesFeWithoutCableOrLocalCharge(GameTestHelper h){
        var machine=capacityMachine(h);machine.energy().deserializeNBT(h.getLevel().registryAccess(),net.minecraft.nbt.IntTag.valueOf(0));
        // Only one output can fit, and only one batch can start.
        for(int slot=4;slot<13;slot++)machine.inventory().setStackInSlot(slot,new ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
        machine.inventory().setStackInSlot(4,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,63));
        h.succeedWhen(()->{h.assertTrue(machine.inventory().getStackInSlot(4).getCount()==64,"ME grid alone supplies simulation FE");h.assertTrue(machine.energy().getEnergyStored()>0,"keep refilling unused buffer");assertPaidBatch(h,machine,1000,1);});
    }
    @GameTest(template="empty")
    public static void boundTooltipReportsPercentageAndStage(GameTestHelper h){
        var crystal=CrystalDataAccess.bound(new CrystalData(ModContent.id("iron"),Optional.empty(),1,1));var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        crystal.getItem().appendHoverText(crystal,net.minecraft.world.item.Item.TooltipContext.of(h.getLevel()),lines,net.minecraft.world.item.TooltipFlag.NORMAL);
        h.assertTrue(lines.stream().anyMatch(c->c.getStyle().getColor()!=null&&c.getStyle().getColor().getValue()==net.minecraft.ChatFormatting.AQUA.getColor()&&c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&java.util.Arrays.asList(t.getArgs()).contains("10.0")),"one of ten strikes displays cyan 10.0 percent");
        h.assertTrue(lines.stream().anyMatch(c->c.getStyle().getColor()!=null&&c.getStyle().getColor().getValue()==net.minecraft.ChatFormatting.LIGHT_PURPLE.getColor()&&c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("item.ae2lt.electro_chime_crystal.stage")),"stage line follows upstream crystal styling");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void collectorBindsUsingArtificialLightning(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));var blocks=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        level.setBlockAndUpdate(p,blocks.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:lightning_collector")).defaultBlockState());
        level.setBlockAndUpdate(p.below(),blocks.get(net.minecraft.resources.ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
        level.setBlockAndUpdate(p.below(2),blocks.get(net.minecraft.resources.ResourceLocation.parse("ae2:drive")).defaultBlockState());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)level.getBlockEntity(p.below(2));drive.getInternalInventory().setItemDirect(0,new ItemStack(com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.get()));
        var collector=(com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity)level.getBlockEntity(p);collector.getInventory().setStackInSlot(0,new ItemStack(ModContent.BLANK.get()));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(p.offset(x,0,z),Blocks.RAW_IRON_BLOCK.defaultBlockState());
        h.runAtTickTime(80,()->{h.assertTrue(collector.captureLightning(false),"artificial bolt actually captured by original collector");h.assertTrue(collector.getInstalledCrystal().is(ModContent.BOUND.get()),"successful artificial capture binds simulation crystal");h.assertTrue(level.isEmptyBlock(p.offset(2,0,2)),"binding consumes raw blocks");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void extremeHighVoltageCannotPayHighVoltageRecipe(GameTestHelper h){
        var machine=capacityMachine(h);var drive=(appeng.blockentity.storage.DriveBlockEntity)h.getLevel().getBlockEntity(machine.getBlockPos().east());var cell=drive.getInternalInventory().getStackInSlot(0);var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);
        storage.extract(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,40,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());storage.insert(com.moakiee.ae2lt.me.key.LightningKey.EXTREME_HIGH_VOLTAGE,40,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());storage.persist();
        h.runAtTickTime(80,()->{h.assertTrue(machine.getMainNode().isActive(),"network online");h.assertTrue(!machine.busy()&&dev.overloadsim.compat.LightningNetwork.extract(machine,Long.MAX_VALUE,true)==0,"EHV cannot start or pay an HV batch");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void finiteAeNetworkDebitsStandardFeConversionOnce(GameTestHelper h){
        var machine=capacityMachine(h);var level=h.getLevel();machine.inventory().setStackInSlot(0,ItemStack.EMPTY);machine.energy().deserializeNBT(level.registryAccess(),net.minecraft.nbt.IntTag.valueOf(2_000_000));
        level.setBlockAndUpdate(machine.getBlockPos().west(),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:energy_cell")).defaultBlockState());
        var cell=(appeng.blockentity.networking.EnergyCellBlockEntity)level.getBlockEntity(machine.getBlockPos().west());cell.injectAEPower(50000,appeng.api.config.Actionable.MODULATE);
        h.runAtTickTime(80,()->{h.assertTrue(machine.getMainNode().isActive(),"finite powered network online");machine.energy().deserializeNBT(level.registryAccess(),net.minecraft.nbt.IntTag.valueOf(0));var grid=machine.getMainNode().getGrid();double before=grid.getEnergyService().getStoredPower();
            try{var charge=machine.getClass().getDeclaredMethod("chargeFromNetwork",int.class);charge.setAccessible(true);charge.invoke(machine,1000);charge.invoke(machine,1000);}catch(ReflectiveOperationException error){throw new RuntimeException(error);}
            h.assertTrue(machine.energy().getEnergyStored()==1000,"1000 FE retained in local buffer");h.assertTrue(Math.abs(before-grid.getEnergyService().getStoredPower()-500)<.0001,"1000 FE costs 500 AE once, under default AE2 2:1 conversion");h.succeed();});
    }
    @GameTest(template="empty")
    public static void oversizedRecipeOutputSavesAsLegalStacks(GameTestHelper h){
        var data=new CrystalData(ModContent.id("iron"),Optional.empty(),0,1);var output=new dev.overloadsim.data.SimulationRecipe.Output(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_iron"),128,1);
        var production=new dev.overloadsim.data.SimulationRecipe.Production(200,1000,10,java.util.List.of(output),Optional.empty(),false,Optional.empty());
        var products=dev.overloadsim.machine.MobLoot.roll(h.getLevel(),h.absolutePos(BlockPos.ZERO),data,production);
        h.assertTrue(products.stream().allMatch(s->s.getCount()<=s.getMaxStackSize()),"split oversized output before journaling");
        h.assertTrue(products.stream().mapToInt(ItemStack::getCount).sum()==128,"no items lost in splitting");
        for(var stack:products)h.assertTrue(ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow().getCount()==stack.getCount(),"outputs survive saving");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void incompatiblePartialOutputsDoNotChargeBatch(GameTestHelper h){
        var machine=capacityMachine(h);
        for(int slot=4;slot<13;slot++)machine.inventory().setStackInSlot(slot,new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        h.runAtTickTime(80,()->{h.assertTrue(machine.getMainNode().isActive(),"test network active");h.assertTrue(!machine.busy()&&machine.status()==4&&dev.overloadsim.compat.LightningNetwork.extract(machine,Long.MAX_VALUE,true)==40,"unrelated partial stacks must not admit a paid batch");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void outputCapacityReducesActualParallel(GameTestHelper h){
        var machine=capacityMachine(h);
        for(int slot=4;slot<13;slot++)machine.inventory().setStackInSlot(slot,new ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
        machine.inventory().setStackInSlot(4,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,63));
        h.succeedWhen(()->{h.assertTrue(machine.inventory().getStackInSlot(4).getCount()==64,"one output fits");h.assertTrue(machine.actualParallel()==1,"capacity limits four installed parallels to one operation");assertPaidBatch(h,machine,1000,1);});
    }
    static dev.overloadsim.machine.SimulationChamberBlockEntity capacityMachine(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,1,5));var registry=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        level.setBlockAndUpdate(p,ModContent.CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(p.west(),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
        level.setBlockAndUpdate(p.east(),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:drive")).defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(p);machine.toggleEject();
        var drive=(appeng.blockentity.storage.DriveBlockEntity)level.getBlockEntity(p.east());var cell=new ItemStack(com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.get());
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);
        storage.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,40,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());storage.persist();drive.getInternalInventory().setItemDirect(0,cell);
        machine.inventory().setStackInSlot(0,CrystalDataAccess.perfect(new CrystalData(ModContent.id("iron"),Optional.empty(),0,1)));
        machine.inventory().setStackInSlot(1,new ItemStack(com.moakiee.ae2lt.registry.ModItems.LIGHTNING_COLLAPSE_MATRIX.get()));
        installFourCards(machine);machine.energy().receiveEnergy(10000,false);return machine;
    }
    private static void installFourCards(dev.overloadsim.machine.SimulationChamberBlockEntity machine){for(int slot=0;slot<4;slot++)machine.getUpgrades().setItemDirect(slot,appeng.core.definitions.AEItems.SPEED_CARD.stack());}
    private static void assertPaidBatch(GameTestHelper h,dev.overloadsim.machine.SimulationChamberBlockEntity machine,long fe,long hv){
        try{var paidFe=machine.getClass().getDeclaredField("paidFe");var paidHv=machine.getClass().getDeclaredField("paidLightning");paidFe.setAccessible(true);paidHv.setAccessible(true);h.assertTrue(paidFe.getLong(machine)==fe&&paidHv.getLong(machine)==hv,"actual batch pays exact FE and HV independent of buffer refilling");}catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    @GameTest(template="empty")
    public static void multiOutputUsesPartialStacksBeforeEmptySlots(GameTestHelper h)throws ReflectiveOperationException{
        var pos=h.absolutePos(new BlockPos(5,1,5));h.getLevel().setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)h.getLevel().getBlockEntity(pos);
        for(int slot=6;slot<13;slot++)machine.inventory().setStackInSlot(slot,new ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
        machine.inventory().setStackInSlot(5,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,63));
        var outputs=java.util.List.of(new dev.overloadsim.data.SimulationRecipe.Output(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_iron"),1,1),new dev.overloadsim.data.SimulationRecipe.Output(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_copper"),1,1));
        var production=new dev.overloadsim.data.SimulationRecipe.Production(200,1000,10,outputs,Optional.empty(),false,Optional.empty());
        var capacity=machine.getClass().getDeclaredMethod("outputParallel",dev.overloadsim.data.SimulationRecipe.Production.class,int.class);capacity.setAccessible(true);
        h.assertTrue((int)capacity.invoke(machine,production,1)==1,"iron must merge into partial slot, leaving empty slot for copper");
        var pendingField=machine.getClass().getDeclaredField("pending");pendingField.setAccessible(true);
        @SuppressWarnings("unchecked") var pending=(java.util.List<ItemStack>)pendingField.get(machine);pending.add(new ItemStack(net.minecraft.world.item.Items.RAW_IRON));pending.add(new ItemStack(net.minecraft.world.item.Items.RAW_COPPER));
        var flush=machine.getClass().getDeclaredMethod("flushPending");flush.setAccessible(true);flush.invoke(machine);
        h.assertTrue(pending.isEmpty()&&machine.inventory().getStackInSlot(4).is(net.minecraft.world.item.Items.RAW_COPPER)&&machine.inventory().getStackInSlot(5).getCount()==64,"actual insertion uses same compatible-first order");h.succeed();
    }
    @GameTest(template="empty")
    public static void replacingCrystalBeforeCommitDoesNotConsumeBlocks(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(5,1,5));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(center.offset(x,0,z),Blocks.RAW_IRON_BLOCK.defaultBlockState());
        var original=new ItemStack(ModContent.BLANK.get());var replacement=original.copy();
        var result=CrystalBinding.bindStructure(level,center,original,true,()->replacement);
        h.assertTrue(result.is(ModContent.BLANK.get()),"changed slot identity aborts binding");
        h.assertTrue(level.getBlockState(center.offset(-2,0,-2)).is(Blocks.RAW_IRON_BLOCK),"keep materials when inventory replaced");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void autoEjectOnlySubtractsWhatChestAccepts(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,2,5));level.setBlockAndUpdate(pos,ModContent.CHAMBER.get().defaultBlockState());level.setBlockAndUpdate(pos.below(),Blocks.CHEST.defaultBlockState());
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)level.getBlockEntity(pos);var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos.below());
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,64));chest.setItem(0,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,62));
        machine.inventory().setStackInSlot(4,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,10));
        h.succeedWhen(()->{h.assertTrue(chest.getItem(0).getCount()==64,"chest accepts two items");h.assertTrue(machine.inventory().getStackInSlot(4).getCount()==8,"exactly eight items remain; no duplication or loss");});
    }
}
