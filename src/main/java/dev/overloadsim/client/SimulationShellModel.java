package dev.overloadsim.client;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.*;
import dev.overloadsim.multiblock.SimulationShellVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;
import net.neoforged.neoforge.common.util.TriState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/** Position-dependent skin for formed frames/controller and the real, unmodified AE2LT port. */
public final class SimulationShellModel extends BakedModelWrapper<BakedModel> {
    public enum Kind{FRAME,CONTROLLER,PORT}
    private static final ModelProperty<SimulationShellRegistry.Cell> CELL=new ModelProperty<>();
    private record Key(int tile,int rotation,Direction face){}
    private final Kind kind;private final Map<Key,List<BakedQuad>> quads=new ConcurrentHashMap<>();
    public SimulationShellModel(BakedModel original,Kind kind){super(original);this.kind=kind;}
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData existing){
        var data=originalModel.getModelData(level,pos,state,existing);var cell=SimulationShellVisuals.lookup(level,pos);
        if(cell==null||cell.role()==SimulationShellTopology.Role.GLASS||cell.role()==SimulationShellTopology.Role.FLOOR)return data;
        return data.derive().with(CELL,cell).build();
    }
    @Override public TriState useAmbientOcclusion(BlockState state,ModelData data,RenderType type){return data.has(CELL)?TriState.FALSE:super.useAmbientOcclusion(state,data,type);}
    @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,RenderType type){
        var cell=data.get(CELL);if(cell==null)return originalModel.getQuads(state,face,random,data,type);if(face==null)return List.of();
        int n=cell.size(),x=cell.x(),y=cell.y(),z=cell.z(),tile=0,rotation=0;
        if(face==Direction.UP&&y==n-1){
            boolean xb=x==0||x==n-1,zb=z==0||z==n-1;tile=xb&&zb?9:xb||zb?8:6;
            if(xb&&zb)rotation=z==0?(x==0?0:1):(x==0?3:2);else if(xb)rotation=x==0?3:1;else if(z==n-1)rotation=2;
        }else if(face.getAxis()!=Direction.Axis.Y){
            var role=cell.role();tile=role==SimulationShellTopology.Role.VERTICAL?2:role==SimulationShellTopology.Role.CORNER||role==SimulationShellTopology.Role.ROOF_CORNER?7:1;
            if(tile==7){boolean start=switch(face){case NORTH->x==n-1;case SOUTH->x==0;case WEST->z==0;default->z==n-1;};rotation=y==0?(start?2:3):(start?1:0);}
            String outward=SimulationShellTopology.outsideFace(n,x,y,z);
            if(face.name().equals(outward)&&kind!=Kind.FRAME){tile=kind==Kind.CONTROLLER?4:5;rotation=0;}
        }
        return quads.computeIfAbsent(new Key(tile,rotation,face),this::bake);
    }
    private List<BakedQuad> bake(Key key){
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/simulation_shell_atlas"));
        float[][] positions=switch(key.face){
            case NORTH->new float[][]{{1,0,0},{0,0,0},{0,1,0},{1,1,0}};
            case SOUTH->new float[][]{{0,0,1},{1,0,1},{1,1,1},{0,1,1}};
            case WEST->new float[][]{{0,0,0},{0,0,1},{0,1,1},{0,1,0}};
            case EAST->new float[][]{{1,0,1},{1,0,0},{1,1,0},{1,1,1}};
            case UP->new float[][]{{0,1,1},{1,1,1},{1,1,0},{0,1,0}};
            case DOWN->new float[][]{{0,0,0},{1,0,0},{1,0,1},{0,0,1}};};
        float[][] uv={{0,1},{1,1},{1,0},{0,0}};int[] vertices=new int[32];
        for(int i=0;i<4;i++){
            int offset=i*8;for(int axis=0;axis<3;axis++)vertices[offset+axis]=Float.floatToRawIntBits(positions[i][axis]);vertices[offset+3]=-1;
            var point=uv[(i+key.rotation)%4];float u=(key.tile%4*32+.02f+point[0]*31.96f)/128f,v=(key.tile/4*32+.02f+point[1]*31.96f)/128f;
            vertices[offset+4]=Float.floatToRawIntBits(sprite.getU(u));vertices[offset+5]=Float.floatToRawIntBits(sprite.getV(v));
            vertices[offset+7]=(key.face.getStepX()*127&255)|(key.face.getStepY()*127&255)<<8|(key.face.getStepZ()*127&255)<<16;
        }return List.of(new BakedQuad(vertices,-1,key.face,sprite,true,false));
    }
}
