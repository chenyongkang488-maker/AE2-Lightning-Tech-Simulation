package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overloadsim.ModContent;
import dev.overloadsim.core.ReceiverShellMesh;
import dev.overloadsim.core.IndustrialMachineMesh;
import dev.overloadsim.core.EmitterReactorMesh;
import dev.overloadsim.core.GateMatrixMesh;
import dev.overloadsim.core.DeflectorShellMesh;
import dev.overloadsim.core.ReceiverPedestalMesh;
import dev.overloadsim.core.ReceiverParticleVisuals;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserSegment;
import dev.overloadsim.laser.LaserStructureGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Bounded cosmetic geometry only: neither beams nor particles create world entities. */
public final class LaserRenderer implements BlockEntityRenderer<LaserMachineBlockEntity> {
    private static final int SIDES = 12, MAX_SEGMENTS = 128, MAX_DOTS = 128;
    private final BlockEntityRendererProvider.Context context;
    private final Map<LaserMachineBlockEntity,Skin> skins = new WeakHashMap<>();
    private record Basis(Vec3 right,Vec3 up,Vec3 forward) {
        Vec3 apply(double x,double y,double z) { return right.scale(x).add(up.scale(y)).add(forward.scale(z)); }
        Vec3 local(Vec3 world) { return new Vec3(world.dot(right),world.dot(up),world.dot(forward)); }
    }
    private record Skin(Direction facing,boolean pedestal,long tick,List<BlockPos> openings,
            List<IndustrialMachineMesh.Surface> mesh,List<ReceiverShellMesh.Quad> glass) {}

    public LaserRenderer(BlockEntityRendererProvider.Context context) { this.context = context; }
    @Override public boolean shouldRenderOffScreen(LaserMachineBlockEntity be) { return be.formed(); }
    @Override public int getViewDistance() { return 128; }

    @Override public AABB getRenderBoundingBox(LaserMachineBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        if (be.formed()) {
            Vec3 center=Vec3.atCenterOf(be.center());
            double radius=be.kind()==LaserKind.CORE?1.7:be.kind()==LaserKind.EMITTER?2.85:
                    be.kind()==LaserKind.CONSTRAINT||be.kind()==LaserKind.FOCUS?4.2:3.65;
            box=box.minmax(new AABB(center,center).inflate(radius));
            if(be.kind()==LaserKind.RECEIVER&&be.receiverPedestalLayout()) {
                var basis=basis(be);
                for(int x:new int[]{-1,1})for(int y:new int[]{-1,1})for(int z:new int[]{-1,1}) {
                    Vec3 corner=center.add(basis.apply(x*3.6,y<0?-5.6:-3.4,z*3.6));
                    box=box.minmax(new AABB(corner,corner));
                }
            }
        }
        int count = 0;
        for (LaserSegment segment : be.segments()) {
            if (++count > MAX_SEGMENTS) break;
            if (valid(segment)) box = box.minmax(segment.bounds());
        }
        return box;
    }

    @Override public void render(LaserMachineBlockEntity be, float partial, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null || !be.formed()) return;
        double time = be.getLevel().getGameTime() + partial;
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        Vec3 center = Vec3.atCenterOf(be.center()).subtract(origin);
        if (be.receiverSkinVisible()) machineSkin(be,center,poses,buffers,light);
        if (be.kind()==LaserKind.CORE) industrialMesh(IndustrialMachineMesh.coreAttachments(),poses.last(),
                buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS)),atlas(),center,light,basis(be));
        boolean active = be.activeUntil() > be.getLevel().getGameTime();
        if (be.kind() == LaserKind.CORE && active) LaserEnergyEffects.core(context,center,time,be.collapseStartTick(),poses,buffers);
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poses.last().pose();
        if (be.kind() == LaserKind.RECEIVER)
            containment(matrix, vertices, center, time, be.particles().stored(), be.particles().capacity());
        if (!active) return;
        if(be.kind()==LaserKind.BURST&&be.lastPower()>0){
            var b=basis(be);LaserEnergyEffects.burst(matrix,vertices,center,b.right,b.up,b.forward,time,be.getBlockPos().asLong());
        }
        var energySprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/simulation_energy_orb"));
        var energyVertices=buffers.getBuffer(RenderType.beaconBeam(InventoryMenu.BLOCK_ATLAS,true));
        int count = 0, dotBudget = MAX_DOTS;
        for (LaserSegment segment : be.segments()) {
            if (++count > MAX_SEGMENTS) break;
            if (!valid(segment)) continue;
            Vec3 start = segment.start().subtract(origin), end = segment.end().subtract(origin);
            if (segment.particle()) {
                int dots = Math.min(12, dotBudget); dotBudget -= dots;
                for (int dot = 0; dot < dots; dot++) {
                    double t = (time * .065 + dot / (double) Math.max(1, dots)) % 1;
                    Vec3 point = start.lerp(end, t);
                    tube(matrix, vertices, point.add(0, -.028, 0), point.add(0, .028, 0),
                            .035, .035, 1, .65f, .85f, .85f);
                }
            } else {
                double startRadius = segment.startRadius(), endRadius = segment.endRadius();
                LaserEnergyEffects.beam(poses.last(),energyVertices,energySprite,start,end,startRadius,endRadius,.38f);
                LaserEnergyEffects.beam(poses.last(),energyVertices,energySprite,start,end,startRadius*.375,endRadius*.375,.9f);
            }
        }
    }

    private static boolean valid(LaserSegment segment) {
        Vec3 start = segment.start(), end = segment.end();
        return finite(start) && finite(end) && start.distanceToSqr(end) > .0001
                && start.distanceToSqr(end) <= 128 * 128;
    }

    private static TextureAtlasSprite atlas() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/laser_pixel_atlas"));
    }

    private static Basis basis(LaserMachineBlockEntity be) {
        Direction forward=be.receiverVisualFacing().getOpposite();
        Vec3 f=Vec3.atLowerCornerOf(forward.getNormal()),up=Vec3.atLowerCornerOf(LaserStructureGeometry.basisUp(forward).getNormal());
        return new Basis(up.cross(f),up,f);
    }

    private void machineSkin(LaserMachineBlockEntity be,Vec3 center,PoseStack poses,MultiBufferSource buffers,int light) {
        var sprite = atlas();
        var porcelain = buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        var basis=basis(be);
        var skin=skin(be,basis);
        var emissive = buffers.getBuffer(RenderType.beaconBeam(InventoryMenu.BLOCK_ATLAS,false));
        industrialMesh(skin.mesh,poses.last(),porcelain,sprite,center,light,basis,emissive);
        if(be.kind()==LaserKind.RECEIVER) {
            // Keep the established fixed translucent buffer; never flush it while opaque atlas geometry is queued.
            var glass=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            shellMesh(skin.glass,poses.last(),glass,sprite,center,light,basis,66.5f,2.5f,11,11);
            if(be.receiverPedestalLayout()) {
                var screen=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ModContent.id("block/simulation_controller_ecg"));
                shellMesh(List.of(ReceiverPedestalMesh.screen()),poses.last(),emissive,screen,center,LightTexture.FULL_BRIGHT,basis,0,0,128,128);
            }
        }
    }

    private Skin skin(LaserMachineBlockEntity be,Basis basis) {
        long tick=be.getLevel().getGameTime();var facing=be.receiverVisualFacing();boolean pedestal=be.receiverPedestalLayout();
        var previous=skins.get(be);
        if(previous!=null&&previous.tick==tick&&previous.facing==facing&&previous.pedestal==pedestal)return previous;
        var openings=new ArrayList<BlockPos>();
        for(var entry:be.structureTemplate().entrySet()) {
            if(entry.getValue()!=LaserStructureGeometry.Part.AIR&&entry.getValue()!=LaserStructureGeometry.Part.FRAME
                    &&entry.getValue()!=LaserStructureGeometry.Part.GLASS) {openings.add(entry.getKey());continue;}
            if(entry.getValue()!=LaserStructureGeometry.Part.FRAME)continue;
            var pos=be.getBlockPos().offset(entry.getKey());
            if(be.getLevel().getBlockState(pos).getBlock()!=LaserContent.FRAME.get())openings.add(entry.getKey());
        }
        if(previous!=null&&previous.facing==facing&&previous.pedestal==pedestal&&previous.openings.equals(openings)) {
            var next=new Skin(facing,pedestal,tick,previous.openings,previous.mesh,previous.glass);
            skins.put(be,next);return next;
        }
        var result=new ArrayList<IndustrialMachineMesh.Surface>();
        List<ReceiverShellMesh.Quad> panes=List.of();
        var centerOffset=Vec3.atLowerCornerOf(be.center().subtract(be.getBlockPos()));
        if(be.kind()==LaserKind.RECEIVER) {
            for(int axis=0;axis<3;axis++)result.addAll(IndustrialMachineMesh.armouredRing(axis,3.3));
            var glassMesh=new ArrayList<IndustrialMachineMesh.Surface>();
            for(var quad:ReceiverShellMesh.glass())glassMesh.add(new IndustrialMachineMesh.Surface(quad,4,false));
            for(var opening:openings) {
                var local=basis.local(Vec3.atLowerCornerOf(opening).subtract(centerOffset));
                result=new ArrayList<>(IndustrialMachineMesh.withoutCell(result,local.x,local.y,local.z));
                glassMesh=new ArrayList<>(IndustrialMachineMesh.withoutCell(glassMesh,local.x,local.y,local.z));
            }
            panes=glassMesh.stream().map(IndustrialMachineMesh.Surface::quad).toList();
            if(pedestal) {
                var plinth=ReceiverPedestalMesh.surfaces();
                for(var opening:openings) {
                    if(opening.equals(BlockPos.ZERO))continue;
                    var local=basis.local(Vec3.atLowerCornerOf(opening).subtract(centerOffset));
                    if(local.y<=-3.5)plinth=IndustrialMachineMesh.withoutCell(plinth,local.x,local.y,local.z);
                }
                result.addAll(plinth);
            }
        } else if(be.kind()==LaserKind.CONSTRAINT||be.kind()==LaserKind.ENHANCEMENT
                ||be.kind()==LaserKind.BURST||be.kind()==LaserKind.FOCUS) {
            result.addAll(IndustrialMachineMesh.armouredRing(2,3.05));
            if(be.kind()==LaserKind.CONSTRAINT||be.kind()==LaserKind.FOCUS)result.addAll(GateMatrixMesh.outer(be.kind()==LaserKind.FOCUS));
            for(var opening:openings) {
                var local=basis.local(Vec3.atLowerCornerOf(opening).subtract(centerOffset));
                result=new ArrayList<>(IndustrialMachineMesh.withoutCell(result,local.x,local.y,local.z));
            }
        } else if(be.kind()==LaserKind.EMITTER) {
            result.addAll(EmitterReactorMesh.surfaces());
            for(var opening:openings) {
                var local=basis.local(Vec3.atLowerCornerOf(opening).subtract(centerOffset));
                // The central source and front optical port are enclosed by the reactor.
                // Real rear controller and optional upstream network interfaces stay exposed.
                if(Math.abs(local.x)<.01&&Math.abs(local.y)<.01&&local.z>=0)continue;
                result=new ArrayList<>(IndustrialMachineMesh.withoutCell(result,local.x,local.y,local.z));
            }
        } else if(be.kind()==LaserKind.DEFLECTOR) {
            result.addAll(DeflectorShellMesh.surfaces());
            for(var opening:openings) {
                var local=basis.local(Vec3.atLowerCornerOf(opening).subtract(centerOffset));
                result=new ArrayList<>(IndustrialMachineMesh.withoutCell(result,local.x,local.y,local.z));
            }
        }
        var next=new Skin(facing,pedestal,tick,List.copyOf(openings),List.copyOf(result),panes);
        skins.put(be,next);return next;
    }

    private static void industrialMesh(List<IndustrialMachineMesh.Surface> mesh,PoseStack.Pose pose,VertexConsumer vertices,
            TextureAtlasSprite sprite,Vec3 center,int light,Basis basis) {
        industrialMesh(mesh,pose,vertices,sprite,center,light,basis,vertices);
    }

    private static void industrialMesh(List<IndustrialMachineMesh.Surface> mesh,PoseStack.Pose pose,VertexConsumer vertices,
            TextureAtlasSprite sprite,Vec3 center,int light,Basis basis,VertexConsumer emissive) {
        for(var surface:mesh) {
            float u=(surface.tile()%8)*16+.5f,v=(surface.tile()/8)*16+.5f;
            var q=surface.quad();int panelLight=surface.glow()?LightTexture.FULL_BRIGHT:light;
            // Each surface is drawn once; the opaque beacon shader keeps lights
            // unshaded while establishing depth before translucent port glass.
            var target=surface.glow()?emissive:vertices;
            shellVertex(q.a(),pose,target,sprite,center,panelLight,basis,u,v,15,15);
            shellVertex(q.b(),pose,target,sprite,center,panelLight,basis,u,v,15,15);
            shellVertex(q.c(),pose,target,sprite,center,panelLight,basis,u,v,15,15);
            shellVertex(q.d(),pose,target,sprite,center,panelLight,basis,u,v,15,15);
        }
    }

    private static void shellMesh(java.util.List<ReceiverShellMesh.Quad> mesh, PoseStack.Pose pose, VertexConsumer vertices,
            TextureAtlasSprite sprite, Vec3 center, int light,Basis basis, float u, float v, float width, float height) {
        for (var quad : mesh) {
            shellVertex(quad.a(), pose, vertices, sprite, center, light,basis, u, v, width, height);
            shellVertex(quad.b(), pose, vertices, sprite, center, light,basis, u, v, width, height);
            shellVertex(quad.c(), pose, vertices, sprite, center, light,basis, u, v, width, height);
            shellVertex(quad.d(), pose, vertices, sprite, center, light,basis, u, v, width, height);
        }
    }

    private static void shellVertex(ReceiverShellMesh.Vertex vertex, PoseStack.Pose pose, VertexConsumer vertices,
            TextureAtlasSprite sprite, Vec3 center, int light,Basis basis, float u, float v, float width, float height) {
        double x=vertex.x(),y=vertex.y(),z=vertex.z(),nx=vertex.nx(),ny=vertex.ny(),nz=vertex.nz();
        // Basis arithmetic stays allocation-free for the cached mesh's thousands of vertices.
        vertices.addVertex(pose.pose(),
                (float)(center.x+basis.right.x*x+basis.up.x*y+basis.forward.x*z),
                (float)(center.y+basis.right.y*x+basis.up.y*y+basis.forward.y*z),
                (float)(center.z+basis.right.z*x+basis.up.z*y+basis.forward.z*z))
                .setColor(255, 255, 255, 255).setUv(sprite.getU((float) (u + vertex.u() * width) / 128), sprite.getV((float) (v + vertex.v() * height) / 128))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,
                        (float)(basis.right.x*nx+basis.up.x*ny+basis.forward.x*nz),
                        (float)(basis.right.y*nx+basis.up.y*ny+basis.forward.y*nz),
                        (float)(basis.right.z*nx+basis.up.z*ny+basis.forward.z*nz));
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static void containment(Matrix4f m,VertexConsumer v,Vec3 center,double time,long stored,long capacity) {
        int count=ReceiverParticleVisuals.count(stored,capacity);
        for(int index=0;index<count;index++) {
            Vec3 point=particlePoint(index,time,center),trail=particlePoint(index,time-1.5,center),tail=particlePoint(index,time-3,center);
            spark(m,v,point,.027,1,.93f,.98f,1);
            particleRibbon(m,v,trail,point,.016f,.9f);
            particleRibbon(m,v,tail,trail,.009f,.38f);
        }
    }
    private static Vec3 particlePoint(int index,double time,Vec3 center) {
        var p=ReceiverParticleVisuals.position(index,time);return center.add(p.x(),p.y(),p.z());
    }
    private static void spark(Matrix4f m,VertexConsumer vertices,Vec3 point,double radius,float r,float g,float b,float alpha) {
        for(int axis=0;axis<3;axis++)for(int reverse=0;reverse<2;reverse++)for(int corner=0;corner<4;corner++) {
            int i=reverse==0?corner:3-corner;double a=i==0||i==3?-radius:radius,c=i<2?-radius:radius;
            Vec3 offset=axis==0?new Vec3(0,a,c):axis==1?new Vec3(a,0,c):new Vec3(a,c,0);
            var p=point.add(offset);vertices.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,alpha);
        }
    }
    private static void particleRibbon(Matrix4f m,VertexConsumer vertices,Vec3 start,Vec3 end,float width,float alpha) {
        for(int axis=0;axis<2;axis++)for(int reverse=0;reverse<2;reverse++)for(int corner=0;corner<4;corner++) {
            int i=reverse==0?corner:3-corner;Vec3 p=i==0||i==3?start:end;
            double offset=i<2?-width:width;p=p.add(axis==0?offset:0,axis==1?offset:0,0);
            vertices.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(1,.55f,.78f,alpha);
        }
    }

    /** A 12-sided round cylinder, or a cone when its endpoint radius is smaller. */
    private static void tube(Matrix4f m, VertexConsumer v, Vec3 start, Vec3 end,
            double startRadius, double endRadius, float r, float g, float b, float alpha) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 normal = direction.cross(Math.abs(direction.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
        Vec3 tangent = direction.cross(normal).normalize();
        for (int i = 0; i < SIDES; i++) {
            double a = i * Math.PI * 2 / SIDES, next = (i + 1) * Math.PI * 2 / SIDES;
            Vec3 u = normal.scale(Math.cos(a)).add(tangent.scale(Math.sin(a)));
            Vec3 w = normal.scale(Math.cos(next)).add(tangent.scale(Math.sin(next)));
            Vec3[] points = {start.add(u.scale(startRadius)), end.add(u.scale(endRadius)),
                    end.add(w.scale(endRadius)), start.add(w.scale(startRadius))};
            // Draw both windings so the tube remains visible from inside its bounds.
            for (int reverse = 0; reverse < 2; reverse++) for (int j = 0; j < 4; j++) {
                Vec3 point = points[reverse == 0 ? j : 3 - j];
                v.addVertex(m, (float) point.x, (float) point.y, (float) point.z).setColor(r, g, b, alpha);
            }
        }
    }
}
