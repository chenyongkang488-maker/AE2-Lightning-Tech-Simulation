package dev.overloadsim.workstation;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** The clicked, owned member is the distance anchor for menus opened from a far frame. */
public final class WorkstationMenuAccess {
    private static final ThreadLocal<BlockPos> ANCHOR=new ThreadLocal<>();
    private WorkstationMenuAccess() {}
    public static BlockPos openingAnchor(HighEnergyWorkstationBlockEntity host){var anchor=ANCHOR.get();return anchor==null?host.getBlockPos():anchor;}
    public static boolean validateFrameAnchor(HighEnergyWorkstationBlockEntity host,BlockPos anchor,Player player){
        var level=host.getLevel();if(level==null||host.isRemoved()||player.level()!=level||!level.hasChunkAt(host.getBlockPos())||level.getBlockEntity(host.getBlockPos())!=host||!level.hasChunkAt(anchor)||player.distanceToSqr(anchor.getCenter())>64)return false;
        if(anchor.equals(host.getBlockPos()))return level.getBlockState(anchor).is(WorkstationContent.CONTROLLER.get());
        var state=level.getBlockState(anchor);return (state.is(dev.overloadsim.laser.LaserContent.FRAME.get())||state.is(dev.overloadsim.laser.LaserContent.PORT.get()))&&WorkstationStructureIndex.owner(level,anchor)==host;
    }
    public static void open(HighEnergyWorkstationBlockEntity host,BlockPos anchor,Player player){
        host.checkStructure();if(!validateFrameAnchor(host,anchor,player))return;ANCHOR.set(anchor.immutable());try{MenuOpener.open(WorkstationContent.MENU.get(),player,MenuLocators.forBlockEntity(host));}finally{ANCHOR.remove();}
    }
}
