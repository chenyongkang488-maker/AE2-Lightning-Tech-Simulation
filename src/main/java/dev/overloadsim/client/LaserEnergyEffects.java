package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.CoreCollapseCycle;
import dev.overloadsim.core.CoreMembraneMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Strictly bounded cosmetic surfaces; no world particles, damage or resource mutations. */
final class LaserEnergyEffects {
    private LaserEnergyEffects(){}
    static void beam(PoseStack.Pose pose,VertexConsumer v,TextureAtlasSprite sprite,Vec3 start,Vec3 end,double r0,double r1,float alpha){
        Vec3 direction=end.subtract(start).normalize();
        Vec3 normal=direction.cross(Math.abs(direction.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
        Vec3 tangent=direction.cross(normal).normalize();
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
            Vec3 u=normal.scale(Math.cos(a)).add(tangent.scale(Math.sin(a))),w=normal.scale(Math.cos(b)).add(tangent.scale(Math.sin(b)));
            Vec3[] points={start.add(u.scale(r0)),end.add(u.scale(r1)),end.add(w.scale(r1)),start.add(w.scale(r0))};
            for(int reverse=0;reverse<2;reverse++)for(int corner=0;corner<4;corner++){
                int j=reverse==0?corner:3-corner;var p=points[j];
                v.addVertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).setColor(1f,1f,1f,alpha)
                        .setUv(sprite.getU(j==0||j==3?0:1),sprite.getV(j<2?0:1))
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose,(float)u.x,(float)u.y,(float)u.z);
            }
        }
    }
    static void burst(Matrix4f m,VertexConsumer v,Vec3 center,Vec3 right,Vec3 up,Vec3 forward,double time,long seed){
        for(int i=0;i<8;i++){
            double angle=i*Math.PI/4;
            Vec3 start=center.add(right.scale(2.8*Math.cos(angle))).add(up.scale(2.8*Math.sin(angle)));
            Vec3 end=center.add(right.scale(.32*Math.cos(angle+time*.03))).add(up.scale(.32*Math.sin(angle+time*.03)))
                    .add(forward.scale(Math.sin(time*.11+i)*.5));
            arc(m,v,start,end,time,seed+i,1,.35f,.74f,.032);
        }
    }
    static void core(BlockEntityRendererProvider.Context context,Vec3 center,double time,long start,PoseStack poses,MultiBufferSource buffers){
        double age=start<0?CoreCollapseCycle.DURATION:Math.max(0,time-start);
        var model=Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(ModContent.id("block/collapse_energy_orb")));
        poses.pushPose();poses.translate(center.x,center.y,center.z);
        poses.mulPose(Axis.YP.rotationDegrees((float)(time*1.8)));
        float scale=(float)CoreCollapseCycle.modelScale(age);poses.scale(scale,scale,scale);poses.translate(-.5,-.5,-.5);
        context.getBlockRenderDispatcher().getModelRenderer().renderModel(poses.last(),buffers.getBuffer(RenderType.beaconBeam(InventoryMenu.BLOCK_ATLAS,false)),null,model,1,1,1,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        poses.popPose();
        double halo=CoreCollapseCycle.halo(age);if(halo<=0)return;
        // Ordinary alpha preserves the pale-orange membrane colour on a bright sky.
        var v=buffers.getBuffer(RenderType.debugQuads());
        for(int ring=0;ring<2;ring++){
            poses.pushPose();poses.translate(center.x,center.y,center.z);
            poses.mulPose(Axis.XP.rotationDegrees(ring==0?35:-35));
            poses.mulPose(Axis.ZP.rotationDegrees((float)(time*.7*(ring==0?1:-1))));
            poses.scale((float)halo,(float)halo,(float)halo);poses.translate(0,0,-1/32d);
            var m=poses.last().pose();
            for(var surface:CoreMembraneMesh.ring(ring)){
                var q=surface.quad();
                for(var p:new dev.overloadsim.core.ReceiverShellMesh.Vertex[]{q.a(),q.b(),q.c(),q.d()})
                    v.addVertex(m,(float)p.x(),(float)p.y(),(float)p.z()).setColor(1,.74f,.48f,.42f);
            }
            poses.popPose();
        }
    }
    private static void arc(Matrix4f m,VertexConsumer v,Vec3 start,Vec3 end,double time,long seed,float red,float green,float blue,double width){
        Vec3 direction=end.subtract(start).normalize();
        Vec3 normal=direction.cross(Math.abs(direction.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
        Vec3 tangent=direction.cross(normal).normalize();Vec3 previous=start;
        long frame=(long)Math.floor(time/3);
        for(int step=1;step<=5;step++){
            double t=step/5d;
            double noise=Math.sin(seed*13.71+frame*5.31+step*7.63)*.12*Math.sin(t*Math.PI);
            Vec3 point=start.lerp(end,t).add(normal.scale(noise)).add(tangent.scale(noise*.7));
            ribbon(m,v,previous,point,normal,tangent,width,red,green,blue,.72f);
            ribbon(m,v,previous,point,normal,tangent,width*.32,1,.92f,.98f,.95f);previous=point;
        }
    }
    private static void ribbon(Matrix4f m,VertexConsumer v,Vec3 a,Vec3 b,Vec3 normal,Vec3 tangent,double width,float red,float green,float blue,float alpha){
        for(var axis:new Vec3[]{normal,tangent}){
            Vec3 side=axis.scale(width);Vec3[] points={a.subtract(side),b.subtract(side),b.add(side),a.add(side)};
            for(int reverse=0;reverse<2;reverse++)for(int corner=0;corner<4;corner++){
                Vec3 p=points[reverse==0?corner:3-corner];v.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(red,green,blue,alpha);
            }
        }
    }
}
