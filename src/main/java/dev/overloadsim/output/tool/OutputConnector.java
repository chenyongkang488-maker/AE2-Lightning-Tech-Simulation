package dev.overloadsim.output.tool;

import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderHost;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import com.moakiee.ae2lt.logic.WirelessConnectorTargetHelper;
import dev.overloadsim.output.OutputConnection;
import dev.overloadsim.output.OutputInterfaceBlockEntity;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-owned selection and tool mutations; never trusts a client-supplied source or resource key. */
public final class OutputConnector {
    private OutputConnector() {}

    public static boolean isTool(ItemStack stack) { return stack.getItem() instanceof OverloadedWirelessConnectorItem; }

    public static boolean originalHost(BlockEntity entity) {
        return entity instanceof WirelessPatternProviderHost || entity instanceof OverloadedInterfaceBlockEntity
                || entity instanceof OverloadedPowerSupplyBlockEntity;
    }

    /** Original hosts retain their own selection behavior, including its mode validation. */
    public static boolean handles(Level level, ItemStack stack, BlockPos pos) {
        if (!isTool(stack) || !level.isLoaded(pos)) return false;
        var target = level.getBlockEntity(pos);
        if (target instanceof OutputInterfaceBlockEntity) return true;
        return !originalHost(target) && OutputSourceSelection.read(stack) != null;
    }

    public static boolean use(ServerPlayer player, InteractionHand hand, int heldSlot, BlockPos pos,
            Direction face, boolean batch) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu
                || heldSlot < 0 || heldSlot > 8 || heldSlot != player.getInventory().selected) return false;
        var stack = player.getItemInHand(hand);
        var level = player.serverLevel();
        if (!isTool(stack) || !level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0D)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) return false;
        var target = level.getBlockEntity(pos);
        if (target instanceof OutputInterfaceBlockEntity source) {
            if (source.getInterfaceMode() != OutputInterfaceBlockEntity.InterfaceMode.WIRELESS) {
                hint(player, "need_wireless"); return false;
            }
            if (!allowedSource(source, player)) { hint(player, "denied"); return false; }
            // Exclusivity only clears upstream's own selection entry, preserving unrelated CUSTOM_DATA.
            OverloadedWirelessConnectorItem.clearSelection(stack);
            new OutputSourceSelection(level.dimension(), pos, 0).write(stack);
            changed(player);
            player.displayClientMessage(Component.translatable("message.overload_sim.output_tool.selected",
                    1, keyName(source, 0), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            return true;
        }
        if (originalHost(target)) return false;
        var selection = OutputSourceSelection.read(stack);
        if (selection == null) return false;
        var source = resolve(player, stack, selection);
        if (source == null) return false;
        if (!allowedSource(source, player)) { hint(player, "denied"); return false; }
        var requested = new ArrayList<OutputConnection>();
        int skipped = 0;
        for (var candidate : WirelessConnectorTargetHelper.collectTargets(level, pos, batch,
                OutputInterfaceBlockEntity.MAX_CONNECTIONS)) {
            if (!level.isLoaded(candidate) || originalHost(level.getBlockEntity(candidate))
                    || level.getBlockEntity(candidate) instanceof OutputInterfaceBlockEntity
                    || !level.mayInteract(player, candidate) || !player.mayUseItemAt(candidate, face, stack)) {
                skipped++; continue;
            }
            requested.add(new OutputConnection(selection.sourceSlot(), level.dimension(), candidate, face));
        }
        var plan = OutputToolRules.planAdd(source.getConnections(), requested, OutputInterfaceBlockEntity.MAX_CONNECTIONS,
                connection -> source.isConnectionAllowed(connection) && source.isTargetValid(level, connection));
        int added = 0;
        for (var connection : plan.add()) {
            if (source.addConnection(connection)) added++;
            else skipped++;
        }
        skipped += plan.full() + plan.rejected();
        player.displayClientMessage(Component.translatable("message.overload_sim.output_tool.bound",
                selection.sourceSlot() + 1, keyName(source, selection.sourceSlot()), added, plan.existing(), skipped,
                Component.translatable("gui.overload_sim.output.direction." + face.getName()))
                .withStyle(added > 0 || plan.existing() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return added > 0 || plan.existing() > 0;
    }

    public static boolean cycle(ServerPlayer player, int heldSlot, int steps) {
        if (!OutputToolRules.canCycle(player.isAlive(), player.isSpectator(), player.isShiftKeyDown(),
                player.containerMenu == player.inventoryMenu, heldSlot, player.getInventory().selected, steps)) return false;
        var stack = player.getMainHandItem();
        if (!isTool(stack)) return false;
        var selection = OutputSourceSelection.read(stack);
        if (selection == null) return false;
        var source = resolve(player, stack, selection);
        if (source == null || !allowedSource(source, player)) return false;
        int slot = OutputToolRules.cycle(selection.sourceSlot(), steps);
        new OutputSourceSelection(selection.dimension(), selection.pos(), slot).write(stack);
        changed(player);
        player.displayClientMessage(Component.translatable("message.overload_sim.output_tool.slot", slot + 1,
                keyName(source, slot)).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean clear(ServerPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isAlive() || player.isSpectator() || !isTool(stack) || !OutputSourceSelection.clear(stack)) return false;
        changed(player);
        hint(player, "deselected");
        return true;
    }

    private static boolean allowedSource(OutputInterfaceBlockEntity source, ServerPlayer player) {
        // AE's Platform helper checks that the position belongs to the player's current level.
        // Wireless selection intentionally survives dimension changes; check the actual source level.
        return source.getLevel() != null && source.getLevel().mayInteract(player, source.getBlockPos());
    }

    private static OutputInterfaceBlockEntity resolve(ServerPlayer player, ItemStack stack, OutputSourceSelection selection) {
        var level = player.getServer().getLevel(selection.dimension());
        if (level == null || !level.isLoaded(selection.pos())) {
            hint(player, "source_unloaded"); return null;
        }
        if (!(level.getBlockEntity(selection.pos()) instanceof OutputInterfaceBlockEntity source)) {
            OutputSourceSelection.clear(stack); changed(player); hint(player, "source_lost"); return null;
        }
        if (source.getInterfaceMode() != OutputInterfaceBlockEntity.InterfaceMode.WIRELESS) {
            hint(player, "need_wireless"); return null;
        }
        return source;
    }

    private static Component keyName(OutputInterfaceBlockEntity source, int slot) {
        var key = source.getConfiguredKey(slot);
        return key == null ? Component.translatable("gui.overload_sim.output.empty") : key.getDisplayName();
    }
    private static void changed(ServerPlayer player) {
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
    }
    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.overload_sim.output_tool." + key), true);
    }
}
