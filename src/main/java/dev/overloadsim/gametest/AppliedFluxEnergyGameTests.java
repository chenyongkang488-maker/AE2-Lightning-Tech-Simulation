package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.machine.SimulationChamberBlockEntity;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.tool.CoilEnergy;
import dev.overloadsim.tool.CoilModules;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests the machines' existing charging entry points against finite, separate FE and AE stores. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class AppliedFluxEnergyGameTests {
    private static final AEKey FE = optionalFeKey();

    private record Network(AENetworkedBlockEntity host, EnergyCellBlockEntity battery, DriveBlockEntity drive) {
        void ready(GameTestHelper h) {
            h.assertTrue(host.getMainNode().isActive(), "finite AE battery keeps the ME network online");
            battery.injectAEPower(battery.getAEMaxPower(), Actionable.MODULATE);
        }
        double ae() { return host.getMainNode().getGrid().getEnergyService().getStoredPower(); }
        long fe() { return FE == null ? 0 : host.getMainNode().getGrid().getStorageService().getInventory().extract(FE, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty()); }
        void seedFe(GameTestHelper h, long amount) {
            long accepted = host.getMainNode().getGrid().getStorageService().getInventory().insert(FE, amount, Actionable.MODULATE, IActionSource.empty());
            h.assertTrue(accepted == amount, "real Applied Flux cell accepts the exact seed");
        }
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void emptyFeCellCannotChargeChamberFromFullAeBattery(GameTestHelper h) {
        var machine = chamber(h); var network = connect(h, machine, true);
        machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(2_000_000));
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            double ae = network.ae(); chargeChamber(machine, 1000);
            h.assertTrue(machine.energy().getEnergyStored() == 0 && network.fe() == 0, "empty FE storage cannot be replaced by AE battery power for the chamber");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void emptyFeCellCannotChargeMultiblockFromFullAeBattery(GameTestHelper h) {
        var machine = controller(h); var network = connect(h, machine, true);
        machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(2_000_000));
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            double ae = network.ae(); chargeController(machine);
            h.assertTrue(machine.energy().getEnergyStored() == 0 && network.fe() == 0, "empty FE storage cannot be replaced by AE battery power for the multiblock");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void emptyFeCellCannotChargeCoilFromFullAeBattery(GameTestHelper h) {
        var bench = bench(h); var network = connect(h, bench, true); var coil = coil();
        h.runAtTickTime(80, () -> {
            network.ready(h); double ae = network.ae(); CoilEnergy.charge(coil, bench.getGrid());
            h.assertTrue(CoilEnergy.read(coil) == 0 && network.fe() == 0, "empty FE storage cannot be replaced by AE battery power for the coil");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void emptyFeCellCannotChargeLaserFromFullAeBattery(GameTestHelper h) {
        var machine = laser(h, LaserKind.CONSTRAINT); var network = connect(h, machine, true);
        machine.energy().load(machine.energy().capacity());
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().load(0); double ae = network.ae(); machine.bridge().charge();
            h.assertTrue(machine.energy().stored() == 0 && network.fe() == 0, "empty FE storage cannot be replaced by AE battery power for laser instruments");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void receiverWithoutFeStorageDoesNotChargeAeBattery(GameTestHelper h) {
        var machine = laser(h, LaserKind.RECEIVER); var network = connect(h, machine, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); network.battery().extractAEPower(20_000, Actionable.MODULATE, PowerMultiplier.ONE);
            machine.energy().load(1000); double ae = network.ae(); machine.bridge().export();
            h.assertTrue(machine.energy().stored() == 1000, "receiver retains FE when no FE storage can accept it");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void chamberChargesExactFeFromRealCell(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = chamber(h); var network = connect(h, machine, true);
        machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(2_000_000));
        h.runAtTickTime(80, () -> {
            network.ready(h); network.seedFe(h, 1500); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            double ae = network.ae(); chargeChamber(machine, 1000); chargeChamber(machine, 1000);
            h.assertTrue(machine.energy().getEnergyStored() == 1000 && network.fe() == 500, "chamber draws exactly 1000 integer FE once to reach its target");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void multiblockUsesOnlyFeAvailableInRealCell(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = controller(h); var network = connect(h, machine, true);
        machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(2_000_000));
        h.runAtTickTime(80, () -> {
            network.ready(h); network.seedFe(h, 2500); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            double ae = network.ae(); chargeController(machine);
            h.assertTrue(machine.energy().getEnergyStored() == 2500 && network.fe() == 0, "multiblock accepts the finite cell's 2500 FE without an AE supplement");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void coilUsesOnlyFeAvailableInRealCell(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var bench = bench(h); var network = connect(h, bench, true); var coil = coil();
        h.runAtTickTime(80, () -> {
            network.ready(h); network.seedFe(h, 3000); double ae = network.ae(); CoilEnergy.charge(coil, bench.getGrid());
            h.assertTrue(CoilEnergy.read(coil) == 3000 && network.fe() == 0, "coil receives precisely the 3000 FE stored in the real cell");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void laserUsesOnlyFeAvailableInRealCell(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = laser(h, LaserKind.CONSTRAINT); var network = connect(h, machine, true);
        machine.energy().load(machine.energy().capacity());
        h.runAtTickTime(80, () -> {
            network.ready(h); network.seedFe(h, 1500); machine.energy().load(0); double ae = network.ae(); machine.bridge().charge();
            h.assertTrue(machine.energy().stored() == 1500 && network.fe() == 0, "laser receives 1500 real FE without supplementing the empty cell from AE");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void receiverDebitsOnlySpaceAcceptedByRealFeCell(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = laser(h, LaserKind.RECEIVER); var network = connect(h, machine, true);
        h.runAtTickTime(80, () -> {
            network.ready(h); network.battery().extractAEPower(20_000, Actionable.MODULATE, PowerMultiplier.ONE);
            long capacity = cellCapacity(network.drive()); network.seedFe(h, capacity - 37);
            machine.energy().load(1000); double ae = network.ae(); machine.bridge().export();
            h.assertTrue(machine.energy().stored() == 963 && network.fe() == capacity, "receiver spends only the FE cell's remaining 37 FE space");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void chamberRefundsFeWhenItsBufferFillsDuringWithdrawal(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = chamber(h); var network = connect(h, machine, false);
        machine.energy().receiveEnergy(2_000_000, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            var source = source(network, 1500, () -> machine.energy().receiveEnergy(2_000_000, false));
            double ae = network.ae(); chargeChamber(machine, 1000);
            h.assertTrue(machine.energy().getEnergyStored() == 2_000_000 && source.stored == 1500,
                    "FE withdrawn after a reentrant physical fill is returned to the original network inventory");
            h.assertTrue(machine.saveWithoutMetadata(h.getLevel().registryAccess()).getLong("NetworkFeCredit") == 0,
                    "successful refund does not create an additional FE credit");
            unchangedAe(h, network, ae); h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void chamberRejectedRefundSurvivesSaveAndSettlesWithoutNewDraw(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = chamber(h); var network = connect(h, machine, false); machine.energy().receiveEnergy(2_000_000, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            var source = source(network, 1000, () -> machine.energy().receiveEnergy(2_000_000, false)); source.rejectRefund = true;
            chargeChamber(machine, 1000); var saved = machine.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(saved.getLong("NetworkFeCredit") == 1000 && source.stored == 0,
                    "network-rejected refund remains owned by the chamber's durable FE credit");
            machine.loadTag(saved, h.getLevel().registryAccess()); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            chargeChamber(machine, 0);
            h.assertTrue(machine.energy().getEnergyStored() == 1000 && source.stored == 0
                            && machine.saveWithoutMetadata(h.getLevel().registryAccess()).getLong("NetworkFeCredit") == 0,
                    "restored FE credit is settled even with a zero new charging target, without drawing again");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void multiblockRejectedRefundPreservesExistingFeCreditJournal(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = controller(h); var network = connect(h, machine, false); machine.energy().receiveEnergy(2_000_000, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            var source = source(network, 2500, () -> machine.energy().receiveEnergy(2_000_000, false)); source.rejectRefund = true;
            chargeController(machine); var saved = machine.saveMachine(h.getLevel().registryAccess());
            h.assertTrue(saved.getLong("FeCredit") == 2500 && source.stored == 0, "unreturned FE is added to the existing durable FE credit journal");
            machine.loadMachine(saved, h.getLevel().registryAccess()); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            chargeController(machine);
            h.assertTrue(machine.energy().getEnergyStored() == 2500 && source.stored == 0
                            && machine.saveMachine(h.getLevel().registryAccess()).getLong("FeCredit") == 0,
                    "restored multiblock credit is consumed once before any empty-grid request");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void destroyedControllerRefundReturnsToPackedOwnerExactlyOnce(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = controller(h); var network = connect(h, machine, false);
        machine.energy().receiveEnergy(2_000_000, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            var pos = machine.getBlockPos(); var state = machine.getBlockState();
            var source = source(network, 2500, () -> h.getLevel().destroyBlock(pos, true)); source.rejectRefund = true;
            chargeController(machine);
            h.assertTrue(machine.isRemoved() && source.stored == 0, "real block destruction happens during the committed FE withdrawal");
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                    entity -> entity.getItem().is(MultiblockContent.CONTROLLER_ITEM.get()));
            h.assertTrue(drops.size() == 1, "destruction emits exactly one packed controller before its refund is known");
            var packed = ItemStack.parse(h.getLevel().registryAccess(), drops.getFirst().getItem().save(h.getLevel().registryAccess())).orElseThrow();
            drops.getFirst().discard();
            h.getLevel().setBlockAndUpdate(pos, state);
            state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            var restored = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            chargeController(restored); chargeController(restored);
            h.assertTrue(restored.energy().getEnergyStored() == 2500 && restored.availableFe() == 2500 && source.stored == 0,
                    "the packed owner recovers all rejected refund FE once, even though its drop was serialized before the callback finished");
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, state);
            state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            var repeated = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(pos); chargeController(repeated);
            h.assertTrue(repeated.availableFe() == 0 && source.stored == 0,
                    "restoring the same pre-refund packed identity cannot claim an already settled refund again");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void destroyedChamberRecoversOnlyNetworkRefundFromItsDroppedItem(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = chamber(h); var network = connect(h, machine, false); machine.energy().receiveEnergy(2_000_000, false);
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().deserializeNBT(h.getLevel().registryAccess(), IntTag.valueOf(0));
            var pos = machine.getBlockPos(); var state = machine.getBlockState();
            var source = source(network, 1000, () -> { machine.energy().receiveEnergy(12_345, false); h.getLevel().destroyBlock(pos, true); });
            source.rejectRefund = true; chargeChamber(machine, 1000); var packed = droppedOwner(h, machine);
            h.getLevel().setBlockAndUpdate(pos, state); state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            var restored = (SimulationChamberBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(restored.energy().getEnergyStored() == 0, "chamber drops retain the existing physical-buffer reset behavior");
            chargeChamber(restored, 0); chargeChamber(restored, 0);
            h.assertTrue(restored.energy().getEnergyStored() == 1000 && source.stored == 0,
                    "the dropped chamber recovers precisely the rejected 1000 FE refund with new withdrawals disabled");
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, state); state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            restored = (SimulationChamberBlockEntity) h.getLevel().getBlockEntity(pos); chargeChamber(restored, 0);
            h.assertTrue(restored.energy().getEnergyStored() == 0, "an already claimed chamber refund is not replayed from its old item"); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void destroyedLaserRecoversOnlyNetworkRefundBeforeFormation(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = laser(h, LaserKind.CONSTRAINT); var network = connect(h, machine, false); machine.energy().load(machine.energy().capacity());
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().load(0); var pos = machine.getBlockPos(); var state = machine.getBlockState();
            var source = source(network, 5000, () -> { machine.energy().load(12_345); h.getLevel().destroyBlock(pos, true); });
            source.rejectRefund = true; machine.bridge().charge(); var packed = droppedOwner(h, machine);
            h.getLevel().setBlockAndUpdate(pos, state); state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            var restored = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(restored.energy().stored() == 0, "laser drops retain the existing physical-buffer reset behavior");
            restored.bridge().charge(); restored.bridge().charge();
            h.assertTrue(restored.energy().stored() == 5000 && source.stored == 0,
                    "the dropped laser settles the rejected refund before formation or a live AE node is required");
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, state); state.getBlock().setPlacedBy(h.getLevel(), pos, state, null, packed);
            restored = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos); restored.bridge().charge();
            h.assertTrue(restored.energy().stored() == 0, "an already claimed laser refund is not replayed from its old item"); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void refundItemSettingsPreserveCustomDataAndNeverCopyThroughMemoryCards(GameTestHelper h) {
        var chamber = chamber(h); var empty = DataComponentMap.builder(); chamber.exportSettings(SettingsFrom.DISMANTLE_ITEM, empty, null);
        assertNoRefundItem(h, empty.build(), "ordinary zero-credit chamber drops do not receive a unique refund component");
        var saved = chamber.saveWithoutMetadata(h.getLevel().registryAccess()); saved.putLong("NetworkFeCredit", 1000);
        chamber.loadTag(saved, h.getLevel().registryAccess());
        var packed = refundSettings(h, chamber, 1000);
        var pos = chamber.getBlockPos(); var state = chamber.getBlockState();
        h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); h.getLevel().setBlockAndUpdate(pos, state);
        var copy = (SimulationChamberBlockEntity) h.getLevel().getBlockEntity(pos); copy.importSettings(SettingsFrom.MEMORY_CARD, packed, null); chargeChamber(copy, 0);
        h.assertTrue(copy.energy().getEnergyStored() == 0, "memory-card import cannot turn chamber refund item data into FE");

        var laser = laser(h, LaserKind.CONSTRAINT); empty = DataComponentMap.builder(); laser.exportSettings(SettingsFrom.DISMANTLE_ITEM, empty, null);
        assertNoRefundItem(h, empty.build(), "ordinary zero-credit laser drops keep their existing stackable components");
        laser.setNetworkFeCredit(5000); packed = refundSettings(h, laser, 5000); pos = laser.getBlockPos(); state = laser.getBlockState();
        h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); h.getLevel().setBlockAndUpdate(pos, state);
        var laserCopy = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos); laserCopy.importSettings(SettingsFrom.MEMORY_CARD, packed, null); laserCopy.bridge().charge();
        h.assertTrue(laserCopy.energy().stored() == 0 && laserCopy.networkFeCredit() == 0, "memory-card import cannot copy laser refund ownership or credit"); h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void coilRejectedRefundSurvivesItemSaveAndPreservesOtherCustomData(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var bench = bench(h); var network = connect(h, bench, false); var coil = coil();
        var custom = new CompoundTag(); custom.putString("fixture_owner", "retained"); coil.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        h.runAtTickTime(80, () -> {
            network.ready(h); var source = source(network, 3000, () -> CoilEnergy.INSTANCE.receiveFe(coil, 10_000_000, false)); source.rejectRefund = true;
            CoilEnergy.charge(coil, bench.getGrid());
            var restored = ItemStack.parse(h.getLevel().registryAccess(), coil.save(h.getLevel().registryAccess())).orElseThrow();
            restored.set(ModContent.COIL_FE.get(), 0L); CoilEnergy.charge(restored, null);
            h.assertTrue(CoilEnergy.read(restored) == 3000 && source.stored == 0,
                    "saved coil receives its rejected-refund credit without a network or a second withdrawal");
            h.assertTrue(restored.get(DataComponents.CUSTOM_DATA).copyTag().getString("fixture_owner").equals("retained"),
                    "FE credit bookkeeping preserves unrelated item custom data"); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void laserRejectedRefundSurvivesSaveAndReturnsToLocalBuffer(GameTestHelper h) {
        if (FE == null) { h.succeed(); return; }
        var machine = laser(h, LaserKind.CONSTRAINT); var network = connect(h, machine, false); machine.energy().load(machine.energy().capacity());
        h.runAtTickTime(80, () -> {
            network.ready(h); machine.energy().load(0);
            var source = source(network, 5000, () -> machine.energy().load(machine.energy().capacity())); source.rejectRefund = true;
            machine.bridge().charge(); var saved = machine.saveMachine(h.getLevel().registryAccess());
            h.assertTrue(saved.getLong("NetworkFeCredit") == 5000 && source.stored == 0, "laser retains every unreturned FE in its saved credit");
            machine.loadMachine(saved, h.getLevel().registryAccess()); machine.checkStructure(); machine.energy().load(0); machine.bridge().charge();
            h.assertTrue(machine.energy().stored() == 5000 && source.stored == 0
                            && machine.saveMachine(h.getLevel().registryAccess()).getLong("NetworkFeCredit") == 0,
                    "reloaded laser settles its credit exactly once"); h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 120)
    public static void receiverRetainsAlreadyDeliveredLegacyFractionDebt(GameTestHelper h) {
        var machine = laser(h, LaserKind.RECEIVER); var network = connect(h, machine, true);
        h.runAtTickTime(80, () -> {
            network.ready(h); var saved = machine.saveMachine(h.getLevel().registryAccess());
            saved.putLong("Energy", 1000); saved.putDouble("AeOutputFraction", 0.25);
            machine.loadMachine(saved, h.getLevel().registryAccess()); machine.checkStructure(); machine.bridge().export();
            h.assertTrue(machine.energy().stored() == (FE == null ? 1000 : 1) && network.fe() == (FE == null ? 0 : 999),
                    "FE cell receives only whole FE that has not partly been delivered before migration");
            h.assertTrue(machine.feCapability().extractEnergy(1000, false) == (FE == null ? 999 : 0)
                            && machine.saveMachine(h.getLevel().registryAccess()).getDouble("AeOutputFraction") == 0.25,
                    "physical FE extraction respects the saved partial debt and does not duplicate it"); h.succeed();
        });
    }

    /** Reentrancy is introduced at the storage boundary; the machine, grid, FE buffer and save remain real. */
    private static final class ChangingStorage implements MEStorage {
        long stored; boolean rejectRefund; final Runnable onDraw;
        ChangingStorage(long amount, Runnable onDraw) { stored = amount; this.onDraw = onDraw; }
        @Override public Component getDescription() { return Component.literal("FE capacity change fixture"); }
        @Override public void getAvailableStacks(KeyCounter out) { if (stored > 0) out.add(FE, stored); }
        @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
            if (!FE.equals(key)) return 0; long drawn = Math.min(stored, amount);
            if (action == Actionable.MODULATE && drawn > 0) { stored -= drawn; onDraw.run(); } return drawn;
        }
        @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
            if (!FE.equals(key) || rejectRefund) return 0; if (action == Actionable.MODULATE) stored += amount; return amount;
        }
    }
    private static ChangingStorage source(Network network, long amount, Runnable onDraw) {
        var result = new ChangingStorage(amount, onDraw);
        network.host().getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts -> mounts.mount(result, 0)); return result;
    }

    private static ItemStack droppedOwner(GameTestHelper h, AENetworkedBlockEntity machine) {
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(machine.getBlockPos()).inflate(2),
                entity -> entity.getItem().is(machine.getBlockState().getBlock().asItem()));
        h.assertTrue(drops.size() == 1, "real destruction emits exactly one owner item");
        var packed = ItemStack.parse(h.getLevel().registryAccess(), drops.getFirst().getItem().save(h.getLevel().registryAccess())).orElseThrow();
        drops.getFirst().discard(); return packed;
    }

    private static DataComponentMap refundSettings(GameTestHelper h, AENetworkedBlockEntity machine, long credit) {
        var custom = new CompoundTag(); custom.putString("fixture_owner", "retained");
        var builder = DataComponentMap.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        machine.exportSettings(SettingsFrom.DISMANTLE_ITEM, builder, null); var components = builder.build();
        var tag = components.get(DataComponents.CUSTOM_DATA).copyTag(); var refund = tag.getCompound("overload_sim:network_fe_refund");
        h.assertTrue(tag.getString("fixture_owner").equals("retained") && refund.hasUUID("Identity") && refund.getLong("Credit") == credit
                        && refund.getAllKeys().equals(java.util.Set.of("Identity", "Credit")),
                "refund item data preserves unrelated custom data and contains only its refund identity and credit");
        var memory = DataComponentMap.builder(); machine.exportSettings(SettingsFrom.MEMORY_CARD, memory, null);
        assertNoRefundItem(h, memory.build(), "memory-card export never carries refund identity or FE credit"); return components;
    }

    private static void assertNoRefundItem(GameTestHelper h, DataComponentMap components, String message) {
        var custom = components.get(DataComponents.CUSTOM_DATA);
        h.assertTrue(custom == null || !custom.copyTag().contains("overload_sim:network_fe_refund"), message);
    }

    private static Network connect(GameTestHelper h, AENetworkedBlockEntity host, boolean installFeCell) {
        var pos = host.getBlockPos();
        h.getLevel().setBlockAndUpdate(pos.below(), appeng.core.definitions.AEBlocks.ENERGY_CELL.block().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.below(2), appeng.core.definitions.AEBlocks.DRIVE.block().defaultBlockState());
        var battery = (EnergyCellBlockEntity) h.getLevel().getBlockEntity(pos.below());
        battery.injectAEPower(battery.getAEMaxPower(), Actionable.MODULATE);
        var drive = (DriveBlockEntity) h.getLevel().getBlockEntity(pos.below(2));
        if (installFeCell && FE != null) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("appflux:fe_1k_cell"));
            var cell = new ItemStack(item);
            h.assertTrue(StorageCells.getCellInventory(cell, drive::saveChanges) != null, "real Applied Flux FE cell handler is registered");
            drive.getInternalInventory().setItemDirect(0, cell);
        }
        return new Network(host, battery, drive);
    }

    private static SimulationChamberBlockEntity chamber(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(5, 3, 5));
        h.getLevel().setBlockAndUpdate(pos, ModContent.CHAMBER.get().defaultBlockState());
        return (SimulationChamberBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static SimulationControllerBlockEntity controller(GameTestHelper h) {
        var result = MultiblockGameTests.build(h, h.absolutePos(new BlockPos(4, 3, 4)), 3);
        result.checkStructure(); return result;
    }
    private static OverloadDeviceWorkbenchBlockEntity bench(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(5, 3, 5));
        h.getLevel().setBlockAndUpdate(pos, com.moakiee.ae2lt.registry.ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get().defaultBlockState());
        return (OverloadDeviceWorkbenchBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static ItemStack coil() {
        var result = new ItemStack(ModContent.COIL.get());
        CoilModules.setCore(result, com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack()); return result;
    }
    private static LaserMachineBlockEntity laser(GameTestHelper h, LaserKind kind) {
        var result = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), kind, Direction.NORTH);
        result.setEnabled(false); result.checkStructure(); h.assertTrue(result.formed(), "laser fixture forms before network charging"); return result;
    }
    private static void chargeChamber(SimulationChamberBlockEntity machine, int target) {
        invokeCharge(machine, "chargeFromNetwork", new Class<?>[]{int.class}, target);
    }
    private static void chargeController(SimulationControllerBlockEntity machine) { invokeCharge(machine, "charge", new Class<?>[0]); }
    private static void invokeCharge(Object machine, String name, Class<?>[] types, Object... args) {
        try { Method method = machine.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(machine, args); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot invoke existing charging entry point", error); }
    }
    private static void unchangedAe(GameTestHelper h, Network network, double before) {
        h.assertTrue(Math.abs(network.ae() - before) < 0.0001, "same-tick FE transaction leaves AE battery balance unchanged");
    }
    private static long cellCapacity(DriveBlockEntity drive) {
        try {
            var storage = StorageCells.getCellInventory(drive.getInternalInventory().getStackInSlot(0), drive::saveChanges);
            return (long) storage.getClass().getMethod("getMaxEnergy").invoke(storage);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot inspect real FE cell capacity", error); }
    }
    private static AEKey optionalFeKey() {
        try {
            var type = Class.forName("com.glodblock.github.appflux.common.me.key.type.EnergyType");
            return (AEKey) Class.forName("com.glodblock.github.appflux.common.me.key.FluxKey").getMethod("of", type).invoke(null, type.getField("FE").get(null));
        } catch (ReflectiveOperationException | LinkageError absent) { return null; }
    }

    static boolean hasFlux() { return FE != null; }

    /** Test fixture utility; unrelated production tests use physical FE rather than depending on this. */
    static void installSeededCell(GameTestHelper h, DriveBlockEntity drive, int slot, long amount) {
        h.assertTrue(FE != null, "Applied Flux is present for the dedicated network charging test");
        var cell = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("appflux:fe_1m_cell")));
        var storage = StorageCells.getCellInventory(cell, drive::saveChanges);
        h.assertTrue(storage != null && storage.insert(FE, amount, Actionable.MODULATE, IActionSource.empty()) == amount, "real FE fixture stores the finite requested energy");
        storage.persist(); drive.getInternalInventory().setItemDirect(slot, cell);
    }
}
