package dev.overloadsim.gametest;

import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import com.moakiee.ae2lt.registry.ModItems;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.output.*;
import dev.overloadsim.output.tool.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class OutputConnectorGameTests {
    private static OutputInterfaceBlockEntity source(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, OutputContent.BLOCK.get().defaultBlockState());
        var source = (OutputInterfaceBlockEntity) h.getLevel().getBlockEntity(pos);
        source.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);
        return source;
    }
    private static ServerPlayer player(GameTestHelper h) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "output-tool"));
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.OVERLOADED_WIRELESS_CONNECT_TOOL.toStack());
        return player;
    }
    private static boolean use(ServerPlayer player, BlockPos pos, Direction face, boolean batch) {
        return OutputConnector.use(player, InteractionHand.MAIN_HAND, player.getInventory().selected, pos, face, batch);
    }
    @GameTest(template = "empty") public static void selectionPreservesForeignDataAndOriginalSelectionClearsAddon(GameTestHelper h) {
        var source = source(h); var player = player(h); var stack = player.getMainHandItem();
        var foreign = new CompoundTag(); foreign.putString("test:foreign", "keep");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(foreign));
        OverloadedWirelessConnectorItem.selectHost(stack, h.getLevel(), source.getBlockPos(), "interface");
        h.assertTrue(use(player, source.getBlockPos(), Direction.UP, false), "tool selects new source");
        var selected = OutputSourceSelection.read(stack);
        h.assertTrue(selected != null && selected.sourceSlot() == 0 && selected.pos().equals(source.getBlockPos()), "selection starts at first slot");
        h.assertTrue(!OverloadedWirelessConnectorItem.hasSelection(stack), "addon source replaces upstream selection only");
        h.assertTrue(stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("test:foreign").equals("keep"), "unrelated tool data survives select");
        OverloadedWirelessConnectorItem.selectHost(stack, h.getLevel(), source.getBlockPos().east(), "interface");
        h.assertTrue(OutputSourceSelection.read(stack) == null && OverloadedWirelessConnectorItem.hasSelection(stack), "upstream source selection clears addon and keeps its own behavior");
        new OutputSourceSelection(h.getLevel().dimension(), source.getBlockPos(), 35).write(stack);
        h.assertTrue(OutputConnector.clear(player, InteractionHand.MAIN_HAND), "air deselection removes addon source");
        h.assertTrue(OverloadedWirelessConnectorItem.hasSelection(stack) && stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("test:foreign").equals("keep"), "clear preserves upstream and unrelated data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void emptySlotsCycleAndDuplicateBindingNeverDisconnects(GameTestHelper h) {
        var source = source(h); var player = player(h);
        h.assertTrue(use(player, source.getBlockPos(), Direction.UP, false), "select source");
        var target = source.getBlockPos().east();
        h.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        h.assertTrue(use(player, target, Direction.WEST, false), "empty slot may bind");
        h.assertTrue(use(player, target, Direction.WEST, false) && source.getConnections().size() == 1, "repeat leaves binding present");
        player.setShiftKeyDown(true);
        h.assertTrue(OutputConnector.cycle(player, 0, -1) && OutputSourceSelection.read(player.getMainHandItem()).sourceSlot() == 35, "reverse wraps into empty slot36");
        h.assertTrue(use(player, target, Direction.WEST, false) && source.getConnections().size() == 2, "same target face accepts distinct source slot");
        h.assertTrue(OutputConnector.cycle(player, 0, 1) && OutputSourceSelection.read(player.getMainHandItem()).sourceSlot() == 0, "forward wraps back to slot1");
        h.assertTrue(use(player, target, Direction.UP, false) && source.getConnections().size() == 3, "same resource slot can bind additional face");
        h.succeed();
    }
    @GameTest(template = "empty") public static void batchAddsEachContiguousTargetOnce(GameTestHelper h) {
        var source = source(h); var player = player(h); use(player, source.getBlockPos(), Direction.UP, false);
        var target = source.getBlockPos().east();
        for (int i = 0; i < 3; i++) h.getLevel().setBlockAndUpdate(target.south(i), Blocks.CHEST.defaultBlockState());
        h.assertTrue(use(player, target, Direction.UP, true) && source.getConnections().size() == 3, "ctrl adds connected matching machines");
        h.assertTrue(use(player, target, Direction.UP, true) && source.getConnections().size() == 3, "repeating batch preserves all existing links");
        h.succeed();
    }
    @GameTest(template = "empty") public static void invalidPacketsAndNormalModeCannotMutateBindings(GameTestHelper h) {
        var source = source(h); var player = player(h); var stack = player.getMainHandItem();
        h.assertTrue(use(player, source.getBlockPos(), Direction.UP, false), "select valid source");
        var before = OutputSourceSelection.read(stack);
        player.setShiftKeyDown(true);
        h.assertTrue(!OutputConnector.cycle(player, 1, 1) && !OutputConnector.cycle(player, 0, 9), "stale slot and invalid wheel packet rejected");
        h.assertTrue(OutputSourceSelection.read(stack).equals(before), "rejected cycle leaves source unchanged");
        var target = source.getBlockPos().east(); h.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        h.assertTrue(!OutputConnector.use(player, InteractionHand.MAIN_HAND, 1, target, Direction.UP, false), "stale use slot rejected");
        h.assertTrue(!use(player, source.getBlockPos().offset(1000, 0, 0), Direction.UP, false), "distant or unloaded target rejected");
        player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        h.assertTrue(!use(player, target, Direction.UP, false), "missing held tool rejected");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        source.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.NORMAL);
        h.assertTrue(!use(player, target, Direction.UP, false) && source.getConnections().isEmpty(), "nonwireless source cannot bind");
        h.succeed();
    }
    @GameTest(template = "empty") public static void unloadedSourceSelectionIsKeptWithoutLoadingChunks(GameTestHelper h) {
        var player = player(h); var stack = player.getMainHandItem();
        var unloaded = new BlockPos(20000000, 80, 20000000);
        var selection = new OutputSourceSelection(h.getLevel().dimension(), unloaded, 11); selection.write(stack);
        player.setShiftKeyDown(true);
        h.assertTrue(!h.getLevel().isLoaded(unloaded), "source fixture is not loaded");
        h.assertTrue(!OutputConnector.cycle(player, 0, 1), "unloaded source rejects mutation");
        h.assertTrue(selection.equals(OutputSourceSelection.read(stack)) && !h.getLevel().isLoaded(unloaded), "selection remains and source chunk stays unloaded");
        h.succeed();
    }
    @GameTest(template = "empty") public static void crossDimensionSourceCanBindCurrentDimensionTarget(GameTestHelper h) {
        var remote = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(remote != null, "test requires nether");
        var localPos = h.absolutePos(new BlockPos(1, 1, 1));
        var pos = new BlockPos(localPos.getX(), 80, localPos.getZ());
        // Nether starts at Y0; the flat GameTest world can place its templates below it.
        // Only the fixture loads its source chunk. The tool itself never forces source or target chunks.
        remote.getChunkAt(pos);
        var prior = remote.getBlockState(pos);
        try {
            remote.setBlockAndUpdate(pos, OutputContent.BLOCK.get().defaultBlockState());
            var source = (OutputInterfaceBlockEntity) remote.getBlockEntity(pos);
            source.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);
            var player = player(h);
            new OutputSourceSelection(remote.dimension(), pos, 0).write(player.getMainHandItem());
            var target = h.absolutePos(new BlockPos(2, 1, 1)); h.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
            h.assertTrue(use(player, target, Direction.WEST, false), "authorized selected source accepts target in other dimension");
            h.assertTrue(source.getConnections().equals(java.util.List.of(new OutputConnection(0, h.getLevel().dimension(), target, Direction.WEST))), "binding stores destination dimension");
        } finally { remote.setBlockAndUpdate(pos, prior); }
        h.succeed();
    }

    @GameTest(template = "empty") public static void upstreamPacketCannotBypassAddonHeldSlotProtocol(GameTestHelper h) {
        var source = source(h); var player = player(h);
        h.assertTrue(use(player, source.getBlockPos(), Direction.UP, false), "select addon source");
        var target = source.getBlockPos().east(); h.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        var packet = new com.moakiee.ae2lt.network.WirelessConnectorUsePacket(InteractionHand.MAIN_HAND,
                target, Direction.WEST, false);
        try {
            // Upstream packets carry no held-slot token and must never mutate an addon selection.
            var method = packet.getClass().getDeclaredMethod("handleOnServer", ServerPlayer.class);
            method.setAccessible(true); method.invoke(packet, player);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        h.assertTrue(source.getConnections().isEmpty(), "only addon protocol with validated held slot may bind addon source");
        h.succeed();
    }
}
