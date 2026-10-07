package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.IndustrialMachineMesh;
import dev.overloadsim.core.ReceiverShellMesh;
import dev.overloadsim.core.WorkstationVisualMesh;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Merged receiving chamber, four native overload-crystal clusters and a nine-place processing table. */
public final class HighEnergyWorkstationRenderer implements BlockEntityRenderer<HighEnergyWorkstationBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    private final BlockState crystal;
    public HighEnergyWorkstationRenderer(BlockEntityRendererProvider.Context context) {
        this.context=context;
        crystal=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:overload_crystal_cluster"))
                .defaultBlockState().setValue(BlockStateProperties.FACING,Direction.NORTH);
    }
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
        if(host.workstationVisualsVisible()) {
            mesh(WorkstationVisualMesh.shellSurfaces(),poses.last(),buffers,atlas,light);
            glass(WorkstationVisualMesh.glass(),poses.last(),buffers,atlas,light);
        }
        mesh(WorkstationVisualMesh.surfaces(),poses.last(),buffers,atlas,light);
        var flow=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/laser_energy_flow"));
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}) {
            // NORTH's native model rests on z=1. Its scaled bottom is exactly the rear wall's inner z=2.5.
            poses.pushPose();poses.translate(sx*1.125-.375,sy*1.125-.375,1.75);
            poses.scale(.75f,.75f,.75f);
            context.getBlockRenderDispatcher().renderSingleBlock(crystal,poses,buffers,LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            poses.popPose();
        }
        if(host.currentPower()>HighEnergyWorkstationBlockEntity.LASER_THRESHOLD){
            var beam=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})
                LaserEnergyEffects.beam(poses.last(),beam,flow,new Vec3(sx*1.125,sy*1.125,1.84375),
                        new Vec3(sx*.8125,-1.9,sy*.8125),.06,.035,.8f);
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
        vertex(point,pose,consumer,atlas,u,v,15,15,light);
    }
    private static void glass(List<ReceiverShellMesh.Quad> panes,PoseStack.Pose pose,MultiBufferSource buffers,
            TextureAtlasSprite atlas,int light) {
        var consumer=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        for(var pane:panes)for(var point:List.of(pane.a(),pane.b(),pane.c(),pane.d()))
            vertex(point,pose,consumer,atlas,66.5f,2.5f,11,11,light);
    }
    private static void vertex(ReceiverShellMesh.Vertex point,PoseStack.Pose pose,VertexConsumer consumer,
            TextureAtlasSprite atlas,float u,float v,float width,float height,int light) {
        consumer.addVertex(pose.pose(),(float)point.x(),(float)point.y(),(float)point.z())
                .setColor(255,255,255,255).setUv(atlas.getU((float)(u+point.u()*width)/128),atlas.getV((float)(v+point.v()*height)/128))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose,(float)point.nx(),(float)point.ny(),(float)point.nz());
    }
}
