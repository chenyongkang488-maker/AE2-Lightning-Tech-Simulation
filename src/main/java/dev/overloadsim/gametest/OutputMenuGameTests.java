package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.helpers.InventoryAction;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.output.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class OutputMenuGameTests {
    private record Fixture(OutputInterfaceBlockEntity host, ServerPlayer player, OutputMenu menu) {}
    private static Fixture fixture(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, OutputContent.BLOCK.get().defaultBlockState());
        var host = (OutputInterfaceBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "output-menu"));
        player.setPos(pos.getCenter());
        var menu = new OutputMenu(1, player.getInventory(), host); player.containerMenu = menu;
        menu.broadcastChanges(); return new Fixture(host, player, menu);
    }
    @GameTest(template = "empty") public static void hiddenConfigurationSlotsAndForgedAmountsCannotMutateTheOtherPage(GameTestHelper h) {
        var f = fixture(h); var config = f.host().getOutputLogic().getConfig();
        f.menu().setFilter(0, new ItemStack(Items.IRON_INGOT, 5));
        f.menu().setFilter(18, new ItemStack(Items.GOLD_INGOT));
        h.assertTrue(config.getAmount(0) == 5 && config.getKey(18) == null, "hidden page config action rejected");
        f.menu().receiveClientAction("outputAmount", "\"18:12\"");
        f.menu().receiveClientAction("outputAmount", "\"0:9223372036854775808\"");
        h.assertTrue(config.getAmount(0) == 5 && config.getKey(18) == null, "hidden and overflowing amount actions rejected");
        f.menu().receiveClientAction("outputPage", "1");
        f.menu().setFilter(18, new ItemStack(Items.GOLD_INGOT));
        f.menu().receiveClientAction("outputAmount", "\"18:1000000000000\"");
        h.assertTrue(config.getAmount(18) == 1_000_000_000_000L && f.host().getLastViewedPage() == 1, "second page accepts exact long amount and persists view");
        f.menu().receiveClientAction("outputPage", "2");
        h.assertTrue(f.menu().currentPage == 1 && !f.menu().getAllConfigSlots().get(0).isActive()
                && f.menu().getAllConfigSlots().get(35).isActive(), "forged page leaves stable visibility intact");
        h.succeed();
    }
    @GameTest(template = "empty") public static void closedOrDistantMenusCannotChangeModeAmountOrTargets(GameTestHelper h) {
        var f = fixture(h); var config = f.host().getOutputLogic().getConfig();
        config.setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 4));
        f.player().containerMenu = f.player().inventoryMenu;
        f.menu().receiveClientAction("outputMode", "null"); f.menu().receiveClientAction("outputAmount", "\"0:99\"");
        f.menu().receiveClientAction("outputTargets", "null");
        h.assertTrue(f.host().getInterfaceMode() == OutputInterfaceBlockEntity.InterfaceMode.NORMAL
                && config.getAmount(0) == 4 && !f.menu().targetsView, "closed menu actions are inert");
        f.player().containerMenu = f.menu(); f.player().setPos(f.host().getBlockPos().getCenter().add(9, 0, 0));
        f.menu().receiveClientAction("outputMode", "null"); f.menu().setFilter(0, new ItemStack(Items.GOLD_INGOT));
        h.assertTrue(f.host().getInterfaceMode() == OutputInterfaceBlockEntity.InterfaceMode.NORMAL
                && config.getKey(0).equals(AEItemKey.of(Items.IRON_INGOT)), "distant menu cannot alter modes or config");
        h.succeed();
    }
    @GameTest(template = "empty") public static void forgedSwapAndTransferIndexesAreIgnoredWithoutChangingInventories(GameTestHelper h) {
        var f = fixture(h);
        f.menu().setFilter(0, new ItemStack(Items.IRON_INGOT));
        f.player().getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 7));
        f.menu().swapSlotContents(-1, 0);
        f.menu().swapSlotContents(0, f.menu().slots.size());
        f.menu().quickMoveStack(f.player(), -1);
        f.menu().quickMoveStack(f.player(), f.menu().slots.size());
        f.menu().clicked(f.menu().slots.size(), 0, ClickType.PICKUP, f.player());
        f.menu().clicked(-100, 0, ClickType.PICKUP, f.player());
        h.assertTrue(f.host().getConfiguredKey(0).equals(AEItemKey.of(Items.IRON_INGOT))
                && f.player().getInventory().getItem(0).getCount() == 7, "invalid indexes neither crash nor change inventories");
        h.succeed();
    }
    @GameTest(template = "empty") public static void targetRemovalUsesExactVisibleRowAndClearStaysWithinSelectedSourceSlot(GameTestHelper h) {
        var f = fixture(h); var position = f.host().getBlockPos().east();
        f.player().setItemInHand(InteractionHand.OFF_HAND, com.moakiee.ae2lt.registry.ModItems.OVERLOADED_WIRELESS_CONNECT_TOOL.toStack());
        h.getLevel().setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
        var first = new OutputConnection(0, h.getLevel().dimension(), position, Direction.UP);
        var second = new OutputConnection(0, h.getLevel().dimension(), position, Direction.DOWN);
        var other = new OutputConnection(1, h.getLevel().dimension(), position, Direction.UP);
        f.host().addConnection(first); f.host().addConnection(second); f.host().addConnection(other);
        f.menu().toggleTargetsView();
        String stale = f.menu().getTargetRows().getFirst().token();
        f.host().removeConnection(first);
        f.menu().removeTarget(stale);
        h.assertTrue(f.host().getConnections().contains(second), "delayed deletion cannot remove next visible row");
        f.menu().removeTarget("forged"); f.menu().clearTargets(1);
        h.assertTrue(f.host().getConnections().size() == 2, "forged token and unselected clear rejected");
        f.menu().removeTarget(f.menu().getTargetRows().getFirst().token());
        h.assertTrue(f.host().getConnections().equals(java.util.List.of(other)), "visible row removes only exact selected-slot endpoint");
        f.menu().selectTargetSlot(1); f.menu().clearTargets(1);
        h.assertTrue(f.host().getConnections().isEmpty(), "selected source slot clear removes remaining endpoint"); h.succeed();
    }
    @GameTest(template = "empty") public static void targetDeletionRequiresHeldToolAndAcceptsOffhandConnector(GameTestHelper h) {
        var f = fixture(h); var position = f.host().getBlockPos().east();
        h.getLevel().setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
        var first = new OutputConnection(0, h.getLevel().dimension(), position, Direction.UP);
        var second = new OutputConnection(0, h.getLevel().dimension(), position, Direction.DOWN);
        var other = new OutputConnection(1, h.getLevel().dimension(), position, Direction.UP);
        f.host().addConnection(first); f.host().addConnection(second); f.host().addConnection(other);
        f.menu().toggleTargetsView();
        f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.player().setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        f.menu().removeTarget(f.menu().getTargetRows().getFirst().token());
        f.menu().clearTargets(0);
        h.assertTrue(f.host().getConnections().equals(java.util.List.of(first, second, other)),
                "deletion and clear without held connector cannot mutate bindings");
        f.menu().broadcastChanges(); h.assertTrue(!f.menu().targetToolHeld, "server synchronization disables deletion controls without tool");
        f.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
        f.menu().removeTarget(f.menu().getTargetRows().getFirst().token());
        f.menu().clearTargets(0);
        h.assertTrue(f.host().getConnections().equals(java.util.List.of(first, second, other)),
                "unrelated held item cannot authorize target deletion");
        f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.player().setItemInHand(InteractionHand.OFF_HAND, com.moakiee.ae2lt.registry.ModItems.OVERLOADED_WIRELESS_CONNECT_TOOL.toStack());
        f.menu().removeTarget(f.menu().getTargetRows().getFirst().token());
        h.assertTrue(f.host().getConnections().equals(java.util.List.of(second, other)), "offhand tool permits exact visible row deletion");
        h.assertTrue(f.menu().targetToolHeld, "server synchronization enables deletion controls with offhand tool");
        f.menu().clearTargets(0);
        h.assertTrue(f.host().getConnections().equals(java.util.List.of(other)), "offhand tool permits selected-slot clear without touching other source slot");
        f.player().setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        f.player().setItemInHand(InteractionHand.MAIN_HAND, com.moakiee.ae2lt.registry.ModItems.OVERLOADED_WIRELESS_CONNECT_TOOL.toStack());
        f.menu().selectTargetSlot(1); f.menu().clearTargets(1);
        h.assertTrue(f.host().getConnections().isEmpty(), "mainhand connector also permits selected-slot clear");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 120) public static void proxyPickupAndShiftMoveDrawRealNetworkItemsWithoutDeposit(GameTestHelper h) {
        var f = fixture(h); var key = AEItemKey.of(Items.IRON_INGOT);
        f.host().getOutputLogic().getConfig().setStack(0, new GenericStack(key, 128));
        h.getLevel().setBlockAndUpdate(f.host().getBlockPos().west(), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAtTickTime(80, () -> {
            h.assertTrue(f.host().getMainNode().isActive(), "output network fixture active");
            var storage = new Storage(key, 1000); mount(f, storage); f.menu().broadcastChanges();
            f.menu().clicked(36, 1, ClickType.PICKUP, f.player());
            h.assertTrue(f.menu().getCarried().getCount() == 32 && storage.amount == 968, "half pickup debits exact native AE stack");
            f.menu().clicked(36, 0, ClickType.QUICK_MOVE, f.player());
            h.assertTrue(f.player().getInventory().getItem(0).is(Items.IRON_INGOT)
                    && f.player().getInventory().getItem(0).getCount() == 64 && storage.amount == 904, "shift transfer draws actual network stack");
            f.menu().setCarried(new ItemStack(Items.GOLD_INGOT, 7));
            f.menu().clicked(36, 0, ClickType.PICKUP, f.player());
            f.menu().doAction(f.player(), InventoryAction.EMPTY_ITEM, 36, 0);
            h.assertTrue(storage.amount == 904 && storage.inserts == 0 && f.menu().getCarried().getCount() == 7,
                    "unrelated cursor and AE emptying routes never deposit"); h.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 120) public static void genericFluidProxyFillsNativeContainerButNeverEmptiesIt(GameTestHelper h) {
        var f = fixture(h); var key = AEFluidKey.of(Fluids.WATER);
        f.host().getOutputLogic().getConfig().setStack(0, new GenericStack(key, 2000));
        h.getLevel().setBlockAndUpdate(f.host().getBlockPos().west(), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAtTickTime(80, () -> {
            var storage = new Storage(key, 10_000); mount(f, storage); f.menu().setCarried(new ItemStack(Items.BUCKET));
            f.menu().doAction(f.player(), InventoryAction.FILL_ITEM, 36, 0);
            h.assertTrue(f.menu().getCarried().is(Items.WATER_BUCKET) && storage.amount == 9000, "native AE fluid strategy debits exact bucket quantity");
            f.menu().doAction(f.player(), InventoryAction.EMPTY_ITEM, 36, 0);
            h.assertTrue(f.menu().getCarried().is(Items.WATER_BUCKET) && storage.amount == 9000 && storage.inserts == 0,
                    "filled container has no reverse insertion route"); h.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 120) public static void fuzzyProxyClicksWithdrawTheDisplayedVariantWithItsActualComponents(GameTestHelper h) {
        var f = fixture(h);
        var pristine = AEItemKey.of(Items.DIAMOND_PICKAXE);
        var damagedStack = new ItemStack(Items.DIAMOND_PICKAXE); damagedStack.setDamageValue(100);
        var damaged = AEItemKey.of(damagedStack);
        f.host().getOutputLogic().getConfig().setStack(0, new GenericStack(pristine, 3));
        f.host().getUpgrades().setItemDirect(0, appeng.core.definitions.AEItems.FUZZY_CARD.stack());
        h.getLevel().setBlockAndUpdate(f.host().getBlockPos().west(), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAtTickTime(80, () -> {
            var storage = new Storage(damaged, 8); mount(f, storage); f.menu().broadcastChanges();
            var visible = f.host().getOutputLogic().getStorage().getStack(0);
            h.assertTrue(visible != null && visible.what().equals(damaged), "proxy displays actual damaged variant when exact configured key is absent");
            f.menu().clicked(36, 0, ClickType.PICKUP, f.player());
            h.assertTrue(f.menu().getCarried().is(Items.DIAMOND_PICKAXE)
                    && f.menu().getCarried().getDamageValue() == 100 && storage.amount == 7,
                    "manual pickup returns displayed variant and debits its exact network key");
            f.menu().clicked(36, 0, ClickType.QUICK_MOVE, f.player());
            h.assertTrue(f.player().getInventory().getItem(0).is(Items.DIAMOND_PICKAXE)
                    && f.player().getInventory().getItem(0).getDamageValue() == 100 && storage.amount == 6,
                    "shift transfer also returns displayed variant with exact components");
            f.player().getInventory().setItem(1, f.menu().getCarried()); f.menu().setCarried(ItemStack.EMPTY);
            f.menu().doAction(f.player(), InventoryAction.PICKUP_OR_SET_DOWN, 36, 0);
            h.assertTrue(f.menu().getCarried().getDamageValue() == 100 && storage.amount == 5,
                    "AE pickup action resolves the same real displayed key");
            h.assertTrue(f.host().getConfiguredKey(0).equals(pristine), "manual fuzzy withdrawal preserves precise configured automatic-export template");
            h.succeed();
        });
    }
    private static void mount(Fixture fixture, Storage storage) {
        fixture.host().getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts -> mounts.mount(storage, 0));
    }
    private static final class Storage implements MEStorage {
        private final AEKey key; private long amount; private int inserts;
        Storage(AEKey key, long amount) { this.key = key; this.amount = amount; }
        @Override public Component getDescription() { return Component.literal("output menu test store"); }
        @Override public void getAvailableStacks(KeyCounter out) { out.add(key, amount); }
        @Override public long extract(AEKey what, long requested, Actionable action, IActionSource source) {
            if (!key.equals(what)) return 0;
            long drawn = Math.min(amount, requested); if (action == Actionable.MODULATE) amount -= drawn; return drawn;
        }
        @Override public long insert(AEKey what, long requested, Actionable action, IActionSource source) { inserts++; return 0; }
    }
}
