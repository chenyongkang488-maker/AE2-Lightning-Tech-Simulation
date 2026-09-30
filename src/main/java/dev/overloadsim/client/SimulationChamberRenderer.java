package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.machine.SimulationChamberBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Client visuals use the small world update packet, independently of an open menu. */
public final class SimulationChamberRenderer implements BlockEntityRenderer<SimulationChamberBlockEntity> {
    private final ItemRenderer items;
    private final ItemStack crystal=new ItemStack(ModContent.PERFECT.get());
    public SimulationChamberRenderer(BlockEntityRendererProvider.Context context){items=context.getItemRenderer();}

    @Override public void render(SimulationChamberBlockEntity machine,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(!machine.displayHasCrystal()||machine.getLevel()==null)return;
        long tick=machine.getLevel().getGameTime();double time=tick+partialTick;
        float height=.52f+(float)Math.sin(time*.09)*.025f;
        poses.pushPose();poses.translate(.5,height,.5);poses.mulPose(Axis.YP.rotationDegrees((float)(time*1.8%360)));poses.scale(.48f,.48f,.48f);
        items.renderStatic(crystal,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,poses,buffers,machine.getLevel(),0);poses.popPose();

        // Four bounded arcs originate at the corner emitters. No world lightning entities are spawned.
        var random=RandomSource.create(machine.getBlockPos().asLong()^(tick/3)*31);
        VertexConsumer vertices=buffers.getBuffer(RenderType.lightning());Matrix4f matrix=poses.last().pose();
        float alpha=machine.displayWorking()?.58f:.25f;
        for(int emitter=0;emitter<4;emitter++){
            float startX=(emitter&1)==0?3.25f/16:12.75f/16,startZ=(emitter&2)==0?3.25f/16:12.75f/16;
            float x=startX,y=6f/16,z=startZ;
            for(int segment=1;segment<=5;segment++){
                float t=segment/5f,jitter=segment==5?0:.045f;
                float nextX=startX+(.5f-startX)*t+(random.nextFloat()-.5f)*jitter;
                float nextY=6f/16+(height-6f/16)*t+(random.nextFloat()-.5f)*jitter;
                float nextZ=startZ+(.5f-startZ)*t+(random.nextFloat()-.5f)*jitter;
                ribbon(matrix,vertices,x,y,z,nextX,nextY,nextZ,.014f,1,.42f,.76f,alpha);
                ribbon(matrix,vertices,x,y,z,nextX,nextY,nextZ,.004f,1,.94f,1,alpha+.2f);
                x=nextX;y=nextY;z=nextZ;
            }
        }
    }
    private static void ribbon(Matrix4f matrix,VertexConsumer vertices,float x,float y,float z,float nx,float ny,float nz,float width,float r,float g,float b,float a){
        // Crossed ribbons keep each arc visible from all four open sides.
        vertices.addVertex(matrix,x-width,y,z).setColor(r,g,b,a);vertices.addVertex(matrix,nx-width,ny,nz).setColor(r,g,b,a);
        vertices.addVertex(matrix,nx+width,ny,nz).setColor(r,g,b,a);vertices.addVertex(matrix,x+width,y,z).setColor(r,g,b,a);
        vertices.addVertex(matrix,x+width,y,z).setColor(r,g,b,a);vertices.addVertex(matrix,nx+width,ny,nz).setColor(r,g,b,a);
        vertices.addVertex(matrix,nx-width,ny,nz).setColor(r,g,b,a);vertices.addVertex(matrix,x-width,y,z).setColor(r,g,b,a);
        vertices.addVertex(matrix,x,y,z-width).setColor(r,g,b,a);vertices.addVertex(matrix,nx,ny,nz-width).setColor(r,g,b,a);
        vertices.addVertex(matrix,nx,ny,nz+width).setColor(r,g,b,a);vertices.addVertex(matrix,x,y,z+width).setColor(r,g,b,a);
        vertices.addVertex(matrix,x,y,z+width).setColor(r,g,b,a);vertices.addVertex(matrix,nx,ny,nz+width).setColor(r,g,b,a);
        vertices.addVertex(matrix,nx,ny,nz-width).setColor(r,g,b,a);vertices.addVertex(matrix,x,y,z-width).setColor(r,g,b,a);
    }
}
