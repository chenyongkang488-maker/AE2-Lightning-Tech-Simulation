package dev.overloadsim.gametest;

import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import dev.overloadsim.workstation.HighEnergyWorkstationMenu;
import dev.overloadsim.workstation.WorkstationContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises menu transactions against real bulk buffers, including hostile packet routes. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationMenuGameTests {
    private record Session(HighEnergyWorkstationBlockEntity host, ServerPlayer player,
            HighEnergyWorkstationMenu menu) {}

    private static Session session(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.getLevel().setBlockAndUpdate(pos, WorkstationContent.CONTROLLER.get().defaultBlockState());
        var host = (HighEnergyWorkstationBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "workstation-menu"));
        player.getInventory().clearContent();
        player.setPos(pos.getCenter());
        var menu = new HighEnergyWorkstationMenu(11, player.getInventory(), host);
        player.containerMenu = menu;
        return new Session(host, player, menu);
    }

    private static void click(Session s, int slot, int mode) {
        s.menu.receiveClientAction("workstationItem", Integer.toString(slot * 3 + mode));
    }

    private static int inventoryCount(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = "laser_empty")
    public static void bulkCursorMovesExactLegalStacksWithoutCreatingResources(GameTestHelper h) {
        var s = session(h);
        s.host.inputs().insert(0, new ItemStack(Items.IRON_INGOT), 999_990, false);
        s.menu.setCarried(new ItemStack(Items.IRON_INGOT, 64));
        click(s, 0, 0);
        h.assertTrue(s.host.inputs().count(0) == 1_000_000 && s.menu.getCarried().getCount() == 54,
                "left insertion debits only ten accepted items at the million-item limit");
        s.menu.setCarried(ItemStack.EMPTY);
        click(s, 0, 1);
        h.assertTrue(s.menu.getCarried().getCount() == 1 && s.host.inputs().count(0) == 999_999,
                "right extraction returns one ordinary item and debits one real item");
        click(s, 0, 0);
        h.assertTrue(s.menu.getCarried().isEmpty() && s.host.inputs().count(0) == 1_000_000,
                "input click with matching carried item inserts rather than duplicating it");
        s.host.outputs().insert(0, new ItemStack(Items.ENDER_PEARL), 1_000_000, false);
        click(s, 9, 0);
        h.assertTrue(s.menu.getCarried().getCount() == 16 && s.host.outputs().count(0) == 999_984,
                "output extraction obeys the item's sixteen-item maximum stack size");
        click(s, 9, 0);
        h.assertTrue(s.menu.getCarried().getCount() == 16 && s.host.outputs().count(0) == 999_984,
                "a full cursor cannot over-extract or insert into output storage");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void shiftInsertionAndExtractionPreserveItemsAtFullCapacity(GameTestHelper h) {
        var s = session(h);
        for (int i = 0; i < 9; i++) s.host.inputs().insert(i, new ItemStack(Items.IRON_INGOT), 1_000_000, false);
        s.host.inputs().debit(8, 3);
        s.player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        int hotbar = s.menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).getFirst().index;
        s.menu.quickMoveStack(s.player, hotbar);
        h.assertTrue(s.host.inputs().count(8) == 1_000_000 && s.player.getInventory().getItem(0).getCount() == 61,
                "shift insertion removes only the three items accepted across all nine inputs");
        s.player.getInventory().clearContent();
        for (int i = 0; i < 36; i++) s.player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        s.player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 61));
        s.host.outputs().insert(0, new ItemStack(Items.IRON_INGOT), 100, false);
        click(s, 9, 2);
        h.assertTrue(inventoryCount(s.player, Items.IRON_INGOT) == 64 && s.host.outputs().count(0) == 97,
                "shift extraction debits only three items when the ordinary inventory is otherwise full");
        click(s, 9, 2);
        h.assertTrue(s.host.outputs().count(0) == 97 && s.menu.getCarried().isEmpty(),
                "repeated shift on a full inventory cannot consume or duplicate bulk items");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void displaySlotsRejectVanillaCopyDragSwapAndOutputInsertion(GameTestHelper h) {
        var s = session(h);
        s.host.inputs().insert(0, new ItemStack(Items.DIAMOND), 1_000_000, false);
        s.host.outputs().insert(0, new ItemStack(Items.IRON_INGOT), 100, false);
        s.menu.broadcastChanges();
        h.assertTrue(s.menu.slots.getFirst().getItem().getCount() == 1 && s.menu.quantity(0) == 1_000_000,
                "vanilla item sync carries a count-one prototype; quantity sync carries exact million count");
        s.player.getInventory().setItem(0, new ItemStack(Items.STONE, 32));
        for (var type : ClickType.values()) s.menu.clicked(0, 0, type, s.player);
        for (var action : appeng.helpers.InventoryAction.values()) s.menu.doAction(s.player, action, 0, 0);
        s.menu.swapSlotContents(0, 18);
        h.assertTrue(!s.menu.canDragTo(s.menu.slots.getFirst()) && s.menu.getCarried().isEmpty()
                && s.host.inputs().count(0) == 1_000_000 && s.player.getInventory().getItem(0).getCount() == 32,
                "vanilla click families, swaps and drag eligibility cannot take a display prototype");
        s.menu.setCarried(new ItemStack(Items.DIAMOND, 7));
        click(s, 9, 0);
        h.assertTrue(s.menu.getCarried().getCount() == 7 && s.host.outputs().count(0) == 100,
                "output slot never accepts the carried item or replaces the stored prototype");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void creativeShiftIntoAFullInventoryCannotDiscardBulkOutput(GameTestHelper h) {
        var s = session(h);
        s.player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for (int i = 0; i < 36; i++) s.player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        s.host.outputs().insert(0, new ItemStack(Items.DIAMOND), 100, false);
        click(s, 9, 2);
        h.assertTrue(s.host.outputs().count(0) == 100 && inventoryCount(s.player, Items.DIAMOND) == 0,
                "creative inventory overflow cannot silently discard and debit a bulk output offer");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void forgedActionsClosedMenuFarPlayerAndReplacedHostCannotMutate(GameTestHelper h) {
        var s = session(h);
        s.host.outputs().insert(0, new ItemStack(Items.DIAMOND), 100, false);
        for (String invalid : new String[] { "-1", "54", "2147483647", "null" })
            s.menu.receiveClientAction("workstationItem", invalid);
        s.player.containerMenu = s.player.inventoryMenu;
        click(s, 9, 0);
        s.menu.receiveClientAction("workstationEject", "null");
        h.assertTrue(s.host.outputs().count(0) == 100 && !s.host.eject() && s.menu.getCarried().isEmpty(),
                "malformed indices and closed-menu packets cannot move items or change configuration");
        s.player.containerMenu = s.menu;
        s.player.setPos(s.host.getBlockPos().getCenter().add(30, 0, 0));
        click(s, 9, 0);
        s.menu.receiveClientAction("workstationEject", "null");
        s.menu.receiveClientAction("workstationNetwork", "null");
        h.assertTrue(s.host.outputs().count(0) == 100 && !s.host.eject() && !s.host.networkExport(),
                "a player outside menu reach cannot change item storage or either automatic output switch");
        s.player.setPos(s.host.getBlockPos().getCenter());
        h.getLevel().setBlockAndUpdate(s.host.getBlockPos(), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(s.host.getBlockPos(), WorkstationContent.CONTROLLER.get().defaultBlockState());
        click(s, 9, 0);
        h.assertTrue(s.host.outputs().count(0) == 100 && s.menu.getCarried().isEmpty() && !s.menu.stillValid(s.player),
                "replacing the block at the same coordinates cannot resurrect an old menu session");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void independentOutputSwitchesUseTheSameValidatedMenu(GameTestHelper h) {
        var s = session(h);
        s.menu.receiveClientAction("workstationEject", "null");
        s.menu.broadcastChanges();
        h.assertTrue(s.host.eject() && s.menu.eject && !s.host.networkExport() && !s.menu.networkExport,
                "adjacent switch does not enable network export");
        s.menu.receiveClientAction("workstationNetwork", "null");
        s.menu.broadcastChanges();
        h.assertTrue(s.host.eject() && s.host.networkExport() && s.menu.eject && s.menu.networkExport,
                "ejection and ME export toggles synchronize without changing the active menu");
        s.menu.receiveClientAction("workstationSide", "6");
        s.menu.receiveClientAction("workstationSide", "2");
        h.assertTrue(s.host.eject() && s.host.networkExport(), "retired face packets cannot change either automatic output setting");
        h.succeed();
    }
}
