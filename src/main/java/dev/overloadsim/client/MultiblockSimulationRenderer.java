package dev.overloadsim.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.SimulationOrbState;
import dev.overloadsim.multiblock.SimulationControllerBlock;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
/** One bounded renderer draws the working sphere, coils and screen without world entities. */
public final class MultiblockSimulationRenderer implements BlockEntityRenderer<SimulationControllerBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    public MultiblockSimulationRenderer(BlockEntityRendererProvider.Context context){this.context=context;}
    @Override public boolean shouldRenderOffScreen(SimulationControllerBlockEntity be){return be.visualSize()>0;}
    @Override public int getViewDistance(){return 96;}
    @Override public AABB getRenderBoundingBox(SimulationControllerBlockEntity be){var min=be.visualMin();int size=Math.max(1,be.visualSize());return (min==null?new AABB(be.getBlockPos()):new AABB(min.getX(),min.getY(),min.getZ(),min.getX()+size,min.getY()+size,min.getZ()+size)).inflate(.02);}
    @Override public void render(SimulationControllerBlockEntity be,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        int n=be.visualSize();if(n<3||be.getLevel()==null||be.visualMin()==null)return;
        var min=be.visualMin();double ox=min.getX()-be.getBlockPos().getX(),oy=min.getY()-be.getBlockPos().getY(),oz=min.getZ()-be.getBlockPos().getZ();
        long tick=be.getLevel().getGameTime();double time=tick+partial;float center=n/2f,inner=n-2;
        var state=SimulationOrbState.of(be.visualFlags());screen(be,poses,buffers,state.visible());
        poses.pushPose();poses.translate(ox,oy,oz);
        float orbY=center+(float)Math.sin(time*.055)*inner*.025f,radius=inner*.275f;
        if(state.visible()){
            var orb=Minecraft.getInstance().getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ModContent.id("block/"+(state.smelting()?"simulation_energy_orb_orange":"simulation_energy_orb"))));
            float scale=radius*2/.75f;
            poses.pushPose();poses.translate(center,orbY,center);poses.mulPose(Axis.YP.rotationDegrees((float)(time*.8)));poses.mulPose(Axis.ZP.rotationDegrees(8));poses.scale(scale,scale,scale);poses.translate(-.5,-.5,-.5);
            context.getBlockRenderDispatcher().getModelRenderer().renderModel(poses.last(),buffers.getBuffer(RenderType.cutout()),null,orb,1,1,1,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);poses.popPose();
        }
        var model=Minecraft.getInstance().getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ModContent.id("block/simulation_emitter")));
        for(int corner=0;corner<4;corner++){
            float x=(corner&1)==0?1.05f:n-1.6f,z=(corner&2)==0?1.05f:n-1.6f;
            poses.pushPose();poses.translate(x,n-1.65f,z);poses.scale(.55f,.55f,.55f);
            context.getBlockRenderDispatcher().getModelRenderer().renderModel(poses.last(),buffers.getBuffer(RenderType.cutout()),null,model,1,1,1,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);poses.popPose();
        }
        if(state.visible()){
            var vertices=buffers.getBuffer(RenderType.lightning());var matrix=poses.last().pose();var random=RandomSource.create(be.getBlockPos().asLong()^tick/3*31);
            for(int corner=0;corner<4;corner++)arc(matrix,vertices,random,(corner&1)==0?1.325f:n-1.325f,n-1.5f,(corner&2)==0?1.325f:n-1.325f,center+((corner&1)==0?-1:1)*radius*.55f,orbY+radius*.5f,center+((corner&2)==0?-1:1)*radius*.55f,.028f);
            if(state.overload()){
                float ringRadius=inner*.38f;for(int segment=0;segment<24;segment++){
                    double a=segment*Math.PI/12+time*.09,b=(segment+1)*Math.PI/12+time*.09;
                    arc(matrix,vertices,random,center+(float)Math.cos(a)*ringRadius,center+(float)Math.sin(a*2)*.12f,center+(float)Math.sin(a)*ringRadius,center+(float)Math.cos(b)*ringRadius,center+(float)Math.sin(b*2)*.12f,center+(float)Math.sin(b)*ringRadius,.018f);
                }
            }
        }poses.popPose();
    }
    private static void screen(SimulationControllerBlockEntity be,PoseStack poses,MultiBufferSource buffers,boolean working){
        poses.pushPose();poses.translate(.5,0,.5);
        var facing=be.getBlockState().getValue(SimulationControllerBlock.FACING);
        poses.mulPose(Axis.YP.rotationDegrees(switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));
        var m=poses.last().pose();
        if(working){
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/simulation_controller_ecg"));
            var v=buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
            screenVertex(v,m,-.25f,.29f,sprite.getU0(),sprite.getV1());screenVertex(v,m,-.25f,.6875f,sprite.getU0(),sprite.getV0());screenVertex(v,m,.25f,.6875f,sprite.getU1(),sprite.getV0());screenVertex(v,m,.25f,.29f,sprite.getU1(),sprite.getV1());
        }else{
            var v=buffers.getBuffer(RenderType.lightning());
            for(var p:new float[][]{{-.2f,.481f},{-.2f,.497f},{.2f,.497f},{.2f,.481f}})v.addVertex(m,p[0],p[1],-.502f).setColor(1,.65f,.85f,1);
        }
        poses.popPose();
    }
    private static void screenVertex(VertexConsumer v,Matrix4f m,float x,float y,float u,float w){v.addVertex(m,x,y,-.502f).setColor(255,255,255,255).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,-1);}
    private static void arc(Matrix4f m,VertexConsumer v,RandomSource r,float x,float y,float z,float ex,float ey,float ez,float width){
        float sx=x,sy=y,sz=z;
        for(int i=1;i<=6;i++){float t=i/6f,j=i==6?0:.12f;float nx=sx+(ex-sx)*t+(r.nextFloat()-.5f)*j,ny=sy+(ey-sy)*t+(r.nextFloat()-.5f)*j,nz=sz+(ez-sz)*t+(r.nextFloat()-.5f)*j;
            ribbon(m,v,x,y,z,nx,ny,nz,width,1,.44f,.78f,.65f);ribbon(m,v,x,y,z,nx,ny,nz,width*.3f,1,.96f,1,.9f);x=nx;y=ny;z=nz;
        }
    }
    private static void ribbon(Matrix4f m,VertexConsumer v,float x,float y,float z,float nx,float ny,float nz,float w,float r,float g,float b,float a){
        for(int axis=0;axis<2;axis++)for(int reverse=0;reverse<2;reverse++){
            float dx=axis==0?w:0,dz=axis==1?w:0;
            float[][] points=reverse==0?new float[][]{{x-dx,y,z-dz},{nx-dx,ny,nz-dz},{nx+dx,ny,nz+dz},{x+dx,y,z+dz}}:new float[][]{{x+dx,y,z+dz},{nx+dx,ny,nz+dz},{nx-dx,ny,nz-dz},{x-dx,y,z-dz}};
            for(var p:points)v.addVertex(m,p[0],p[1],p[2]).setColor(r,g,b,a);
        }
    }
}
