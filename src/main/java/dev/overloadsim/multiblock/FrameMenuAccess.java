package dev.overloadsim.multiblock;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserStructureIndex;
import dev.overloadsim.tool.CoilWrench;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Routes an ordinary frame use through its validated owner and the normal AE menu locator. */
public final class FrameMenuAccess {
    private record Opening(BlockEntity host, BlockPos anchor) {}
    private static final ThreadLocal<Opening> OPENING = new ThreadLocal<>();
    private FrameMenuAccess() {}

    /** Captured by the server menu factory while MenuOpener is constructing this specific session. */
    public static @Nullable BlockPos openingAnchor(BlockEntity host) {
        var opening = OPENING.get();
        return opening != null && opening.host == host ? opening.anchor : null;
    }

    public static InteractionResult use(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer) || player.level() != level
                || !player.isAlive() || player.isSpectator() || player.isSecondaryUseActive()
                || configurationTool(player.getMainHandItem()) || configurationTool(player.getOffhandItem())
                || !level.hasChunkAt(pos) || !player.canInteractWithBlock(pos,0) || !level.mayInteract(player,pos)) {
            return InteractionResult.PASS;
        }
        BlockEntity host = owner(level,pos);
        if (host == null) return InteractionResult.PASS;
        var previous = OPENING.get();
        OPENING.set(new Opening(host,pos.immutable()));
        try {
            boolean opened = host instanceof LaserMachineBlockEntity
                    ? MenuOpener.open(LaserContent.MENU.get(),player,MenuLocators.forBlockEntity(host))
                    : MenuOpener.open(MultiblockContent.MENU.get(),player,MenuLocators.forBlockEntity(host));
            return opened ? InteractionResult.CONSUME : InteractionResult.PASS;
        } finally {
            if (previous == null) OPENING.remove(); else OPENING.set(previous);
        }
    }

    /** Called every tick and before server menu actions; an old anchor grants no stale access. */
    public static boolean validAnchor(Player player, BlockEntity host, UUID identity, BlockPos anchor) {
        var level = host.getLevel();
        if (level == null || level.isClientSide || player.level() != level || !player.isAlive()
                || player.isSpectator() || host.isRemoved() || !level.hasChunkAt(anchor)
                || !level.hasChunkAt(host.getBlockPos()) || level.getBlockEntity(host.getBlockPos()) != host
                || player.distanceToSqr(anchor.getCenter()) > 64) return false;
        if (host instanceof LaserMachineBlockEntity laser && !laser.identity().equals(identity)
                || host instanceof SimulationControllerBlockEntity chamber && !chamber.identity().equals(identity)) return false;
        return owner(level,anchor) == host;
    }

    private static @Nullable BlockEntity owner(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(LaserContent.FRAME.get())) return LaserStructureIndex.frameOwner(level,pos);
        if (level.getBlockState(pos).is(MultiblockContent.FRAME.get())) return SimulationStructureIndex.frameOwner(level,pos);
        return null;
    }

    private static boolean configurationTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (InteractionUtil.canWrenchDisassemble(stack) || InteractionUtil.canWrenchRotate(stack) || CoilWrench.active(stack)) return true;
        // Optional-tool IDs avoid loading Mekanism classes when that mod is absent.
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("mekanism") && id.getPath().equals("configurator")
                || id.getNamespace().equals("ae2") && (id.getPath().equals("memory_card") || id.getPath().equals("network_tool"));
    }
}
