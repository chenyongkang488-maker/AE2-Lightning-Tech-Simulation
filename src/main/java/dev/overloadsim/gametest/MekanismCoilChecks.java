package dev.overloadsim.gametest;

import dev.overloadsim.tool.*;
import mekanism.api.RelativeSide;
import mekanism.api.security.SecurityMode;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

/** Optional integration checks are loaded only in the -PmekTests run. */
final class MekanismCoilChecks {
    private static UseOnContext context(ServerPlayer p,BlockPos pos){return new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.EAST,pos,false));}
    static void run(GameTestHelper h,ServerPlayer p,ItemStack coil){
        p.server.getProfileCache().add(p.getGameProfile());
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,1,3));p.setItemInHand(InteractionHand.MAIN_HAND,coil);
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:metallurgic_infuser"));level.setBlockAndUpdate(pos,block.defaultBlockState());
        var tile=(TileEntityMekanism)level.getBlockEntity(pos);var config=((ISideConfiguration)tile).getConfig();var side=RelativeSide.fromDirections(tile.getDirection(),Direction.EAST);
        p.setShiftKeyDown(true);CoilConfiguration.apply(coil,6,1);
        var itemConfig=config.getConfig(TransmissionType.ITEM);var before=itemConfig.getDataType(side);p.gameMode.useItemOn(p,level,coil,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.EAST,pos,false));h.assertTrue(itemConfig.getDataType(side)!=before,"full server right-click dispatch cycles native Mek item side configuration");
        var foreign=java.util.UUID.randomUUID();p.server.getProfileCache().add(new com.mojang.authlib.GameProfile(foreign,"coil-owner"));tile.getSecurity().setOwnerUUID(foreign);tile.getSecurity().setMode(SecurityMode.PRIVATE);before=itemConfig.getDataType(side);CoilWrench.use(context(p,pos));h.assertTrue(itemConfig.getDataType(side)==before,"foreign private machine rejects coil configurator");
        tile.getSecurity().setOwnerUUID(p.getUUID());tile.getSecurity().setMode(SecurityMode.PUBLIC);
        for(int mode=1;mode<=5;mode++){
            CoilConfiguration.apply(coil,6,mode);var transmission=new TransmissionType[]{TransmissionType.ITEM,TransmissionType.FLUID,TransmissionType.CHEMICAL,TransmissionType.ENERGY,TransmissionType.HEAT}[mode-1];var info=config.getConfig(transmission);
            if(info!=null){var old=info.getDataType(side);CoilWrench.use(context(p,pos));h.assertTrue(info.getSupportedDataTypes().size()==1||info.getDataType(side)!=old,"native "+transmission+" side cycle");}
        }
        var inventory=tile.getInventorySlots(null);inventory.get(0).setStack(new ItemStack(Items.DIAMOND,3));CoilConfiguration.apply(coil,6,6);CoilWrench.use(context(p,pos));h.assertTrue(inventory.stream().allMatch(slot->slot.isEmpty()),"native EMPTY clears real machine slots");
        h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).stream().anyMatch(e->e.getItem().is(Items.DIAMOND)&&e.getItem().getCount()==3),"EMPTY drops stored items without loss");
        p.setShiftKeyDown(false);tile.setFacing(Direction.NORTH);CoilConfiguration.apply(coil,6,7);CoilWrench.use(context(p,pos));h.assertTrue(tile.getDirection()==Direction.EAST,"native ROTATE applies clicked facing");
        for(int mode=0;mode<8;mode++){
            CoilConfiguration.apply(coil,6,mode);var nativeItem=mekanism.common.registries.MekanismItems.CONFIGURATOR.get();var proxy=new ItemStack(nativeItem);proxy.set(nativeItem.getModeDataType(),mekanism.common.item.ItemConfigurator.ConfiguratorMode.values()[mode==0?7:mode-1]);
            for(var ability:java.util.List.of(mekanism.api.MekanismItemAbilities.WRENCH_CONFIGURE,mekanism.api.MekanismItemAbilities.WRENCH_DISMANTLE,mekanism.api.MekanismItemAbilities.WRENCH_ROTATE,mekanism.api.MekanismItemAbilities.WRENCH_EMPTY))h.assertTrue(coil.canPerformAction(ability)==proxy.canPerformAction(ability),"mode "+mode+" matches native item ability "+ability);
        }
        CoilConfiguration.apply(coil,5,0);h.assertTrue(!coil.canPerformAction(mekanism.api.MekanismItemAbilities.WRENCH_ROTATE),"disabled coil exposes no Mek wrench action");
        CoilConfiguration.apply(coil,5,1);CoilConfiguration.apply(coil,6,0);p.setShiftKeyDown(true);
        p.gameMode.useItemOn(p,level,coil,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.EAST,pos,false));h.assertTrue(level.isEmptyBlock(pos),"native Mek wrench mode dismantles through actual right-click");
        var pipePos=pos.above();level.setBlockAndUpdate(pipePos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_logistical_transporter")).defaultBlockState());
        h.runAfterDelay(5,()->{
            var pipe=(mekanism.common.tile.transmitter.TileEntityTransmitter)level.getBlockEntity(pipePos);pipe.getTransmitter().setConnectionTypeRaw(Direction.EAST,mekanism.common.lib.transmitter.ConnectionType.NONE);var oldConnection=pipe.getTransmitter().getConnectionTypeRaw(Direction.EAST);
            CoilConfiguration.apply(coil,6,1);p.setPos(pipePos.getX()+4,pipePos.getY(),pipePos.getZ()+4);p.setYRot(0);p.setXRot(-60);
            CoilWrench.use(context(p,pipePos));h.assertTrue(pipe.getTransmitter().getConnectionTypeRaw(Direction.EAST)!=oldConnection,"native configurable capability cycles transporter connection");
            h.assertTrue(!coil.has(mekanism.common.registries.MekanismItems.CONFIGURATOR.get().getModeDataType()),"native compatibility never persists an optional Mek component on coil");h.succeed();
        });
    }
}
