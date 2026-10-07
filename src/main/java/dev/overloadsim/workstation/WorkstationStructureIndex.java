package dev.overloadsim.workstation;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Owns the whole 343-cell envelope, including clear interior cells. Never loads chunks. */
public final class WorkstationStructureIndex {
    private record Claim(BlockPos controller,UUID identity) {}
    private static final Map<Level,Map<BlockPos,Claim>> CLAIMS=new WeakHashMap<>();
    private static boolean registered;
    private WorkstationStructureIndex() {}
    private static Claim claim(Level level,BlockPos position){
        var claims=CLAIMS.get(level);if(claims==null)return null;var claim=claims.get(position);if(claim==null)return null;
        if(!level.hasChunkAt(claim.controller())||!(level.getBlockEntity(claim.controller()) instanceof HighEnergyWorkstationBlockEntity host)||host.isRemoved()||!host.identity().equals(claim.identity())||!host.structureClaimed()){claims.values().removeIf(value->value.equals(claim));return null;}return claim;
    }
    public static boolean occupiedByOther(Level level,BlockPos position,BlockEntity host){
        var claim=claim(level,position);return claim!=null&&(!(host instanceof HighEnergyWorkstationBlockEntity station)||!claim.controller().equals(host.getBlockPos())||!claim.identity().equals(station.identity()));
    }
    public static HighEnergyWorkstationBlockEntity owner(Level level,BlockPos position){
        if(level==null||level.isClientSide||!level.hasChunkAt(position))return null;var claim=claim(level,position);if(claim==null)return null;
        var host=(HighEnergyWorkstationBlockEntity)level.getBlockEntity(claim.controller());host.checkStructure();
        return host.formed()&&claim.equals(claim(level,position))?host:null;
    }
    public static boolean bind(HighEnergyWorkstationBlockEntity host,Collection<BlockPos> members){
        for(var pos:members)if(occupiedByOther(host.getLevel(),pos,host))return false;
        release(host);var claims=CLAIMS.computeIfAbsent(host.getLevel(),ignored->new HashMap<>());var claim=new Claim(host.getBlockPos().immutable(),host.identity());for(var pos:members)claims.put(pos.immutable(),claim);return true;
    }
    public static void release(HighEnergyWorkstationBlockEntity host){var claims=CLAIMS.get(host.getLevel());if(claims!=null)claims.values().removeIf(claim->claim.controller().equals(host.getBlockPos())&&claim.identity().equals(host.identity()));}
    public static void changed(Level level,BlockPos pos){var claim=claim(level,pos);if(claim!=null&&level.getBlockEntity(claim.controller()) instanceof HighEnergyWorkstationBlockEntity host)host.markStructureDirty();}
    public static void register(){if(registered)return;registered=true;NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event)->changed((Level)event.getLevel(),event.getPos()));NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event)->{if(event.getLevel() instanceof Level level)changed(level,event.getPos());});}
}
