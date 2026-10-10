// SPDX-License-Identifier: LGPL-3.0-only
package dev.overloadsim.output;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import com.google.gson.Gson;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingMenuHost;
import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import dev.overloadsim.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Independent output-only menu. The paged config/storage layout and toolbar
 * interactions follow AE2LT's OverloadedInterfaceMenu (Moakiee and contributors,
 * LGPL-3.0-only); no InterfaceMenu input or stocking routes are inherited.
 */
public final class OutputMenu extends AEBaseMenu implements FrequencyBindingMenuHost {
    private static final MenuTypeBuilder.MenuFactory<OutputMenu, OutputInterfaceBlockEntity> FACTORY = OutputMenu::new;
    public static final MenuType<OutputMenu> TYPE = MenuTypeBuilder.create(FACTORY, OutputInterfaceBlockEntity.class)
            .withMenuTitle(host -> Component.translatable("block.overload_sim.overload_output_interface"))
            .buildUnregistered(ModContent.id("overload_output_interface"));
    public static final int TARGET_ROWS = 5;
    private static final Gson JSON = new Gson();

    @GuiSync(20) public int currentPage;
    @GuiSync(21) public int interfaceMode;
    @GuiSync(22) public int exportMode;
    @GuiSync(23) public int ioSpeedMode;
    @GuiSync(24) public int energyDirOrdinal = -1;
    @GuiSync(25) public long unlimitedBits;
    @GuiSync(26) public int frequency = -1;
    @GuiSync(27) public FuzzyMode fuzzyMode = FuzzyMode.IGNORE_ALL;
    @GuiSync(28) public boolean targetsView;
    @GuiSync(29) public int selectedTargetSlot;
    @GuiSync(30) public int targetPage;
    @GuiSync(31) public int targetCount;
    @GuiSync(32) public String targetRows = "[]";
    @GuiSync(33) public boolean fuzzyInstalled;
    @GuiSync(34) public boolean targetToolHeld;

    public record TargetRow(String token, String dimension, int x, int y, int z, int face) {}
    private final OutputInterfaceBlockEntity host;
    private final List<Slot> configSlots = new ArrayList<>();
    private final List<Slot> storageSlots = new ArrayList<>();
    private final OutputTargetSnapshot<OutputConnection> snapshot = new OutputTargetSnapshot<>();
    private String decodedRows = "";
    private List<TargetRow> cachedRows = List.of();

    public OutputMenu(int id, Inventory inventory, OutputInterfaceBlockEntity host) {
        super(TYPE, id, inventory, host);
        this.host = host;
        var config = host.getOutputLogic().getConfig().createMenuWrapper();
        var storage = host.getOutputLogic().getStorage().createMenuWrapper();
        for (int i = 0; i < OutputMenuRules.SLOTS; i++)
            configSlots.add(addSlot(new FakeSlot(config, i), OutputSlotSemantics.CONFIG[i / 9]));
        for (int i = 0; i < OutputMenuRules.SLOTS; i++) {
            var slot = new AppEngSlot(storage, i) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public ItemStack remove(int amount) { return ItemStack.EMPTY; }
            };
            slot.setNotDraggable();
            storageSlots.add(addSlot(slot, OutputSlotSemantics.STORAGE[i / 9]));
        }
        setupUpgrades(host.getUpgrades());
        createPlayerInventorySlots(inventory);
        currentPage = Math.clamp(host.getLastViewedPage(), 0, 1);
        registerClientAction("outputPage", Integer.class, this::setPage);
        registerClientAction("outputMode", this::cycleInterfaceMode);
        registerClientAction("outputExport", this::cycleExportMode);
        registerClientAction("outputSpeed", this::cycleIOSpeed);
        registerClientAction("outputDirection", this::cycleEnergyDirection);
        registerClientAction("outputFuzzy", this::cycleFuzzyMode);
        registerClientAction("outputUnlimited", Integer.class, this::toggleUnlimited);
        registerClientAction("outputAmount", String.class, this::receiveAmount);
        registerClientAction("outputTargets", this::toggleTargetsView);
        registerClientAction("outputTargetSlot", Integer.class, this::selectTargetSlot);
        registerClientAction("outputTargetPage", Integer.class, this::setTargetPage);
        registerClientAction("outputRemoveTarget", String.class, this::removeTarget);
        registerClientAction("outputClearTargets", Integer.class, this::clearTargets);
        applyPageVisibility();
    }

    public OutputInterfaceBlockEntity host() { return host; }
    public IUpgradeInventory getUpgrades() { return host.getUpgrades(); }
    public List<Slot> getAllConfigSlots() { return List.copyOf(configSlots); }
    public List<Slot> getAllStorageSlots() { return List.copyOf(storageSlots); }
    public boolean isSlotUnlimited(int slot) { return slot >= 0 && slot < 36 && (unlimitedBits & (1L << slot)) != 0; }
    public int targetPages() { return Math.max(1, (targetCount + TARGET_ROWS - 1) / TARGET_ROWS); }

    private boolean validAction() {
        return isServerSide() && getPlayer().containerMenu == this && stillValid(getPlayer());
    }
    private boolean holdsTargetTool() {
        return getPlayer().getMainHandItem().getItem() instanceof OverloadedWirelessConnectorItem
                || getPlayer().getOffhandItem().getItem() instanceof OverloadedWirelessConnectorItem;
    }
    @Override public boolean stillValid(Player player) {
        var level = host.getLevel();
        if (level == null || host.isRemoved() || player.level() != level) return false;
        if (level.isClientSide) return true;
        return player == getPlayer() && player.isAlive() && !player.isSpectator()
                && level.hasChunkAt(host.getBlockPos()) && level.getBlockEntity(host.getBlockPos()) == host
                && player.distanceToSqr(host.getBlockPos().getCenter()) <= 64 && super.stillValid(player);
    }

    public void applyPageVisibility() {
        for (int i = 0; i < 36; i++) {
            boolean active = !targetsView && OutputMenuRules.visibleSlot(i, currentPage);
            ((AppEngSlot) configSlots.get(i)).setActive(active);
            ((AppEngSlot) storageSlots.get(i)).setActive(active);
        }
    }
    public void setPage(Integer page) {
        if (page == null || page < 0 || page > 1) return;
        if (isClientSide()) { sendClientAction("outputPage", page); return; }
        if (!validAction()) return;
        currentPage = page; host.setLastViewedPage(page); applyPageVisibility(); broadcastChanges();
    }
    public void cycleInterfaceMode() {
        if (isClientSide()) { sendClientAction("outputMode"); return; }
        if (!validAction()) return;
        var modes = OutputInterfaceBlockEntity.InterfaceMode.values();
        host.setInterfaceMode(modes[(host.getInterfaceMode().ordinal() + 1) % modes.length]); broadcastChanges();
    }
    public void cycleExportMode() {
        if (isClientSide()) { sendClientAction("outputExport"); return; }
        if (!validAction()) return;
        var modes = OutputInterfaceBlockEntity.ExportMode.values();
        host.setExportMode(modes[(host.getExportMode().ordinal() + 1) % modes.length]); broadcastChanges();
    }
    public void cycleIOSpeed() {
        if (isClientSide()) { sendClientAction("outputSpeed"); return; }
        if (!validAction()) return;
        var modes = OutputInterfaceBlockEntity.IOSpeedMode.values();
        host.setIOSpeedMode(modes[(host.getIOSpeedMode().ordinal() + 1) % modes.length]); broadcastChanges();
    }
    public void cycleEnergyDirection() {
        if (isClientSide()) { sendClientAction("outputDirection"); return; }
        if (!validAction()) return;
        var direction = host.getEnergyOutputDirection();
        int next = direction == null ? 0 : direction.get3DDataValue() + 1;
        host.setEnergyOutputDirection(next >= 6 ? null : Direction.from3DDataValue(next)); broadcastChanges();
    }
    public void cycleFuzzyMode() {
        if (isClientSide()) { sendClientAction("outputFuzzy"); return; }
        if (!validAction() || !host.getUpgrades().isInstalled(AEItems.FUZZY_CARD)) return;
        var manager = host.getOutputLogic().getConfigManager();
        var values = FuzzyMode.values();
        manager.putSetting(Settings.FUZZY_MODE, values[(manager.getSetting(Settings.FUZZY_MODE).ordinal() + 1) % values.length]);
        broadcastChanges();
    }
    public void toggleUnlimited(Integer slot) {
        if (slot == null || !OutputMenuRules.visibleSlot(slot, currentPage)) return;
        if (isClientSide()) { sendClientAction("outputUnlimited", slot); return; }
        if (!validAction() || targetsView || host.getConfiguredKey(slot) == null) return;
        host.setUnlimited(slot, !host.isUnlimited(slot)); broadcastChanges();
    }
    public void setAmount(int slot, String amount) { sendClientAction("outputAmount", slot + ":" + amount); }
    private void receiveAmount(String input) {
        if (!validAction() || input == null || input.length() > 23 || targetsView) return;
        var parts = input.split(":", -1);
        if (parts.length != 2) return;
        try {
            int slot = Integer.parseInt(parts[0]);
            if (!OutputMenuRules.visibleSlot(slot, currentPage)) return;
            var config = host.getOutputLogic().getConfig();
            var key = config.getKey(slot); if (key == null) return;
            long amount = OutputMenuRules.parseAmount(parts[1], config.getMaxAmount(key));
            if (amount < 1) return;
            config.setStack(slot, new GenericStack(key, amount)); broadcastChanges();
        } catch (NumberFormatException ignored) {}
    }
    public void toggleTargetsView() {
        if (isClientSide()) { sendClientAction("outputTargets"); return; }
        if (!validAction()) return;
        targetsView = !targetsView; applyPageVisibility(); broadcastChanges();
    }
    public void selectTargetSlot(Integer slot) {
        if (slot == null || slot < 0 || slot >= 36) return;
        if (isClientSide()) { sendClientAction("outputTargetSlot", slot); return; }
        if (!validAction()) return;
        if (selectedTargetSlot != slot) { selectedTargetSlot = slot; targetPage = 0; }
        broadcastChanges();
    }
    public void setTargetPage(Integer page) {
        if (page == null || page < 0 || page >= targetPages()) return;
        if (isClientSide()) { sendClientAction("outputTargetPage", page); return; }
        if (!validAction() || !targetsView) return;
        targetPage = page; broadcastChanges();
    }
    public void removeTarget(String token) {
        if (isClientSide()) { sendClientAction("outputRemoveTarget", token); return; }
        if (!validAction() || !holdsTargetTool() || !targetsView || token == null || token.length() != 36) return;
        refreshTargets(); // Invalidate a row if another interaction changed the visible list meanwhile.
        var connection = snapshot.resolve(selectedTargetSlot, token);
        if (connection != null && connection.sourceSlot() == selectedTargetSlot) host.removeConnection(connection);
        broadcastChanges();
    }
    public void clearTargets(Integer slot) {
        if (slot == null || slot != selectedTargetSlot) return;
        if (isClientSide()) { sendClientAction("outputClearTargets", slot); return; }
        if (!validAction() || !holdsTargetTool() || !targetsView) return;
        host.clearConnections(slot); broadcastChanges();
    }
    private void refreshTargets() {
        var all = host.getConnections().stream().filter(c -> c.sourceSlot() == selectedTargetSlot).toList();
        targetCount = all.size(); targetPage = Math.clamp(targetPage, 0, targetPages() - 1);
        int start = targetPage * TARGET_ROWS;
        var visible = targetsView ? all.subList(start, Math.min(start + TARGET_ROWS, all.size())) : List.<OutputConnection>of();
        snapshot.refresh(selectedTargetSlot, visible);
        var rows = new ArrayList<TargetRow>(visible.size());
        for (int i = 0; i < visible.size(); i++) {
            var c = visible.get(i); var pos = c.pos();
            rows.add(new TargetRow(snapshot.token(i), c.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(), c.face().get3DDataValue()));
        }
        targetRows = JSON.toJson(rows);
    }
    public List<TargetRow> getTargetRows() {
        if (!decodedRows.equals(targetRows)) {
            decodedRows = targetRows; cachedRows = List.of();
            if (targetRows.length() <= 4096) try {
                var rows = JSON.fromJson(targetRows, TargetRow[].class);
                if (rows != null && rows.length <= TARGET_ROWS) cachedRows = List.of(rows);
            } catch (RuntimeException ignored) {}
        }
        return cachedRows;
    }

    private boolean configurationSlot(int id) { return id >= 0 && id < 36; }
    private boolean proxySlot(int id) { return id >= 36 && id < 72; }
    private boolean visiblePair(int id) { return !targetsView && OutputMenuRules.visibleSlot(id % 36, currentPage); }
    @Override public void clicked(int id, int button, ClickType type, Player player) {
        if (id >= slots.size() || id < -1 && id != -999) return;
        if (isServerSide() && (player != getPlayer() || !validAction())) return;
        if ((configurationSlot(id) || proxySlot(id)) && !visiblePair(id)) return;
        if (proxySlot(id)) {
            if (isServerSide()) extractItems(id - 36, button, type, player);
            return;
        }
        super.clicked(id, button, type, player);
    }
    @Override public void setFilter(int id, ItemStack stack) {
        if (!configurationSlot(id) || !visiblePair(id) || isServerSide() && !validAction()) return;
        super.setFilter(id, stack);
    }
    @Override public void doAction(ServerPlayer player, InventoryAction action, int id, long token) {
        if (player != getPlayer() || !validAction()) return;
        if ((configurationSlot(id) || proxySlot(id)) && !visiblePair(id)) return;
        if (proxySlot(id)) {
            int sourceSlot = id - 36;
            var key = proxyKey(sourceSlot);
            if (key == null) return;
            switch (action) {
                case FILL_ITEM, FILL_ENTIRE_ITEM -> handleFillingHeldItem(
                        (amount, mode) -> host.getOutputLogic().extractConfigured(sourceSlot, key, amount, mode, IActionSource.ofPlayer(player)),
                        key, action == InventoryAction.FILL_ENTIRE_ITEM);
                case PICKUP_OR_SET_DOWN -> extractItems(sourceSlot, 0, ClickType.PICKUP, player);
                case SPLIT_OR_PLACE_SINGLE -> extractItems(sourceSlot, 1, ClickType.PICKUP, player);
                case SHIFT_CLICK -> extractItems(sourceSlot, 0, ClickType.QUICK_MOVE, player);
                case PICKUP_SINGLE -> extractItems(sourceSlot, 0, ClickType.THROW, player);
                default -> {} // EMPTY_ITEM, PLACE_SINGLE, region moves and deposits have no output route.
            }
            return;
        }
        super.doAction(player, action, id, token);
    }
    @Override public boolean canDragTo(Slot slot) {
        return !proxySlot(slot.index) && (!configurationSlot(slot.index) || visiblePair(slot.index)) && super.canDragTo(slot);
    }
    @Override public void swapSlotContents(int first, int second) {
        if (first < 0 || second < 0 || first >= slots.size() || second >= slots.size()
                || configurationSlot(first) != configurationSlot(second)
                || proxySlot(first) || proxySlot(second) || configurationSlot(first) && !visiblePair(first)
                || configurationSlot(second) && !visiblePair(second) || isServerSide() && !validAction()) return;
        super.swapSlotContents(first, second);
    }
    @Override protected List<Slot> getQuickMoveDestinationSlots(ItemStack stack, boolean fromPlayer) {
        return fromPlayer ? getSlots(SlotSemantics.UPGRADE) : super.getQuickMoveDestinationSlots(stack, false);
    }
    @Override public ItemStack quickMoveStack(Player player, int id) {
        if (id < 0 || id >= slots.size() || !validAction() || player != getPlayer()) return ItemStack.EMPTY;
        if (proxySlot(id)) {
            if (visiblePair(id)) extractItems(id - 36, 0, ClickType.QUICK_MOVE, player);
            return ItemStack.EMPTY;
        }
        if (configurationSlot(id) && !visiblePair(id)) return ItemStack.EMPTY;
        return super.quickMoveStack(player, id);
    }

    /** Fuzzy proxies display a real variant; manual withdrawal must use that variant's components. */
    private AEKey proxyKey(int slot) {
        var stack = host.getOutputLogic().getStorage().getStack(slot);
        return stack == null || stack.amount() <= 0 ? null : stack.what();
    }
    private void extractItems(int slot, int button, ClickType type, Player player) {
        var key = proxyKey(slot);
        if (!(key instanceof AEItemKey item)) return;
        var logic = host.getOutputLogic(); var source = IActionSource.ofPlayer(player);
        long available = logic.extractConfigured(slot, key, item.getMaxStackSize(), Actionable.SIMULATE, source);
        long limit;
        if (type == ClickType.PICKUP) {
            var carried = getCarried();
            limit = OutputMenuRules.extractionLimit(carried.isEmpty(), item.equals(AEItemKey.of(carried)),
                    carried.getCount(), item.getMaxStackSize(), available, button == 1);
            long extracted = logic.extractConfigured(slot, key, limit, Actionable.MODULATE, source);
            if (extracted > 0) {
                if (carried.isEmpty()) setCarried(item.toStack((int) extracted));
                else { carried.grow((int) extracted); setCarried(carried); }
            }
        } else if (type == ClickType.QUICK_MOVE) {
            int space = inventorySpace(item.toStack());
            limit = Math.min(available, space);
            long extracted = logic.extractConfigured(slot, key, limit, Actionable.MODULATE, source);
            if (extracted > 0) insertOrdinaryInventory(item.toStack((int) extracted));
        } else if (type == ClickType.THROW) {
            limit = Math.min(available, button == 1 ? item.getMaxStackSize() : 1);
            long extracted = logic.extractConfigured(slot, key, limit, Actionable.MODULATE, source);
            if (extracted > 0) player.drop(item.toStack((int) extracted), true);
        } else if (type == ClickType.SWAP && button >= 0 && button < 9 && player.getInventory().getItem(button).isEmpty()) {
            long extracted = logic.extractConfigured(slot, key, available, Actionable.MODULATE, source);
            if (extracted > 0) player.getInventory().setItem(button, item.toStack((int) extracted));
        }
        broadcastChanges();
    }
    private int inventorySpace(ItemStack prototype) {
        int total = 0; var inv = getPlayerInventory();
        for (int i = 0; i < 36; i++) {
            var current = inv.getItem(i);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, prototype)) continue;
            total += Math.max(0, Math.min(inv.getMaxStackSize(), prototype.getMaxStackSize()) - current.getCount());
        }
        return total;
    }
    private void insertOrdinaryInventory(ItemStack offered) {
        var inv = getPlayerInventory();
        for (int pass = 0; pass < 2 && !offered.isEmpty(); pass++) for (int i = 0; i < 36 && !offered.isEmpty(); i++) {
            var current = inv.getItem(i);
            if (pass == 0 && (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, offered))) continue;
            if (pass == 1 && !current.isEmpty()) continue;
            int space = Math.max(0, Math.min(inv.getMaxStackSize(), offered.getMaxStackSize()) - current.getCount());
            int move = Math.min(space, offered.getCount());
            if (move == 0) continue;
            if (current.isEmpty()) inv.setItem(i, offered.copyWithCount(move)); else current.grow(move);
            offered.shrink(move); inv.setChanged();
        }
        // Synchronous vanilla inventory preflight guarantees space; a modded callback cannot silently destroy overflow.
        if (!offered.isEmpty()) getPlayer().drop(offered, false);
    }

    @Override public void broadcastChanges() {
        if (isServerSide()) {
            interfaceMode = host.getInterfaceMode().ordinal(); exportMode = host.getExportMode().ordinal();
            ioSpeedMode = host.getIOSpeedMode().ordinal(); frequency = host.getFrequencyId();
            var direction = host.getEnergyOutputDirection(); energyDirOrdinal = direction == null ? -1 : direction.get3DDataValue();
            fuzzyMode = host.getOutputLogic().getConfigManager().getSetting(Settings.FUZZY_MODE);
            fuzzyInstalled = host.getUpgrades().isInstalled(AEItems.FUZZY_CARD);
            targetToolHeld = holdsTargetTool();
            long bits = 0; for (int i = 0; i < 36; i++) if (host.isUnlimited(i)) bits |= 1L << i;
            unlimitedBits = bits; host.getOutputLogic().refreshStorage(); refreshTargets();
        }
        applyPageVisibility(); super.broadcastChanges();
    }
}
