package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.DeflectorCrystalMesh;
import dev.overloadsim.core.IndustrialMachineMesh;
import dev.overloadsim.core.ReceiverShellMesh;
import dev.overloadsim.core.WorkstationVisualMesh;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One renderer for the receiving face, four prisms and a nine-place processing table. */
public final class HighEnergyWorkstationRenderer implements BlockEntityRenderer<HighEnergyWorkstationBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    public HighEnergyWorkstationRenderer(BlockEntityRendererProvider.Context context) { this.context=context; }
    @Override public boolean shouldRenderOffScreen(HighEnergyWorkstationBlockEntity host) { return host.formed(); }
    @Override public int getViewDistance() { return 96; }
    @Override public AABB getRenderBoundingBox(HighEnergyWorkstationBlockEntity host) {
        return new AABB(host.center()).inflate(3.8);
    }

    @Override public void render(HighEnergyWorkstationBlockEntity host,float partial,PoseStack poses,
            MultiBufferSource buffers,int light,int overlay) {
        if(host.getLevel()==null||!host.formed())return;
        var centre=host.center().getCenter().subtract(Vec3.atLowerCornerOf(host.getBlockPos()));
        poses.pushPose();
        poses.translate(centre.x,centre.y,centre.z);
        // Local +Z is inward from the controller, matching the authoritative construction template.
        float rotation=switch(host.facing()){case NORTH->0;case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        poses.mulPose(Axis.YP.rotationDegrees(rotation));
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/laser_pixel_atlas"));
        mesh(WorkstationVisualMesh.surfaces(),poses.last(),buffers,atlas,light);
        double time=host.getLevel().getGameTime()+partial;
        var flow=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/laser_energy_flow"));
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}) {
            var prism=new Vec3(sx*1.125,sy*1.125,2.125);
            poses.pushPose();poses.translate(prism.x,prism.y,prism.z);
            poses.mulPose(Axis.YP.rotationDegrees((float)(time*.4+sx*sy*30)));
            poses.scale(.45f,.45f,.45f);
            mesh(DeflectorCrystalMesh.surfaces(),poses.last(),buffers,atlas,LightTexture.FULL_BRIGHT);
            poses.popPose();
        }
        if(host.currentPower()>1_000_000_000L){
            var beam=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})
                LaserEnergyEffects.beam(poses.last(),beam,flow,new Vec3(sx*1.125,sy*1.125,2.125),
                        new Vec3(sx*.375,-1.9,sy*.375),.09,.06,.8f);
        }
        for(int slot=0;slot<9;slot++) {
            var item=host.inputs().prototype(slot);if(item.isEmpty())continue;
            poses.pushPose();poses.translate((slot%3-1)*.8125,-1.87,(slot/3-1)*.8125);
            poses.scale(.36f,.36f,.36f);
            context.getItemRenderer().renderStatic(item,ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,
                    poses,buffers,host.getLevel(),slot);
            poses.popPose();
        }
        poses.popPose();
    }

    private static void mesh(List<IndustrialMachineMesh.Surface> surfaces,PoseStack.Pose pose,
            MultiBufferSource buffers,TextureAtlasSprite atlas,int light) {
        var solid=buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        // Fixed entity buffers establish interior depth before vanilla translucent glass.
        // A transient beacon pass can otherwise hide every prism when viewed from outside.
        for(var surface:surfaces) {
            float u=(surface.tile()%8)*16+.5f,v=(surface.tile()/8)*16+.5f;
            int illumination=surface.glow()?LightTexture.FULL_BRIGHT:light;
            var consumer=solid;
            var q=surface.quad();
            vertex(q.a(),pose,consumer,atlas,u,v,illumination);
            vertex(q.b(),pose,consumer,atlas,u,v,illumination);
            vertex(q.c(),pose,consumer,atlas,u,v,illumination);
            vertex(q.d(),pose,consumer,atlas,u,v,illumination);
        }
    }
    private static void vertex(ReceiverShellMesh.Vertex point,PoseStack.Pose pose,VertexConsumer consumer,
            TextureAtlasSprite atlas,float u,float v,int light) {
        consumer.addVertex(pose.pose(),(float)point.x(),(float)point.y(),(float)point.z())
                .setColor(255,255,255,255).setUv(atlas.getU((float)(u+point.u()*15)/128),atlas.getV((float)(v+point.v()*15)/128))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose,(float)point.nx(),(float)point.ny(),(float)point.nz());
    }
}
