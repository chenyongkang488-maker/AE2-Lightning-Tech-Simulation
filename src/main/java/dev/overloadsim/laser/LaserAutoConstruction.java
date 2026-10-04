package dev.overloadsim.laser;
import dev.overloadsim.construction.AutomaticConstruction;
import net.minecraft.server.level.ServerPlayer;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the exact gameplay template; no additional structure or material rules live in the UI. */
public final class LaserAutoConstruction {
    private static final ResourceLocation INTERFACE=ResourceLocation.parse("ae2lt:overloaded_interface");
    private LaserAutoConstruction() {}
    public static AutomaticConstruction.Plan plan(LaserMachineBlockEntity host) {
        var level=host.getLevel();var identity=host.identity();var controller=host.getBlockPos();
        var kind=host.kind();var facing=host.getBlockState().getValue(LaserMachineBlock.FACING);
        boolean receiverPedestalLayout=host.receiverPedestalLayout();
        var cells=new LinkedHashMap<BlockPos,BlockState>();int interfaces=0;
        for(var entry:host.structureTemplate().entrySet()) {
            var pos=controller.offset(entry.getKey());
            if(!level.hasChunkAt(pos))throw new IllegalArgumentException("unloaded");
            var block=switch(entry.getValue()) {
                case CONTROLLER->LaserContent.machine(kind);case FRAME->LaserContent.FRAME.get();case GLASS->LaserContent.GLASS.get();
                case SOURCE->LaserContent.SOURCE.get();case PORT->LaserContent.PORT.get();case AIR->Blocks.AIR;
            };
            var state=entry.getValue()==LaserStructureGeometry.Part.CONTROLLER?host.getBlockState():block.defaultBlockState();
            var current=level.getBlockState(pos);
            if(entry.getValue()==LaserStructureGeometry.Part.FRAME&&BuiltInRegistries.BLOCK.getKey(current.getBlock()).equals(INTERFACE)) {
                var relative=pos.subtract(host.center());var normal=host.forward().getNormal();
                if(++interfaces>1)throw new IllegalArgumentException("duplicates");
                if(relative.getX()*normal.getX()+relative.getY()*normal.getY()+relative.getZ()*normal.getZ()>0)throw new IllegalArgumentException("interface_position");
                state=current;
            }
            cells.put(pos.immutable(),state);
        }
        return new AutomaticConstruction.Plan(cells,()->host.getLevel()==level&&!host.isRemoved()
                &&host.identity().equals(identity)&&level.hasChunkAt(controller)&&level.getBlockEntity(controller)==host
                &&host.getBlockState().getValue(LaserMachineBlock.FACING)==facing
                &&host.receiverPedestalLayout()==receiverPedestalLayout
                &&cells.keySet().stream().noneMatch(pos->LaserStructureIndex.occupied(level,pos,host)||LaserStructureIndex.touching(level,pos,host)));
    }
    public static AutomaticConstruction.Result build(ServerPlayer player,LaserMachineBlockEntity host) {
        if(host.isRemoved()||host.getLevel()!=player.level()||!player.isAlive()||player.isSpectator()
                ||player.distanceToSqr(host.getBlockPos().getCenter())>64||!host.getLevel().hasChunkAt(host.getBlockPos())
                ||host.getLevel().getBlockEntity(host.getBlockPos())!=host)
            return new AutomaticConstruction.Result(false,0,"host_invalid",List.of());
        host.checkStructure();
        if(host.formed())return new AutomaticConstruction.Result(true,0,"already_formed",List.of());
        if(host.explosionTriggered())return new AutomaticConstruction.Result(false,0,"unavailable",List.of());
        final AutomaticConstruction.Plan plan;
        try {plan=plan(host);}catch(IllegalArgumentException invalid){return new AutomaticConstruction.Result(false,0,invalid.getMessage(),List.of());}
        var result=AutomaticConstruction.build(player,host,plan,host.getMainNode().isActive()?host.getMainNode().getGrid():null);
        if(result.successful()) {
            host.checkStructure();host.bridge().refresh();host.sync();
            if(!host.formed())return new AutomaticConstruction.Result(false,result.placed(),"formation_cancelled",List.of());
        }
        return result;
    }
}
