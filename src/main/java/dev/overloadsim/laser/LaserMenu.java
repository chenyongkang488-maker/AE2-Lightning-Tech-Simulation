package dev.overloadsim.laser;

import java.util.List;
import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import dev.overloadsim.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative controls and sealed, insertion-only consumable slots. */
public final class LaserMenu extends AEBaseMenu {
    @GuiSync(30) public boolean enabled;
    @GuiSync(31) public boolean formed;
    @GuiSync(32) public boolean network;
    @GuiSync(33) public long boost;
    @GuiSync(34) public long power;
    @GuiSync(35) public long energy;
    @GuiSync(36) public long energyCapacity;
    @GuiSync(37) public long particleCount;
    @GuiSync(38) public long coreRemainder;
    @GuiSync(39) public int outputMask;
    @GuiSync(40) public int inputSide;
    @GuiSync(41) public int fuelRemaining;
    @GuiSync(42) public int particleRemaining;
    @GuiSync(43) public String activeFuel = "";
    @GuiSync(44) public String status = "";
    @GuiSync(45) public long extremeLightning;
    @GuiSync(46) public String buildFeedback="";
    private final LaserMachineBlockEntity host;
    private final java.util.UUID hostIdentity;

    public static MenuType<LaserMenu> createType() {
        return MenuTypeBuilder.create(LaserMenu::new, LaserMachineBlockEntity.class)
                .withMenuTitle(h -> Component.translatable("block.overload_sim." + h.kind().id()))
                .buildUnregistered(ModContent.id("laser_machine"));
    }

    public LaserMenu(int id, Inventory inventory, LaserMachineBlockEntity host) {
        super(LaserContent.MENU.get(), id, inventory, host);
        this.host = host;
        hostIdentity=host.identity();
        inputSide = host.forward().getOpposite().get3DDataValue();
        if (host.kind() == LaserKind.RECEIVER) {
            var fuel = host.fuelInput();
            var wrapped = new PlatformInventoryWrapper(fuel);
            for (int i = 0; i < fuel.getSlots(); i++) {
                final int index = i;
                addSlot(new AppEngSlot(wrapped, i) {
                    @Override public boolean mayPickup(Player player) { return false; }
                    @Override public ItemStack remove(int amount) { return ItemStack.EMPTY; }
                    @Override public boolean mayPlace(ItemStack stack) {
                        if (!stillValid(inventory.player)) return false;
                        // Pack fuel rules are server data. A remote client can predict an insertion,
                        // but only the server inventory decides whether the ingredient is valid.
                        return LaserMenu.this.isClientSide() ? isSlotEnabled() && !stack.isEmpty()
                                : fuel.isItemValid(index, stack) && super.mayPlace(stack);
                    }
                    @Override public List<Component> getCustomTooltip(ItemStack stack) {
                        return List.of(Component.translatable("gui.overload_sim.laser.sealed_fuel"));
                    }
                }, SlotSemantics.MACHINE_INPUT);
            }
        }
        createPlayerInventorySlots(inventory);
        registerClientAction("laserEnabled", () -> {
            if (!validAction()) return;
            if (!host.enabled() && !validStructure()) return;
            host.setEnabled(!host.enabled());
        });
        registerClientAction("laserBoost", Integer.class, step -> {
            if (step == null || (step != -1 && step != 1) || !validAction()
                    || host.kind() != LaserKind.ENHANCEMENT || !validStructure()) return;
            long value = Math.clamp(host.boost(), 100_000L, 9_000_000L);
            host.setBoost(Math.clamp(value + step * 100_000L, 100_000L, 9_000_000L));
        });
        registerClientAction("laserSide", Integer.class, side -> {
            if (side == null || side < 0 || side >= 6 || !validAction()
                    || host.kind() != LaserKind.DEFLECTOR || !validStructure()) return;
            int input = host.forward().getOpposite().get3DDataValue();
            if (side == input) return;
            host.setOutputMask((host.outputMask() ^ (1 << side)) & 63 & ~(1 << input));
        });
        registerClientAction("laserBuild",()->{
            if(!validAction()||!host.identity().equals(hostIdentity)||!(getPlayer() instanceof net.minecraft.server.level.ServerPlayer player))return;
            var result=LaserAutoConstruction.build(player,host);
            buildFeedback=Component.Serializer.toJson(result.message(),player.registryAccess());
            player.displayClientMessage(result.message(),false);broadcastChanges();
        });
    }

    private boolean validAction() {
        return isServerSide() && getPlayer().containerMenu == this && stillValid(getPlayer());
    }

    private boolean validStructure() {
        host.checkStructure();
        return host.formed();
    }

    public LaserKind kind() { return host.kind(); }
    public void toggleEnabled() { sendClientAction("laserEnabled"); }
    public void changeBoost(int step) { sendClientAction("laserBoost", step); }
    public void toggleSide(int side) { sendClientAction("laserSide", side); }
    public void autoBuild() { sendClientAction("laserBuild"); }

    @Override public void broadcastChanges() {
        if (isServerSide()) {
            enabled = host.enabled(); formed = host.formed(); network = host.networkActive();
            boost = host.boost(); power = host.lastPower();
            energy = host.energy().stored(); energyCapacity = host.energy().capacity();
            particleCount = host.particles().stored(); coreRemainder = host.coreRemainder();
            outputMask = host.outputMask(); inputSide = host.forward().getOpposite().get3DDataValue();
            fuelRemaining = host.fuelRemaining(); particleRemaining = host.particleRemaining();
            activeFuel = host.activeFuel(); status = host.status(); extremeLightning = host.extremeLightning();
        }
        super.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return !host.isRemoved() && player.level() == host.getLevel()
                && host.getLevel().hasChunkAt(host.getBlockPos())
                && host.getLevel().getBlockEntity(host.getBlockPos()) == host
                && player.distanceToSqr(host.getBlockPos().getCenter()) <= 64;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()
                || getSlotSemantic(slots.get(index)) == SlotSemantics.MACHINE_INPUT) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override protected List<Slot> getQuickMoveDestinationSlots(ItemStack stack, boolean fromPlayer) {
        if (!fromPlayer) return List.of();
        return getSlots(SlotSemantics.MACHINE_INPUT).stream().filter(slot -> slot.mayPlace(stack)).toList();
    }
}
