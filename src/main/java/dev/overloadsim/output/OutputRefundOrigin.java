package dev.overloadsim.output;

import appeng.api.networking.*;
import appeng.api.parts.IPartHost;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.parts.AEBasePart;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Read-only original-network identity. All persistent origin data belongs to the new output block. */
record OutputRefundOrigin(int frequencyId,ResourceKey<Level> dimension,BlockPos pos,int partSide,boolean part,UUID identity) {
    private static final Map<Object,UUID> RUNTIME_IDENTITIES=Collections.synchronizedMap(new WeakHashMap<>());
    private record Anchor(Object owner,BlockEntity block,IGridNode node,int side,boolean part) {}
    static @Nullable OutputRefundOrigin capture(OutputInterfaceBlockEntity host,IGrid grid){
        if(!(host.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return null;
        int frequency=host.getFrequencyId();
        if(frequency>0){
            var transmitter=FrequencyApi.getTransmitter(level.getServer(),frequency);
            if(transmitter.isPresent()){
                var info=transmitter.get();var target=level.getServer().getLevel(info.dimension());
                if(target!=null&&target.hasChunkAt(info.pos())&&target.getBlockEntity(info.pos()) instanceof AENetworkedBlockEntity anchor&&anchor.getMainNode().getGrid()==grid)
                    return fromAnchor(new Anchor(anchor,anchor,anchor.getMainNode().getNode(),-1,false),frequency);
            }
        }
        var candidates=new ArrayList<Anchor>();
        for(var node:grid.getNodes()){
            if(node.getOwner() instanceof AENetworkedBlockEntity owner&&owner!=host&&!owner.isRemoved()&&owner.getMainNode().getNode()==node)candidates.add(new Anchor(owner,owner,node,-1,false));
            else if(node.getOwner() instanceof AEBasePart owner&&owner.getMainNode().getNode()==node&&owner.getBlockEntity()!=null&&!owner.getBlockEntity().isRemoved())
                candidates.add(new Anchor(owner,owner.getBlockEntity(),node,owner.getSide()==null?-1:owner.getSide().get3DDataValue(),true));
        }
        candidates.sort(Comparator.comparingInt((Anchor anchor)->anchor.block() instanceof DriveBlockEntity?0:anchor.part()?2:1)
                .thenComparing(anchor->anchor.block().getLevel().dimension().location().toString()).thenComparingLong(anchor->anchor.block().getBlockPos().asLong()));
        return candidates.isEmpty()?null:fromAnchor(candidates.getFirst(),-1);
    }
    private static OutputRefundOrigin fromAnchor(Anchor anchor,int frequency){
        var identity=RUNTIME_IDENTITIES.computeIfAbsent(anchor.owner(),unused->UUID.randomUUID());
        return new OutputRefundOrigin(frequency,anchor.block().getLevel().dimension(),anchor.block().getBlockPos().immutable(),anchor.side(),anchor.part(),identity);
    }
    @Nullable IGrid resolve(MinecraftServer server){
        var level=server.getLevel(dimension);if(level==null||!level.hasChunkAt(pos))return null;
        var block=level.getBlockEntity(pos);if(block==null||block.isRemoved())return null;
        Object owner=block;IGridNode node=null;
        if(part){
            var container=block instanceof IPartHost host?host:null;
            if(container==null)return null;
            var candidate=container.getPart(partSide<0?null:Direction.from3DDataValue(partSide));
            if(!(candidate instanceof AEBasePart base))return null;
            owner=base;node=base.getMainNode().getNode();
        }else if(block instanceof AENetworkedBlockEntity networked)node=networked.getMainNode().getNode();
        if(node==null)return null;
        boolean sameRuntime=identity.equals(RUNTIME_IDENTITIES.get(owner));
        if(frequencyId>0){
            var transmitter=FrequencyApi.getTransmitter(server,frequencyId);
            if(transmitter.isEmpty()||!dimension.equals(transmitter.get().dimension())||!pos.equals(transmitter.get().pos()))return null;
            // The public frequency system supplies a persistent logical network identity.
            return node.getGrid();
        }
        // AE2 no longer serializes the historical stable grid-storage id. A recreated wired
        // anchor cannot be proven original; retain ownership rather than guessing a refund.
        return sameRuntime?node.getGrid():null;
    }
    CompoundTag toTag(){var tag=new CompoundTag();tag.putInt("Frequency",frequencyId);tag.putString("Dimension",dimension.location().toString());tag.putLong("Position",pos.asLong());tag.putInt("PartSide",partSide);tag.putBoolean("Part",part);tag.putUUID("Identity",identity);return tag;}
    static @Nullable OutputRefundOrigin fromTag(CompoundTag tag){
        if(!tag.hasUUID("Identity"))return null;
        try{int side=tag.getInt("PartSide");if(side< -1||side>5)return null;return new OutputRefundOrigin(tag.getInt("Frequency"),ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(tag.getString("Dimension"))),BlockPos.of(tag.getLong("Position")),side,tag.getBoolean("Part"),tag.getUUID("Identity"));}
        catch(IllegalArgumentException malformed){return null;}
    }
}
