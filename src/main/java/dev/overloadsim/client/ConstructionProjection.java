package dev.overloadsim.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.construction.ConstructionProjectionGeometry;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserStructureGeometry;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.SimulationAutoConstruction;
import dev.overloadsim.multiblock.SimulationControllerBlock;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.SimulationMemberBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

/** One local construction guide. It only reads loaded client blocks; it never sends build requests. */
@EventBusSubscriber(modid = OverloadSimulation.ID, value = Dist.CLIENT)
public final class ConstructionProjection {
    private static final double MAX_DISTANCE_SQUARED = 64 * 64;
    private static final ResourceLocation INTERFACE = ResourceLocation.parse("ae2lt:overloaded_interface");
    private static final ResourceLocation VIBRANT_GLASS = ResourceLocation.parse("ae2:quartz_vibrant_glass");
    private static final TagKey<Block> FRAMES = TagKey.create(Registries.BLOCK, ModContent.id("simulation_frames"));
    private static final TagKey<Block> GLASS = TagKey.create(Registries.BLOCK, ModContent.id("simulation_glass"));
    private static final Color PINK = new Color(1, .38f, .76f);
    private static final Color CYAN = new Color(.30f, .86f, 1);
    private static final Color AMBER = new Color(1, .80f, .25f);
    private static final Color VIOLET = new Color(.77f, .53f, 1);
    private static final Color GREEN = new Color(.25f, 1, .48f);
    private static final Color RED = new Color(1, .22f, .27f);
    private static final RenderType OUTLINES = RenderType.create(
            "overload_sim_construction_projection_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1536, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false));
    private static final RenderType FILLS = RenderType.create(
            "overload_sim_construction_projection_fill", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false));

    private static Preview active;
    private static final Quaternionf CAMERA_ROTATION=new Quaternionf();

    private ConstructionProjection() {}

    private record Color(float red, float green, float blue) {}
    private record Cell(BlockPos position, BlockState expected, Color missing) {}
    private record DrawCell(BlockPos position, Color color, float opacity) {}

    private static final class Preview {
        final ClientLevel level;
        final BlockEntity host;
        final BlockState controllerState;
        final Direction laserFacing;
        final boolean receiverPedestal;
        final boolean deflectorLegacy;
        final int size;
        final List<Cell> cells;
        final List<BlockPos> chunkAnchors;
        List<DrawCell> drawing = List.of();

        Preview(ClientLevel level, BlockEntity host, int size, List<Cell> cells) {
            if (cells.size() > ConstructionProjectionGeometry.MAX_VISIBLE_CELLS)
                throw new IllegalArgumentException("projection size");
            this.level = level;
            this.host = host;
            controllerState = host.getBlockState();
            laserFacing = host instanceof LaserMachineBlockEntity laser ? laser.receiverVisualFacing() : null;
            receiverPedestal = host instanceof LaserMachineBlockEntity laser && laser.receiverPedestalLayout();
            deflectorLegacy = host instanceof LaserMachineBlockEntity laser && laser.deflectorLegacyLayout();
            this.size = size;
            this.cells = List.copyOf(cells);
            chunkAnchors = cells.stream().map(cell -> new BlockPos(cell.position().getX() & ~15,
                    host.getBlockPos().getY(), cell.position().getZ() & ~15)).distinct().toList();
        }
    }

    /** Returns whether this controller's projection is enabled after the local toggle. */
    public static boolean toggleLaser(LaserMachineBlockEntity host) {
        if (laserActive(host)) { active = null; return false; }
        var mc = Minecraft.getInstance();
        if (!canStart(mc, host) || host.formed()) return false;
        var cells = new ArrayList<Cell>();
        ConstructionProjectionGeometry.laserCells(host.getBlockPos(), host.structureTemplate()).forEach((pos, part) -> {
            var block = switch (part) {
                case FRAME -> LaserContent.FRAME.get();
                case GLASS -> LaserContent.GLASS.get();
                case SOURCE -> LaserContent.SOURCE.get();
                case PORT -> LaserContent.PORT.get();
                default -> throw new IllegalArgumentException("invisible projection cell");
            };
            var color = switch (part) { case GLASS -> CYAN; case SOURCE -> AMBER; case PORT -> VIOLET; default -> PINK; };
            cells.add(new Cell(pos, block.defaultBlockState(), color));
        });
        return start(new Preview(mc.level, host, 0, cells));
    }

    public static boolean laserActive(LaserMachineBlockEntity host) {
        validateActive();
        return active != null && host != null && active.host == host && active.size == 0;
    }

    /** A different selected chamber size replaces the previous preview. */
    public static boolean toggleChamber(SimulationControllerBlockEntity host, int size) {
        if (chamberActive(host, size)) { active = null; return false; }
        var mc = Minecraft.getInstance();
        if (size < 3 || size > 7 || !canStart(mc, host) || host.visualSize() > 0) return false;
        var cells = new ArrayList<Cell>();
        SimulationAutoConstruction.previewTemplate(host.getBlockPos(),
                host.getBlockState().getValue(SimulationControllerBlock.FACING), size).forEach((pos, expected) -> {
            if (!pos.equals(host.getBlockPos()) && !expected.isAir()) {
                var color = expected.is(MultiblockContent.FRAME.get()) ? PINK : CYAN;
                cells.add(new Cell(pos.immutable(), expected, color));
            }
        });
        return start(new Preview(mc.level, host, size, cells));
    }

    public static boolean chamberActive(SimulationControllerBlockEntity host, int size) {
        validateActive();
        return active != null && host != null && active.host == host && active.size == size;
    }

    public static boolean toggleWorkstation(dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity host) {
        validateActive();
        if(active!=null&&active.host==host){active=null;return false;}
        var mc=Minecraft.getInstance();
        if(!canStart(mc,host)||host.formed())return false;
        var cells=new ArrayList<Cell>();
        host.template().forEach((pos,state)->{
            if(!pos.equals(host.getBlockPos())&&!state.isAir()) {
                var color=state.is(LaserContent.GLASS.get())?CYAN :state.is(LaserContent.PORT.get())?VIOLET:PINK;
                cells.add(new Cell(pos.immutable(),state,color));
            }
        });
        return start(new Preview(mc.level,host,-1,cells));
    }

    private static boolean canStart(Minecraft mc, BlockEntity host) {
        return host != null && mc.level != null && mc.player != null && mc.player.isAlive()
                && host.getLevel() == mc.level && !host.isRemoved()
                && loaded(mc.level, host.getBlockPos()) && mc.level.getBlockEntity(host.getBlockPos()) == host
                && mc.player.distanceToSqr(host.getBlockPos().getCenter()) <= MAX_DISTANCE_SQUARED;
    }

    private static boolean loaded(ClientLevel level, BlockPos pos) {
        // ClientLevel.hasChunk always returns true; querying the client cache avoids its empty-chunk fallback.
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean start(Preview preview) {
        if (preview.cells.isEmpty() || !available(preview)) return false;
        active = preview;
        refresh(preview);
        return true;
    }

    private static boolean available(Preview preview) {
        var mc = Minecraft.getInstance();
        if (!canStart(mc, preview.host) || mc.level != preview.level
                || !preview.host.getBlockState().equals(preview.controllerState)) return false;
        if (preview.host instanceof LaserMachineBlockEntity laser && (laser.formed()
                || laser.receiverVisualFacing() != preview.laserFacing
                || laser.receiverPedestalLayout() != preview.receiverPedestal
                || laser.deflectorLegacyLayout() != preview.deflectorLegacy)) return false;
        if (preview.host instanceof SimulationControllerBlockEntity chamber && chamber.visualSize() > 0) return false;
        if (preview.host instanceof dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity station && station.formed()) return false;
        for (var pos : preview.chunkAnchors) if (!loaded(preview.level, pos)) return false;
        return true;
    }

    private static void validateActive() {
        if (active != null && !available(active)) active = null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        validateActive();
        if (active != null) refresh(active);
    }

    private static void refresh(Preview preview) {
        // Optional ME interfaces obey the same one-port/placement rules as the structure validator.
        int interfaces = 0;
        for (var cell : preview.cells)
            if (BuiltInRegistries.BLOCK.getKey(preview.level.getBlockState(cell.position()).getBlock()).equals(INTERFACE))
                interfaces++;
        var drawing = new ArrayList<DrawCell>(preview.cells.size());
        for (var cell : preview.cells) {
            var current = preview.level.getBlockState(cell.position());
            boolean correct = matches(preview, cell, current, interfaces);
            boolean missing = !preview.level.isOutsideBuildHeight(cell.position())
                    && preview.level.getWorldBorder().isWithinBounds(cell.position())
                    && (current.isAir() || current.canBeReplaced()) && current.getFluidState().isEmpty()
                    && preview.level.getBlockEntity(cell.position()) == null;
            drawing.add(new DrawCell(cell.position(), correct ? GREEN : missing ? cell.missing() : RED,
                    correct ? .52f : missing ? .78f : .95f));
        }
        preview.drawing = List.copyOf(drawing);
    }

    private static boolean matches(Preview preview, Cell cell, BlockState current, int interfaces) {
        var memberEntity = preview.level.getBlockEntity(cell.position());
        if (memberEntity instanceof SimulationMemberBlockEntity member && member.owner() != null
                && !member.owner().equals(preview.host.getBlockPos())) return false;
        LaserStructureGeometry.Part receiverPart=null;
        if (preview.host instanceof LaserMachineBlockEntity laser
                && dev.overloadsim.laser.ReceiverOpticalPorts.compatible(laser,cell.position()))
            receiverPart=laser.structureTemplate().get(cell.position().subtract(laser.getBlockPos()));
        if(preview.host instanceof dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity)
            return dev.overloadsim.workstation.WorkstationStructure.matches(cell.expected(),current);
        if (BuiltInRegistries.BLOCK.getKey(current.getBlock()).equals(INTERFACE)) {
            if (interfaces != 1) return false;
            if (preview.host instanceof LaserMachineBlockEntity laser && (receiverPart==LaserStructureGeometry.Part.FRAME
                    || receiverPart==null&&cell.expected().is(LaserContent.FRAME.get()))) {
                var center = laser.getBlockPos().offset(LaserStructureGeometry.centerOffset(
                        laser.kind(), preview.laserFacing, preview.receiverPedestal, preview.deflectorLegacy));
                var relative = cell.position().subtract(center);
                var normal = preview.laserFacing.getOpposite().getNormal();
                return relative.getX() * normal.getX() + relative.getY() * normal.getY() + relative.getZ() * normal.getZ() <= 0;
            }
            if (preview.host instanceof SimulationControllerBlockEntity chamber && cell.expected().is(MultiblockContent.FRAME.get())) {
                var min = SimulationAutoConstruction.previewMin(chamber.getBlockPos(),
                        preview.controllerState.getValue(SimulationControllerBlock.FACING), preview.size);
                var relative = cell.position().subtract(min);
                boolean xEdge = relative.getX() == 0 || relative.getX() == preview.size - 1;
                boolean zEdge = relative.getZ() == 0 || relative.getZ() == preview.size - 1;
                return relative.getY() == 0 && xEdge != zEdge;
            }
            return false;
        }
        if(receiverPart!=null)return receiverPart==LaserStructureGeometry.Part.PORT?current.is(LaserContent.PORT.get())
                :receiverPart==LaserStructureGeometry.Part.FRAME&&current.is(LaserContent.FRAME.get());
        if (preview.host instanceof SimulationControllerBlockEntity chamber) {
            if (cell.expected().is(MultiblockContent.FRAME.get()))
                return current.is(MultiblockContent.FRAME.get()) || current.is(FRAMES);
            if (BuiltInRegistries.BLOCK.getKey(cell.expected().getBlock()).equals(VIBRANT_GLASS)) {
                if (current.is(MultiblockContent.GLASS.get()))
                    return memberEntity instanceof SimulationMemberBlockEntity member
                            && chamber.getBlockPos().equals(member.owner());
                return current.is(GLASS) || BuiltInRegistries.BLOCK.getKey(current.getBlock()).equals(VIBRANT_GLASS);
            }
        }
        return current.is(cell.expected().getBlock());
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        validateActive();
        if (active == null) return;
        var preview = active;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        try {
            // AFTER_LEVEL supplies an empty pose stack. Match the native wireless
            // guide's camera view rotation and draw after shader/particle passes.
            CAMERA_ROTATION.set(event.getCamera().rotation()).invert();
            pose.mulPose(CAMERA_ROTATION);
            var fill = buffers.getBuffer(FILLS);
            for (var cell : preview.drawing) {
                var p = cell.position();
                var color = cell.color();
                // Subtract the camera in double precision before vanilla writes float vertices.
                double x = p.getX() - camera.x, y = p.getY() - camera.y, z = p.getZ() - camera.z;
                LevelRenderer.addChainedFilledBoxVertices(pose, fill, x + .035, y + .035, z + .035,
                        x + .965, y + .965, z + .965,
                        color.red(), color.green(), color.blue(), cell.opacity() * .085f);
            }
            buffers.endBatch(FILLS);
            var lines = buffers.getBuffer(OUTLINES);
            for (var cell : preview.drawing) {
                var color = cell.color();
                LevelRenderer.renderLineBox(pose, lines, new AABB(cell.position()).move(-camera.x, -camera.y, -camera.z).inflate(-.015),
                        color.red(), color.green(), color.blue(), cell.opacity());
            }
            buffers.endBatch(OUTLINES);
        } finally {
            pose.popPose();
        }
    }
}
