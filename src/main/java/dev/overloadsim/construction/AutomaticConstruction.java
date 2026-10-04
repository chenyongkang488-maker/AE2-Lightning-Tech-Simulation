package dev.overloadsim.construction;

import appeng.api.networking.IGrid;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Server-thread material/placement transaction shared by all automatic structure builders. */
public final class AutomaticConstruction {
    private static final int MAX_CELLS=1024;
    private AutomaticConstruction() {}
    public record Plan(Map<BlockPos,BlockState> cells,BooleanSupplier available) {
        public Plan {
            java.util.Objects.requireNonNull(available);
            if(cells.isEmpty()||cells.size()>MAX_CELLS)throw new IllegalArgumentException("invalid construction size");
            var copy=new LinkedHashMap<BlockPos,BlockState>();
            cells.forEach((pos,state)->copy.put(pos.immutable(),java.util.Objects.requireNonNull(state)));
            cells=java.util.Collections.unmodifiableMap(copy);
        }
    }
    public record Result(boolean successful,int placed,String reason,List<ItemStack> missing) {
        public Result { missing=missing.stream().map(ItemStack::copy).toList(); }
        public Component message() {
            if(reason.equals("materials")) {
                var names=Component.empty();
                for(var item:missing) {
                    if(!names.getString().isEmpty())names.append(Component.literal(", "));
                    names.append(item.getHoverName()).append(Component.literal(" ×"+item.getCount()));
                }
                return Component.translatable("message.overload_sim.construction.materials",names);
            }
            return Component.translatable("message.overload_sim.construction."+reason,placed);
        }
    }
    private record Pocket(int slot,ItemStack original,int amount) {}
    private record Network(AEItemKey key,int amount) {}
    private static Result failure(String reason) {return new Result(false,0,reason,List.of());}

    public static Result build(ServerPlayer player,BlockEntity host,Plan plan,IGrid grid) {
        if(!valid(player,host))return failure("host_invalid");
        if(!available(host,plan))return failure("owned");
        var level=player.serverLevel();
        var placements=new LinkedHashMap<BlockPos,BlockState>();
        var materials=new LinkedHashMap<Item,Integer>();
        for(var entry:plan.cells().entrySet()) {
            var pos=entry.getKey();var required=entry.getValue();
            if(!level.hasChunkAt(pos))return failure("unloaded");
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return failure("bounds");
            var current=level.getBlockState(pos);
            if(matches(current,required))continue;
            if(required.isAir()||!replaceable(host,pos))return failure("blocked");
            var item=required.getBlock().asItem();
            if(item==Items.AIR)return failure("unavailable");
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,new ItemStack(item)))return failure("protected");
            placements.put(pos,required);materials.merge(item,1,Integer::sum);
        }
        if(placements.isEmpty())return new Result(true,0,"already_formed",List.of());

        var pockets=new ArrayList<Pocket>();var network=new ArrayList<Network>();
        MEStorage storage=null;
        if(grid!=null&&host instanceof IActionHost actionHost&&actionHost.getActionableNode()!=null
                &&actionHost.getActionableNode().isActive()&&actionHost.getActionableNode().getGrid()==grid)
            storage=grid.getStorageService().getInventory();
        var source=host instanceof IActionHost actionHost?IActionSource.ofPlayer(player,actionHost):IActionSource.ofPlayer(player);
        if(!player.isCreative()) {
            var missing=new ArrayList<ItemStack>();
            var available=storage==null?null:storage.getAvailableStacks();
            for(var need:materials.entrySet()) {
                int remaining=need.getValue();
                for(int slot=0;slot<41&&remaining>0;slot++) {
                    if(slot>=36&&slot!=40)continue; // Main inventory plus offhand; equipped armor is not construction stock.
                    var item=player.getInventory().getItem(slot);
                    if(item.is(need.getKey())) {
                        int take=Math.min(remaining,item.getCount());pockets.add(new Pocket(slot,item.copy(),take));remaining-=take;
                    }
                }
                if(storage!=null&&remaining>0) {
                    var canonical=AEItemKey.of(need.getKey());
                    int take=(int)Math.clamp(storage.extract(canonical,remaining,Actionable.SIMULATE,source),0L,remaining);
                    if(take>0){network.add(new Network(canonical,take));remaining-=take;}
                    if(remaining>0)for(var key:available.keySet()) {
                        if(!(key instanceof AEItemKey variant)||variant.equals(canonical)||variant.getItem()!=need.getKey())continue;
                        take=(int)Math.clamp(storage.extract(variant,remaining,Actionable.SIMULATE,source),0L,remaining);
                        if(take>0){network.add(new Network(variant,take));remaining-=take;}
                        if(remaining==0)break;
                    }
                }
                if(remaining>0)missing.add(new ItemStack(need.getKey(),remaining));
            }
            if(!missing.isEmpty())return new Result(false,0,"materials",missing);
        }

        var pocketDebits=new ArrayList<Pocket>();var networkDebits=new ArrayList<Network>();
        var snapshots=new ArrayList<BlockSnapshot>();boolean completed=false;
        try {
            for(var debit:network) {
                int drawn=(int)Math.clamp(storage.extract(debit.key(),debit.amount(),Actionable.MODULATE,source),0L,debit.amount());
                if(drawn>0)networkDebits.add(new Network(debit.key(),drawn));
                if(drawn!=debit.amount())return failure("source_changed");
            }
            if(!valid(player,host)||!available(host,plan))return failure("owned");
            for(var debit:pockets)if(!ItemStack.matches(player.getInventory().getItem(debit.slot()),debit.original()))return failure("source_changed");
            for(var debit:pockets) {
                player.getInventory().getItem(debit.slot()).shrink(debit.amount());pocketDebits.add(debit);
            }
            // AE callbacks may change the world. Recheck the entire plan before any placement.
            for(var entry:plan.cells().entrySet()) {
                if(!level.hasChunkAt(entry.getKey()))return failure("unloaded");
                var current=level.getBlockState(entry.getKey());
                if(placements.containsKey(entry.getKey())) {
                    if(!replaceable(host,entry.getKey())||!level.mayInteract(player,entry.getKey()))return failure("blocked");
                } else if(!matches(current,entry.getValue()))return failure("blocked");
            }
            for(var entry:placements.entrySet()) {
                if(!valid(player,host)||!available(host,plan))return failure("owned");
                var pos=entry.getKey();var state=entry.getValue();
                if(!level.hasChunkAt(pos)||!replaceable(host,pos))return failure("blocked");
                if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,new ItemStack(state.getBlock())))return failure("protected");
                var snapshot=BlockSnapshot.create(level.dimension(),level,pos,Block.UPDATE_CLIENTS);
                snapshots.add(snapshot);
                if(!level.setBlock(pos,state,Block.UPDATE_CLIENTS)||EventHooks.onBlockPlace(player,snapshot,Direction.UP))return failure("protected");
                if(!matches(level.getBlockState(pos),state))return failure("blocked");
            }
            if(!valid(player,host)||!available(host,plan))return failure("owned");
            for(var entry:plan.cells().entrySet())if(!level.hasChunkAt(entry.getKey())||!matches(level.getBlockState(entry.getKey()),entry.getValue()))return failure("blocked");
            completed=true;
            for(var entry:placements.entrySet())level.updateNeighborsAt(entry.getKey(),entry.getValue().getBlock());
            player.getInventory().setChanged();host.setChanged();
            return new Result(true,placements.size(),"success",List.of());
        } finally {
            if(!completed) {
                for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore(Block.UPDATE_CLIENTS);
                for(var debit:pocketDebits)refund(player,debit.original().copyWithCount(debit.amount()));
                for(var debit:networkDebits) {
                    int returned=(int)Math.clamp(storage.insert(debit.key(),debit.amount(),Actionable.MODULATE,source),0L,debit.amount());
                    if(returned<debit.amount())refund(player,debit.key().toStack(debit.amount()-returned));
                }
                for(var snapshot:snapshots)if(level.hasChunkAt(snapshot.getPos()))level.updateNeighborsAt(snapshot.getPos(),snapshot.getState().getBlock());
                player.getInventory().setChanged();
            }
        }
    }
    private static boolean valid(ServerPlayer player,BlockEntity host) {
        return player.isAlive()&&!player.isSpectator()&&player.getAbilities().mayBuild&&!host.isRemoved()
                &&host.getLevel()==player.level()&&player.distanceToSqr(host.getBlockPos().getCenter())<=64
                &&player.serverLevel().hasChunkAt(host.getBlockPos())&&player.level().getBlockEntity(host.getBlockPos())==host;
    }
    private static boolean matches(BlockState actual,BlockState required) {
        return required.isAir()?actual.isAir():actual.is(required.getBlock());
    }
    private static boolean available(BlockEntity host,Plan plan) {
        if(!plan.available().getAsBoolean())return false;
        for(var pos:plan.cells().keySet())if(dev.overloadsim.laser.LaserStructureIndex.occupiedByOther(host.getLevel(),pos,host)
                ||dev.overloadsim.multiblock.SimulationStructureIndex.occupiedByOther(host.getLevel(),pos,host))return false;
        return true;
    }
    private static boolean replaceable(BlockEntity host,BlockPos pos) {
        var level=host.getLevel();var state=level.getBlockState(pos);
        return (state.isAir()||state.canBeReplaced())&&state.getFluidState().isEmpty()&&level.getBlockEntity(pos)==null;
    }
    private static void refund(ServerPlayer player,ItemStack stack) {
        while(!stack.isEmpty()) {
            var portion=stack.split(Math.min(stack.getCount(),stack.getMaxStackSize()));
            player.getInventory().add(portion);
            if(!portion.isEmpty())player.drop(portion,false);
        }
    }
}
