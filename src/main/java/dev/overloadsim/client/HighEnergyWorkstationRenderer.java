package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.ReceiverShellMesh;
import dev.overloadsim.core.WorkstationVisualMesh;
import dev.overloadsim.core.WorkstationRenderLighting;
import dev.overloadsim.core.WorkstationOptics;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    private final Map<HighEnergyWorkstationBlockEntity,Lighting> lighting=new WeakHashMap<>();
    private final PreparedMachineMesh station=PreparedMachineMesh.surfaces(WorkstationVisualMesh.surfaces());
    private final PreparedMachineMesh shell=PreparedMachineMesh.surfaces(WorkstationVisualMesh.shellSurfaces());
    private final PreparedMachineMesh panes=PreparedMachineMesh.panes(WorkstationVisualMesh.glass());
    private record Lighting(BlockPos center,int turns,PreparedSurfaceLighting samples) {}
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
        if(!ClientRenderVisibility.visible(getRenderBoundingBox(host)))return;
        var centre=host.center().getCenter().subtract(Vec3.atLowerCornerOf(host.getBlockPos()));
        poses.pushPose();
        poses.translate(centre.x,centre.y,centre.z);
        // Local +Z is inward from the controller, matching the authoritative construction template.
        float rotation=switch(host.facing()){case NORTH->0;case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        poses.mulPose(Axis.YP.rotationDegrees(rotation));
        int turns=switch(host.facing()){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        var origin=host.center().getCenter();
        var cached=lighting.get(host);
        if(cached==null||!cached.center.equals(host.center())||cached.turns!=turns) {
            var samples=new PreparedSurfaceLighting(p -> WorkstationRenderLighting.world(p.x(),p.y(),p.z(),
                    origin.x,origin.y,origin.z,turns),false);
            cached=new Lighting(host.center(),turns,samples);lighting.put(host,cached);
        }
        var frame=cached.samples.beginFrame(host.getLevel(),light);
        ToIntFunction<WorkstationRenderLighting.Point> lightAt=frame::lightAt;
        ToIntFunction<ReceiverShellMesh.Quad> faceLight=frame;
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/workstation_shell_atlas"));
        var solid=buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        if(host.workstationVisualsVisible()) {
            shell.draw(poses.last(),solid,solid,atlas,faceLight);
            var glass=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            panes.draw(poses.last(),glass,glass,atlas,faceLight);
        }
        // Reacquire after the translucent pass; shader wrappers may switch their current segment.
        solid=buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        station.draw(poses.last(),solid,solid,atlas,faceLight);
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}) {
            // NORTH's native model rests on z=1. Its scaled bottom is exactly the rear wall's inner z=2.5.
            poses.pushPose();poses.translate(sx*1.125-.375,sy*1.125-.375,1.75);
            poses.scale(.75f,.75f,.75f);
            context.getBlockRenderDispatcher().renderSingleBlock(crystal,poses,buffers,LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            poses.popPose();
        }
        if(host.currentPower()>host.laserThreshold()){
            // The existing fixed translucent beacon pass follows opaque geometry and writes colour only.
            var beam=buffers.getBuffer(RenderType.beaconBeam(InventoryMenu.BLOCK_ATLAS,true));
            for(var ray:WorkstationOptics.rays()) {
                var a=ray.start();var b=ray.end();
                LaserEnergyEffects.pinkBeam(poses.last(),beam,atlas,new Vec3(a.x(),a.y(),a.z()),
                        new Vec3(b.x(),b.y(),b.z()));
            }
        }
        for(int slot=0;slot<9;slot++) {
            var item=host.inputs().prototype(slot);if(item.isEmpty())continue;
            poses.pushPose();poses.translate((slot%3-1)*.8125,-1.87,(slot/3-1)*.8125);
            poses.scale(.36f,.36f,.36f);
            int itemLight=lightAt.applyAsInt(WorkstationRenderLighting.world((slot%3-1)*.8125,-1.8,
                    (slot/3-1)*.8125,origin.x,origin.y,origin.z,turns));
            context.getItemRenderer().renderStatic(item,ItemDisplayContext.GROUND,itemLight,OverlayTexture.NO_OVERLAY,
                    poses,buffers,host.getLevel(),slot);
            poses.popPose();
        }
        poses.popPose();
    }

}
