package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overloadsim.ModContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Bounded cosmetic geometry only: neither beams nor particles create world entities. */
public final class LaserRenderer implements BlockEntityRenderer<LaserMachineBlockEntity> {
    private static final int SIDES = 12, MAX_SEGMENTS = 128, MAX_DOTS = 128;
    private final BlockEntityRendererProvider.Context context;

    public LaserRenderer(BlockEntityRendererProvider.Context context) { this.context = context; }
    @Override public boolean shouldRenderOffScreen(LaserMachineBlockEntity be) { return be.formed(); }
    @Override public int getViewDistance() { return 128; }

    @Override public AABB getRenderBoundingBox(LaserMachineBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        if (be.formed()) box = box.minmax(new AABB(be.center()).inflate(1.6));
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
        if (be.kind() == LaserKind.EMITTER) crystal(be, center, time, poses, buffers);
        boolean active = be.activeUntil() > be.getLevel().getGameTime();
        if (be.kind() == LaserKind.CORE && active) orb(center, time, poses, buffers);
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poses.last().pose();
        if (be.kind() == LaserKind.RECEIVER && (active || be.particles().stored() > 0))
            containment(matrix, vertices, center, time);
        if (!active) return;
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
                            .035, .035, .4f, .8f, 1, .85f);
                }
            } else {
                double startRadius = segment.startRadius(), endRadius = segment.endRadius();
                tube(matrix, vertices, start, end, startRadius, endRadius, 1, .35f, .73f, .32f);
                tube(matrix, vertices, start, end, startRadius * .375, endRadius * .375,
                        1, .9f, .98f, .65f);
            }
        }
    }

    private static boolean valid(LaserSegment segment) {
        Vec3 start = segment.start(), end = segment.end();
        return finite(start) && finite(end) && start.distanceToSqr(end) > .0001
                && start.distanceToSqr(end) <= 128 * 128;
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private void crystal(LaserMachineBlockEntity be, Vec3 center, double time,
            PoseStack poses, MultiBufferSource buffers) {
        poses.pushPose();
        poses.translate(center.x, center.y + Math.sin(time * .04) * .06, center.z);
        poses.mulPose(Axis.YP.rotationDegrees((float) (time * 1.2)));
        poses.scale(1.3f, 1.3f, 1.3f);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModContent.PERFECT.get()),
                ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poses, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
        poses.popPose();
    }

    private void orb(Vec3 center, double time, PoseStack poses, MultiBufferSource buffers) {
        var model = Minecraft.getInstance().getModelManager().getModel(
                ModelResourceLocation.standalone(ModContent.id("block/simulation_energy_orb")));
        poses.pushPose();
        poses.translate(center.x, center.y, center.z);
        poses.mulPose(Axis.YP.rotationDegrees((float) (time * .8)));
        poses.scale(4, 4, 4); // Existing 12/16 block voxel sphere becomes three blocks across.
        poses.translate(-.5, -.5, -.5);
        context.getBlockRenderDispatcher().getModelRenderer().renderModel(poses.last(),
                buffers.getBuffer(RenderType.cutout()), null, model, 1, 1, 1,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poses.popPose();
    }

    private static void containment(Matrix4f m, VertexConsumer v, Vec3 center, double time) {
        for (int axis = 0; axis < 3; axis++) for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12 + time * .01, b = a + Math.PI / 12;
            Vec3 start = ring(axis, a).scale(1.2).add(center), end = ring(axis, b).scale(1.2).add(center);
            tube(m, v, start, end, .018, .018, .45f, .8f, 1, .48f);
        }
    }

    private static Vec3 ring(int axis, double a) {
        return switch (axis) {
            case 0 -> new Vec3(0, Math.cos(a), Math.sin(a));
            case 1 -> new Vec3(Math.cos(a), 0, Math.sin(a));
            default -> new Vec3(Math.cos(a), Math.sin(a), 0);
        };
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
