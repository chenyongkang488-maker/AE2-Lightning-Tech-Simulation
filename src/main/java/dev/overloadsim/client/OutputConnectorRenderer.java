/* SPDX-License-Identifier: LGPL-3.0-only
 * Overlay geometry adapted from AE2 Lightning Tech WirelessConnectorRenderer.
 * Copyright AE2 Lightning Tech contributors; addon routing/colors qiqi.
 */
package dev.overloadsim.client;

import appeng.client.render.overlay.OverlayRenderType;
import com.moakiee.ae2lt.client.Ae2ltRenderTypes;
import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import com.moakiee.ae2lt.logic.WirelessConnectorTargetHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.output.*;
import dev.overloadsim.output.tool.OutputConnector;
import dev.overloadsim.output.tool.OutputSourceSelection;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Same held-tool host/face/line overlays as the original, with a stable color for every source slot. */
@EventBusSubscriber(modid=OverloadSimulation.ID,value=Dist.CLIENT)
public final class OutputConnectorRenderer {
    private static final int SCAN_RANGE=64, RESCAN_TICKS=4;
    private static final List<BlockPos> HOSTS=new ArrayList<>();
    private static final Map<BlockPos,Snapshot> SNAPSHOTS=new HashMap<>();
    private static final Quaternionf ROTATION=new Quaternionf();
    private static ClientLevel cachedLevel;
    private static long lastScan=Long.MIN_VALUE, lastPreview=Long.MIN_VALUE;
    private static BlockPos previewPosition;
    private static boolean previewBatch;
    private static List<BlockPos> previewTargets=List.of();
    private record Face(BlockPos pos,Direction face) {}
    private record Snapshot(OutputInterfaceBlockEntity host,int revision,List<OutputConnection> links,
                            Map<Face,Set<Integer>> faceSlots) {}
    private OutputConnectorRenderer() {}

    private static ItemStack heldTool() {
        var player=Minecraft.getInstance().player;
        if(player!=null)for(var hand:InteractionHand.values()) {
            var stack=player.getItemInHand(hand);
            if(OutputConnector.isTool(stack))return stack;
        }
        return ItemStack.EMPTY;
    }
    public static boolean hasOutputSelection() {
        var tool=heldTool();
        return !tool.isEmpty()&&OutputSourceSelection.read(tool)!=null;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return;
        var tool=heldTool();
        if(tool.isEmpty())return;
        var selected=OutputSourceSelection.read(tool);
        if(selected==null&&OverloadedWirelessConnectorItem.hasSelection(tool))return;
        if(cachedLevel!=mc.level) {clear();cachedLevel=mc.level;}
        if(selected!=null&&!selected.dimension().equals(mc.level.dimension()))return;
        long now=mc.level.getGameTime();
        if(selected==null&&(lastScan==Long.MIN_VALUE||now<lastScan||now-lastScan>=RESCAN_TICKS)) {
            scan(mc.level,mc.player.blockPosition());lastScan=now;
        }
        var pose=event.getPoseStack();
        var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();
        try {
            ROTATION.set(event.getCamera().rotation()).invert();
            pose.mulPose(ROTATION);
            var camera=event.getCamera().getPosition();
            pose.translate(-camera.x,-camera.y,-camera.z);
            if(selected!=null) {
                var host=host(mc.level,selected.pos());
                if(host!=null) {
                    renderHost(pose,buffers,mc.level,host,true);
                    renderPreview(pose,buffers,mc,host,selected.sourceSlot(),now);
                }
            } else for(var pos:HOSTS) {
                var host=host(mc.level,pos);
                if(host!=null)renderHost(pose,buffers,mc.level,host,false);
            }
        } finally {pose.popPose();}
        buffers.endBatch(Ae2ltRenderTypes.getFaceSeeThrough());
        buffers.endBatch(OverlayRenderType.getBlockHilightFace());
        buffers.endBatch(OverlayRenderType.getBlockHilightLine());
    }
    private static OutputInterfaceBlockEntity host(ClientLevel level,BlockPos pos) {
        if(!level.isLoaded(pos))return null;
        return level.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity host
                &&host.getInterfaceMode()==OutputInterfaceBlockEntity.InterfaceMode.WIRELESS?host:null;
    }
    private static void scan(ClientLevel level,BlockPos player) {
        HOSTS.clear();
        for(int x=(player.getX()-SCAN_RANGE)>>4;x<=(player.getX()+SCAN_RANGE)>>4;x++)
            for(int z=(player.getZ()-SCAN_RANGE)>>4;z<=(player.getZ()+SCAN_RANGE)>>4;z++) {
                if(!level.hasChunk(x,z))continue;
                var chunk=level.getChunk(x,z);
                for(var pos:chunk.getBlockEntitiesPos())
                    if(chunk.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity)HOSTS.add(pos.immutable());
            }
        SNAPSHOTS.keySet().retainAll(HOSTS);
    }
    private static Snapshot snapshot(OutputInterfaceBlockEntity host,ClientLevel level) {
        var previous=SNAPSHOTS.get(host.getBlockPos());
        if(previous!=null&&previous.host()==host&&previous.revision()==host.getRenderRevision())return previous;
        var links=host.getRenderConnections().stream().filter(c->c.dimension().equals(level.dimension())).toList();
        var slots=new HashMap<Face,Set<Integer>>();
        for(var link:links)slots.computeIfAbsent(new Face(link.pos(),link.face()),unused->new HashSet<>()).add(link.sourceSlot());
        var result=new Snapshot(host,host.getRenderRevision(),links,slots);
        SNAPSHOTS.put(host.getBlockPos(),result);return result;
    }
    private static void renderHost(PoseStack pose,MultiBufferSource buffers,ClientLevel level,
                                   OutputInterfaceBlockEntity host,boolean selected) {
        renderInnerCube(pose,buffers,host.getBlockPos(),selected?0x80FFFF00:0x800080FF);
        var state=snapshot(host,level);
        for(var link:state.links()) {
            boolean shared=state.faceSlots().get(new Face(link.pos(),link.face())).size()>1;
            renderSlotFace(pose,buffers,link.pos(),link.face(),link.sourceSlot(),shared,0x60);
            renderSlotLine(pose,buffers,host.getBlockPos(),link.pos(),link.face(),link.sourceSlot(),shared,0xC0);
        }
    }
    private static void renderPreview(PoseStack pose,MultiBufferSource buffers,Minecraft mc,
                                      OutputInterfaceBlockEntity host,int slot,long now) {
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK
                ||hit.getBlockPos().equals(host.getBlockPos())||!mc.level.isLoaded(hit.getBlockPos())
                ||mc.level.getBlockEntity(hit.getBlockPos())==null)return;
        boolean batch=Screen.hasControlDown();
        if(!hit.getBlockPos().equals(previewPosition)||batch!=previewBatch||lastPreview==Long.MIN_VALUE
                ||now<lastPreview||now-lastPreview>=RESCAN_TICKS) {
            previewPosition=hit.getBlockPos().immutable();previewBatch=batch;lastPreview=now;
            previewTargets=List.copyOf(WirelessConnectorTargetHelper.collectTargets(mc.level,previewPosition,batch,
                    OutputInterfaceBlockEntity.MAX_CONNECTIONS));
        }
        var state=snapshot(host,mc.level);
        for(var pos:previewTargets) {
            if(pos.equals(host.getBlockPos())||!mc.level.isLoaded(pos))continue;
            var target=mc.level.getBlockEntity(pos);
            if(target==null||OutputConnector.originalHost(target)||target instanceof OutputInterfaceBlockEntity)continue;
            var slots=state.faceSlots().get(new Face(pos,hit.getDirection()));
            if(slots!=null&&slots.contains(slot))continue;
            boolean shared=slots!=null&&!slots.isEmpty();
            renderSlotFace(pose,buffers,pos,hit.getDirection(),slot,shared,0x40);
            renderSlotLine(pose,buffers,host.getBlockPos(),pos,hit.getDirection(),slot,shared,0x90);
        }
    }
    /** Shared faces get a six-by-six color patch grid so overlapping slot colors stay visible. */
    private static void renderSlotFace(PoseStack pose,MultiBufferSource buffers,BlockPos pos,
                                       Direction face,int slot,boolean shared,int alpha) {
        pose.pushPose();
        try {
            if(shared) {
                double u=(slot%6+.1)/6.0,v=(slot/6+.1)/6.0,scale=.8/6;
                pose.translate(pos.getX(),pos.getY(),pos.getZ());
                switch(face.getAxis()) {
                    case X->{pose.translate(0,v,u);pose.scale(1,(float)scale,(float)scale);}
                    case Y->{pose.translate(u,0,v);pose.scale((float)scale,1,(float)scale);}
                    case Z->{pose.translate(u,v,0);pose.scale((float)scale,(float)scale,1);}
                }
                renderFaceOverlay(pose,buffers,BlockPos.ZERO,face,OutputLinkColors.color(slot,alpha));
            } else renderFaceOverlay(pose,buffers,pos,face,OutputLinkColors.color(slot,alpha));
        } finally {pose.popPose();}
    }
    private static void renderSlotLine(PoseStack pose,MultiBufferSource buffers,BlockPos from,BlockPos to,
                                       Direction face,int slot,boolean shared,int alpha) {
        double u=shared?(slot%6+.5)/6.0-.5:0,v=shared?(slot/6+.5)/6.0-.5:0;
        var end=Vec3.atCenterOf(to).add(face.getStepX()*.501,face.getStepY()*.501,face.getStepZ()*.501);
        end=switch(face.getAxis()){case X->end.add(0,v,u);case Y->end.add(u,0,v);case Z->end.add(u,v,0);};
        var start=Vec3.atCenterOf(from);var delta=end.subtract(start);
        double length=delta.length();if(length<1e-6)return;
        var normal=delta.scale(1/length);
        var vc=buffers.getBuffer(OverlayRenderType.getBlockHilightLine());
        int color=OutputLinkColors.color(slot,alpha);
        var matrix=pose.last().pose();
        vc.addVertex(matrix,(float)start.x,(float)start.y,(float)start.z).setColor(color)
                .setNormal((float)normal.x,(float)normal.y,(float)normal.z);
        vc.addVertex(matrix,(float)end.x,(float)end.y,(float)end.z).setColor(color)
                .setNormal((float)normal.x,(float)normal.y,(float)normal.z);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    private static void clear() {
        HOSTS.clear();SNAPSHOTS.clear();cachedLevel=null;lastScan=Long.MIN_VALUE;lastPreview=Long.MIN_VALUE;
        previewPosition=null;previewTargets=List.of();
    }
    private static void renderInnerCube(PoseStack poseStack, MultiBufferSource buffer,
            BlockPos pos, int color) {
        VertexConsumer vc = buffer.getBuffer(Ae2ltRenderTypes.getFaceSeeThrough());
        int[] c = OverlayRenderType.decomposeColor(color);

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f mat = poseStack.last().pose();

        float lo = 0.25f, hi = 0.75f;

        // DOWN (Y-)
        quad(vc, mat, c, lo, lo, lo,  hi, lo, lo,  hi, lo, hi,  lo, lo, hi,  0, -1, 0);
        // UP (Y+)
        quad(vc, mat, c, lo, hi, hi,  hi, hi, hi,  hi, hi, lo,  lo, hi, lo,  0, 1, 0);
        // NORTH (Z-)
        quad(vc, mat, c, lo, lo, lo,  lo, hi, lo,  hi, hi, lo,  hi, lo, lo,  0, 0, -1);
        // SOUTH (Z+)
        quad(vc, mat, c, hi, lo, hi,  hi, hi, hi,  lo, hi, hi,  lo, lo, hi,  0, 0, 1);
        // WEST (X-)
        quad(vc, mat, c, lo, lo, hi,  lo, hi, hi,  lo, hi, lo,  lo, lo, lo,  -1, 0, 0);
        // EAST (X+)
        quad(vc, mat, c, hi, lo, lo,  hi, hi, lo,  hi, hi, hi,  hi, lo, hi,  1, 0, 0);

        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, int[] c,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz) {
        vc.addVertex(mat, x1, y1, z1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
    }


    /**
     * Render a single face overlay (quad) on the given block face.
     */
    private static void renderFaceOverlay(PoseStack poseStack, MultiBufferSource buffer,
            BlockPos pos, Direction face, int color) {
        VertexConsumer vc = buffer.getBuffer(OverlayRenderType.getBlockHilightFace());
        int[] c = OverlayRenderType.decomposeColor(color);

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f mat = poseStack.last().pose();

        // Slight offset to avoid z-fighting
        float offset = 0.001f;

        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();

        switch (face) {
            case DOWN -> {
                vc.addVertex(mat, 0, -offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, -offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, -offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, -offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case UP -> {
                vc.addVertex(mat, 0, 1 + offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1 + offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1 + offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1 + offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case NORTH -> {
                vc.addVertex(mat, 0, 0, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 0, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case SOUTH -> {
                vc.addVertex(mat, 1, 0, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 0, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case WEST -> {
                vc.addVertex(mat, -offset, 0, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 1, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 1, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 0, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case EAST -> {
                vc.addVertex(mat, 1 + offset, 0, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 1, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 1, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 0, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
        }

        poseStack.popPose();
    }


}
