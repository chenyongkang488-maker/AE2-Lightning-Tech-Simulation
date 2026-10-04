package dev.overloadsim.multiblock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.overloadsim.construction.AutomaticConstruction;
import dev.overloadsim.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the normal chamber shell while retaining legal, already placed optional parts. */
public final class SimulationAutoConstruction {
    private static final TagKey<Block> FRAMES = TagKey.create(Registries.BLOCK,ModContent.id("simulation_frames"));
    private static final TagKey<Block> GLASS = TagKey.create(Registries.BLOCK,ModContent.id("simulation_glass"));
    private static final ResourceLocation VIBRANT_GLASS = ResourceLocation.parse("ae2:quartz_vibrant_glass");
    private static final ResourceLocation INTERFACE = ResourceLocation.parse("ae2lt:overloaded_interface");

    private SimulationAutoConstruction() {}

    public static AutomaticConstruction.Plan plan(SimulationControllerBlockEntity host,int size) {
        if (size<3 || size>7) throw new IllegalArgumentException("size");
        Level level = host.getLevel();
        if (level==null || host.isRemoved() || !level.hasChunkAt(host.getBlockPos())
                || level.getBlockEntity(host.getBlockPos())!=host) throw new IllegalArgumentException("unavailable");
        BlockState controllerState = level.getBlockState(host.getBlockPos());
        if (!controllerState.is(MultiblockContent.CONTROLLER.get())) throw new IllegalArgumentException("unavailable");
        Direction facing = controllerState.getValue(SimulationControllerBlock.FACING);
        int middle = (size-1)/2;
        BlockPos min = switch (facing) {
            case NORTH -> host.getBlockPos().offset(-middle,0,0);
            case SOUTH -> host.getBlockPos().offset(-middle,0,1-size);
            case WEST -> host.getBlockPos().offset(0,0,-middle);
            case EAST -> host.getBlockPos().offset(1-size,0,-middle);
            default -> throw new IllegalArgumentException("unavailable");
        };
        var cells = new LinkedHashMap<BlockPos,BlockState>();
        BlockState frame = MultiblockContent.FRAME.get().defaultBlockState();
        BlockState glass = BuiltInRegistries.BLOCK.get(VIBRANT_GLASS).defaultBlockState();
        if (glass.isAir()) throw new IllegalArgumentException("unavailable");
        int interfaces=0,overloads=0,smelting=0;
        for (int x=0;x<size;x++) for (int y=0;y<size;y++) for (int z=0;z<size;z++) {
            BlockPos pos = min.offset(x,y,z);
            if (!level.hasChunkAt(pos)) throw new IllegalArgumentException("unloaded");
            if (owned(level,pos,host)) throw new IllegalArgumentException("owned");
            BlockState existing = level.getBlockState(pos);
            int boundaries = (x==0||x==size-1?1:0)+(y==0||y==size-1?1:0)+(z==0||z==size-1?1:0);
            if (existing.is(MultiblockContent.CONTROLLER.get())) {
                if (!pos.equals(host.getBlockPos())) throw new IllegalArgumentException("controllers");
                cells.put(pos,existing);continue;
            }
            BlockState expected;
            if (boundaries==0) {
                boolean ownLight = pos.equals(SimulationLightBlockEntity.center(min,size))
                        && existing.is(MultiblockContent.LIGHT.get())
                        && level.getBlockEntity(pos) instanceof SimulationLightBlockEntity light && light.ownedBy(host);
                expected = ownLight ? existing : Blocks.AIR.defaultBlockState();
            } else if (boundaries>=2) {
                if (BuiltInRegistries.BLOCK.getKey(existing.getBlock()).equals(INTERFACE)) {
                    if (y!=0 || boundaries!=2) throw new IllegalArgumentException("interface_position");
                    if (++interfaces>1) throw new IllegalArgumentException("duplicates");
                    expected = existing;
                } else expected = isFrame(existing) ? existing : frame;
            } else if (y==size-1) {
                expected = isFrame(existing) ? existing : frame;
            } else if (y==0) {
                var kind = MultiblockData.module(existing.getBlock());
                if (kind!=null) {
                    if (kind==SimulationPartBlock.Kind.OVERLOAD && ++overloads>1
                            || kind==SimulationPartBlock.Kind.SMELTING && ++smelting>1) throw new IllegalArgumentException("duplicates");
                    expected = existing;
                } else expected = existing.is(FRAMES) ? existing : frame;
            } else {
                if (existing.is(MultiblockContent.GLASS.get())) {
                    if (!(level.getBlockEntity(pos) instanceof SimulationMemberBlockEntity member) || !member.ownedBy(host))
                        throw new IllegalArgumentException("owned");
                    expected = existing;
                } else expected = existing.is(GLASS) || existing.is(glass.getBlock()) ? existing : glass;
            }
            cells.put(pos,expected);
        }
        UUID identity = host.identity();
        return new AutomaticConstruction.Plan(cells,()->available(host,identity,controllerState,cells));
    }

    public static AutomaticConstruction.Result build(ServerPlayer player,SimulationControllerBlockEntity host,int size) {
        Level level = host.getLevel();
        if (level==null || level.isClientSide || host.isRemoved() || player.level()!=level
                || !level.hasChunkAt(host.getBlockPos()) || level.getBlockEntity(host.getBlockPos())!=host
                || player.distanceToSqr(host.getBlockPos().getCenter())>64) return failure("unavailable");
        if (host.structure()!=null) return new AutomaticConstruction.Result(true,0,"already_formed",List.of());
        final AutomaticConstruction.Plan plan;
        try { plan=plan(host,size); }
        catch (IllegalArgumentException invalid) { return failure(invalid.getMessage()); }
        var grid = host.getMainNode().isActive() ? host.getMainNode().getGrid() : null;
        var result = AutomaticConstruction.build(player,host,plan,grid);
        if (!result.successful()) return result;
        host.checkStructure();
        if (host.structure()==null) return new AutomaticConstruction.Result(false,result.placed(),"formation_cancelled",List.of());
        return result;
    }

    private static boolean isFrame(BlockState state) {
        return state.is(MultiblockContent.FRAME.get()) || state.is(FRAMES);
    }

    private static boolean owned(Level level,BlockPos pos,SimulationControllerBlockEntity host) {
        return SimulationStructureIndex.occupied(level,pos,host)
                || level.getBlockEntity(pos) instanceof SimulationMemberBlockEntity member
                && member.owner()!=null && !member.ownedBy(host);
    }

    private static boolean available(SimulationControllerBlockEntity host,UUID identity,BlockState controllerState,
            Map<BlockPos,BlockState> cells) {
        Level level=host.getLevel();
        if (level==null || host.isRemoved() || !host.identity().equals(identity) || host.structure()!=null
                || !level.hasChunkAt(host.getBlockPos()) || level.getBlockEntity(host.getBlockPos())!=host
                || !level.getBlockState(host.getBlockPos()).equals(controllerState)) return false;
        for (BlockPos pos:cells.keySet()) if (!level.hasChunkAt(pos) || owned(level,pos,host)) return false;
        return true;
    }

    private static AutomaticConstruction.Result failure(String reason) {
        return new AutomaticConstruction.Result(false,0,reason,List.of());
    }
}
