package dev.overloadsim.workstation;

import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingMenuHost;
import dev.overloadsim.ModContent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Bulk slots are count-one displays. Only server transactions touch the real buffers. */
public final class HighEnergyWorkstationMenu extends AEBaseMenu implements FrequencyBindingMenuHost {
    public static final int INPUTS = 9;
    public static final int DISPLAYS = 18;
    public static final long SLOT_CAPACITY = 1_000_000;
    @GuiSync(30) public boolean formed;
    @GuiSync(31) public long power;
    @GuiSync(32) public int progress;
    @GuiSync(33) public int parallel;
    @GuiSync(34) public String recipe = "";
    @GuiSync(35) public String quantities = "";
    @GuiSync(36) public int frequency = -1;
    @GuiSync(37) public int sideModes;
    @GuiSync(38) public boolean eject;
    @GuiSync(39) public boolean networkExport;
    @GuiSync(40) public String inputFluid = "";
    @GuiSync(41) public long inputFluidAmount;
    @GuiSync(42) public String outputFluid = "";
    @GuiSync(43) public long outputFluidAmount;
    @GuiSync(44) public String buildFeedback = "";
    private final HighEnergyWorkstationBlockEntity host;
    private final UUID hostIdentity;
    private final BlockPos frameAnchor;
    private final ItemStackHandler display = new ItemStackHandler(DISPLAYS);

    public static MenuType<HighEnergyWorkstationMenu> createType() {
        return MenuTypeBuilder.create(HighEnergyWorkstationMenu::new, HighEnergyWorkstationBlockEntity.class)
                .withMenuTitle(h -> Component.translatable("block.overload_sim.high_energy_workstation"))
                .buildUnregistered(ModContent.id("high_energy_workstation"));
    }

    public HighEnergyWorkstationMenu(int id, Inventory inventory, HighEnergyWorkstationBlockEntity host) {
        super(WorkstationContent.MENU.get(), id, inventory, host);
        this.host = host;
        hostIdentity = host.identity();
        frameAnchor = WorkstationMenuAccess.openingAnchor(host);
        var wrapped = new PlatformInventoryWrapper(display);
        for (int i = 0; i < DISPLAYS; i++) {
            var slot = new AppEngSlot(wrapped, i) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public ItemStack remove(int amount) { return ItemStack.EMPTY; }
            };
            slot.setNotDraggable();
            slot.setHideAmount(true);
            addSlot(slot, i < INPUTS ? SlotSemantics.MACHINE_INPUT : SlotSemantics.MACHINE_OUTPUT);
        }
        createPlayerInventorySlots(inventory);
        registerClientAction("workstationItem", Integer.class, action -> {
            if (action == null || action < 0 || action >= DISPLAYS * 3 || !validAction()) return;
            transferBulk(action / 3, action % 3);
        });
        registerClientAction("workstationSide", Integer.class, side -> {
            if (side == null || side < 0 || side >= 6 || !validAction()) return;
            var direction = Direction.from3DDataValue(side);
            host.setSideMode(direction, (host.sideMode(direction) + 1) % 4);
            broadcastChanges();
        });
        registerClientAction("workstationEject", () -> {
            if (!validAction()) return;
            host.setEject(!host.eject());
            broadcastChanges();
        });
        registerClientAction("workstationNetwork", () -> {
            if (!validAction()) return;
            host.setNetworkExport(!host.networkExport());
            broadcastChanges();
        });
        registerClientAction("workstationBuild", () -> {
            if (!validAction() || !(getPlayer() instanceof ServerPlayer player)) return;
            var result = host.build(player);
            buildFeedback = Component.Serializer.toJson(result.message(), player.registryAccess());
            player.displayClientMessage(result.message(), false);
            broadcastChanges();
        });
    }

    public HighEnergyWorkstationBlockEntity host() { return host; }
    public void requestItem(int slot, int mode) {
        if (slot >= 0 && slot < DISPLAYS && mode >= 0 && mode < 3) sendClientAction("workstationItem", slot * 3 + mode);
    }
    public void cycleSide(int side) { sendClientAction("workstationSide", side); }
    public void toggleEject() { sendClientAction("workstationEject"); }
    public void toggleNetworkExport() { sendClientAction("workstationNetwork"); }
    public void autoBuild() { sendClientAction("workstationBuild"); }
    public int sideMode(int side) { return side >= 0 && side < 6 ? sideModes >> (side * 2) & 3 : 0; }

    /** Exact counts never enter ItemStack's normal network codec. The CSV is at most 143 characters. */
    public long quantity(int slot) {
        if (slot < 0 || slot >= DISPLAYS || quantities.length() > DISPLAYS * 8) return 0;
        var parts = quantities.split(",", -1);
        if (parts.length != DISPLAYS) return 0;
        try { return Math.clamp(Long.parseLong(parts[slot]), 0, SLOT_CAPACITY); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private boolean validAction() {
        return isServerSide() && getPlayer().containerMenu == this && stillValid(getPlayer());
    }

    @Override public boolean stillValid(Player player) {
        var level = host.getLevel();
        if (level == null || host.isRemoved() || player.level() != level) return false;
        if (level.isClientSide) return true;
        if (!player.isAlive() || player.isSpectator() || !host.identity().equals(hostIdentity)
                || !level.hasChunkAt(host.getBlockPos()) || level.getBlockEntity(host.getBlockPos()) != host) return false;
        return frameAnchor == null ? player.distanceToSqr(host.getBlockPos().getCenter()) <= 64
                : WorkstationMenuAccess.validateFrameAnchor(host, frameAnchor, player);
    }

    /** Deny vanilla/AE item routes into prototype displays, including forged drag and clone packets. */
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (bulkSlot(slot) || isServerSide() && (player != getPlayer() || !validAction())) return;
        super.clicked(slot, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return !bulkSlot(slot.index) && super.canDragTo(slot); }
    @Override public void swapSlotContents(int first, int second) {
        if (bulkSlot(first) || bulkSlot(second) || isServerSide() && !validAction()) return;
        super.swapSlotContents(first, second);
    }
    @Override public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        if (bulkSlot(slot) || player != getPlayer() || !validAction()) return;
        super.doAction(player, action, slot, id);
    }
    @Override protected List<Slot> getQuickMoveDestinationSlots(ItemStack stack, boolean fromPlayer) {
        return List.of();
    }
    private static boolean bulkSlot(int slot) { return slot >= 0 && slot < DISPLAYS; }
    private WorkstationBuffer buffer(int slot) { return slot < INPUTS ? host.inputs() : host.outputs(); }
    private static int localSlot(int slot) { return slot % INPUTS; }

    /** mode 0 fills a legal cursor stack, mode 1 moves one item, mode 2 moves into ordinary inventory. */
    private void transferBulk(int slot, int mode) {
        var buffer = buffer(slot);
        int local = localSlot(slot);
        if (mode == 2) {
            moveToPlayer(buffer, local);
        } else {
            var carried = getCarried();
            if (!carried.isEmpty() && carried.getCount() > carried.getMaxStackSize()) return;
            if (slot < INPUTS && !carried.isEmpty()) {
                long accepted = buffer.insert(local, carried, mode == 1 ? 1 : carried.getCount(), false);
                if (accepted <= 0) return;
                carried.shrink((int) accepted);
                setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            } else {
                var prototype = buffer.prototype(local);
                if (prototype.isEmpty() || !carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, prototype)) return;
                int space = prototype.getMaxStackSize() - carried.getCount();
                if (space <= 0) return;
                var taken = buffer.extract(local, mode == 1 ? 1 : space, false);
                if (taken.isEmpty()) return;
                if (carried.isEmpty()) setCarried(taken);
                else { carried.grow(taken.getCount()); setCarried(carried); }
            }
        }
        host.saveChanges();
        broadcastChanges();
    }

    private int moveToPlayer(WorkstationBuffer buffer, int slot) {
        int total = 0;
        // There are only 36 ordinary player slots. Every offer is a normal, legal item stack.
        for (int attempt = 0; attempt < 36 && buffer.count(slot) > 0; attempt++) {
            var offered = buffer.extract(slot, Integer.MAX_VALUE, true);
            if (offered.isEmpty()) break;
            int before = offered.getCount();
            insertOrdinaryInventory(offered);
            int moved = before - offered.getCount();
            if (moved <= 0) break;
            buffer.debit(slot, moved);
            total += moved;
        }
        return total;
    }

    /** Explicit insertion also prevents creative Inventory.add from silently discarding a full offer. */
    private void insertOrdinaryInventory(ItemStack offered) {
        var inventory = getPlayerInventory();
        for (int pass = 0; pass < 2 && !offered.isEmpty(); pass++) {
            for (int i = 0; i < 36 && !offered.isEmpty(); i++) {
                var current = inventory.getItem(i);
                if (pass == 0 && (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, offered))) continue;
                if (pass == 1 && !current.isEmpty()) continue;
                int limit = Math.min(inventory.getMaxStackSize(), offered.getMaxStackSize());
                int amount = Math.min(offered.getCount(), Math.max(0, limit - current.getCount()));
                if (amount == 0) continue;
                if (current.isEmpty()) inventory.setItem(i, offered.copyWithCount(amount));
                else current.grow(amount);
                offered.shrink(amount);
                inventory.setChanged();
            }
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (player != getPlayer() || !validAction() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (bulkSlot(index)) {
            var prototype = buffer(index).prototype(localSlot(index));
            if (moveToPlayer(buffer(index), localSlot(index)) == 0) return ItemStack.EMPTY;
            host.saveChanges();
            broadcastChanges();
            return prototype;
        }
        var slot = slots.get(index);
        if (!isPlayerSideSlot(slot) || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        var source = slot.getItem();
        var original = source.copy();
        int remaining = source.getCount();
        for (int i = 0; i < INPUTS && remaining > 0; i++)
            remaining -= (int) host.inputs().insert(i, source, remaining, false);
        int moved = original.getCount() - remaining;
        if (moved <= 0) return ItemStack.EMPTY;
        source.shrink(moved);
        slot.set(source.isEmpty() ? ItemStack.EMPTY : source);
        slot.setChanged();
        host.saveChanges();
        broadcastChanges();
        return original;
    }

    @Override public void broadcastChanges() {
        if (isServerSide()) {
            formed = host.formed();
            power = host.currentPower();
            progress = host.progress();
            parallel = host.lastParallel();
            recipe = host.lastRecipe();
            frequency = host.getFrequencyId();
            eject = host.eject();
            networkExport = host.networkExport();
            sideModes = 0;
            for (var direction : Direction.values()) sideModes |= (host.sideMode(direction) & 3) << (direction.get3DDataValue() * 2);
            var csv = new StringBuilder(DISPLAYS * 8);
            for (int i = 0; i < DISPLAYS; i++) {
                if (i > 0) csv.append(',');
                csv.append(Math.clamp(buffer(i).count(localSlot(i)), 0, SLOT_CAPACITY));
                display.setStackInSlot(i, buffer(i).prototype(localSlot(i)));
            }
            quantities = csv.toString();
            var input = host.fluidInput();
            var output = host.fluidOutput();
            inputFluid = input.isEmpty() ? "" : bounded(input.getHoverName().getString());
            outputFluid = output.isEmpty() ? "" : bounded(output.getHoverName().getString());
            inputFluidAmount = input.getAmount();
            outputFluidAmount = output.getAmount();
        }
        super.broadcastChanges();
    }

    private static String bounded(String value) { return value.substring(0, Math.min(value.length(), 160)); }
}
