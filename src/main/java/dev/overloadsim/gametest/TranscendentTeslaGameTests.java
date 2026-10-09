package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEBlocks;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilInventory;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.TeslaCoilMenu;
import com.moakiee.ae2lt.registry.ModItems;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real upstream coil, grid, FE buffer, menu and ticker; storage alone controls backpressure. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class TranscendentTeslaGameTests {
    @GameTest(template="laser_empty",timeoutTicks=80)
    public static void configuredCrystalRecipeFreezesFeAndPartialOutputAcrossReload(GameTestHelper h){
        withCoil(h,f->{
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_FE,300_000L,()->
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_EHV,40L,()->{
                f.storage.shortInsert=true;f.storage.refuseExtremeExtraction=true;prepare(h,f,1,300_000,true);ticks(f,5);
                h.assertTrue(f.coil.getCurrentTotalEnergy()==300_000&&f.coil.isReadyToCommit()&&f.storage.extreme==17,"configured recipe pays new FE and journals partial actual output");
            }));
            var saved=f.coil.saveWithoutMetadata(h.getLevel().registryAccess());
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_FE,100_000L,()->
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_EHV,9L,()->{
                f.coil.loadTag(saved,h.getLevel().registryAccess());
                h.assertTrue(f.coil.getConsumedEnergy()==300_000&&f.coil.getCurrentTotalEnergy()==300_000,"load restores frozen batch FE before upstream clamps consumed energy");
                f.storage.shortInsert=false;tick(f);
                h.assertTrue(f.storage.extreme==40&&!f.coil.hasLockedMode()&&f.coil.getInventory().getStackInSlot(0).isEmpty(),"original output promise completes once despite lower current config");
            }));
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=80)
    public static void legacyLockedCrystalBatchUsesHistoricalFeAndOutput(GameTestHelper h){
        withCoil(h,f->{
            prepare(h,f,1,500_000,true);ticks(f,4);var saved=f.coil.saveWithoutMetadata(h.getLevel().registryAccess());
            saved.remove("OverloadSimTranscendentTotalFe");saved.remove("OverloadSimTranscendentOutputEhv");
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_FE,5L,()->
            SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.TESLA_CRYSTAL_EHV,1L,()->{
                f.coil.loadTag(saved,h.getLevel().registryAccess());
                h.assertTrue(f.coil.getCurrentTotalEnergy()==500_000&&f.coil.getConsumedEnergy()==400_000,"legacy locked job restores historical fees independently of current values");
                tick(f);h.assertTrue(f.storage.extreme==64&&!f.coil.hasLockedMode(),"legacy job completes historical output exactly once");
                prepare(h,f,1,5,true);ticks(f,5);h.assertTrue(f.storage.extreme==65&&!f.coil.hasLockedMode(),"minimum five-FE new job advances all five native processing steps");
            }));
        });
    }
    private record Fixture(TeslaCoilBlockEntity coil, LightningStorage storage) {}

    private static ItemStack crystal(GameTestHelper h, int count) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("overload_sim:transcendent_crystal"));
        h.assertTrue(item != Items.AIR, "transcendent crystal is a real registered item");
        return new ItemStack(item, count);
    }

    private static TeslaCoilBlockEntity place(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:tesla_coil"));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (TeslaCoilBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void withCoil(GameTestHelper h, Consumer<Fixture> test) {
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        var coil = place(h, pos);
        h.getLevel().setBlockAndUpdate(pos.below(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAtTickTime(40, () -> {
            var grid = coil.getMainNode().getGrid();
            h.assertTrue(grid != null && coil.getMainNode().isActive(), "native Tesla joins a powered real AE grid");
            var storage = new LightningStorage();
            grid.getStorageService().addGlobalStorageProvider(mounts -> mounts.mount(storage, 0));
            test.accept(new Fixture(coil, storage));
            h.succeed();
        });
    }

    private static void prepare(GameTestHelper h, Fixture f, int crystals, int fe, boolean matrix) {
        f.coil.getInventory().setStackInSlot(TeslaCoilInventory.SLOT_DUST, crystal(h, crystals));
        if (matrix) f.coil.getInventory().setStackInSlot(TeslaCoilInventory.SLOT_MATRIX, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack());
        f.coil.getEnergyStorage().receiveEnergy(fe, false);
    }

    private static TickRateModulation tick(Fixture f) {
        return f.coil.getLogic().tickingRequest(f.coil.getMainNode().getNode(), 1);
    }

    private static void ticks(Fixture f, int count) {
        for (int i = 0; i < count; i++) tick(f);
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void nativeInputAutomationAndMenuAcceptTranscendent(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        var coil = place(h, pos);
        var offered = crystal(h, 3);
        h.assertTrue(coil.getAutomationInventory().insertItem(0, offered, true).isEmpty()
                        && coil.getInventory().getStackInSlot(0).isEmpty(),
                "automation simulation accepts crystal without mutation");
        h.assertTrue(coil.getAutomationInventory().insertItem(0, offered, false).isEmpty()
                        && coil.getInventory().getStackInSlot(0).getCount() == 3,
                "native input handler actually accepts crystal");
        h.assertTrue(!coil.getAutomationInventory().insertItem(1, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack(), false).isEmpty()
                        && coil.getAutomationInventory().extractItem(0, 1, false).isEmpty(),
                "matrix remains manual-only and native automation cannot extract fuel");
        coil.getInventory().setStackInSlot(0, ItemStack.EMPTY);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "transcendent-tesla-menu"));
        player.getInventory().clearContent();
        player.setPos(pos.getCenter());
        var menu = new TeslaCoilMenu(17, player.getInventory(), coil);
        player.containerMenu = menu;
        player.getInventory().setItem(0, crystal(h, 2));
        int hotbar = menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).getFirst().index;
        h.assertTrue(menu.slots.getFirst().mayPlace(crystal(h, 1)), "native GUI fuel slot accepts crystal");
        menu.quickMoveStack(player, hotbar);
        h.assertTrue(coil.getInventory().getStackInSlot(0).getCount() == 2 && player.getInventory().getItem(0).isEmpty(),
                "native shift-click transfers real crystals from player inventory");
        menu.setCarried(crystal(h, 1));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(coil.getInventory().getStackInSlot(0).getCount() == 3 && menu.getCarried().isEmpty(),
                "native cursor click inserts crystal through the GUI backing inventory");
        h.succeed();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void missingCollapseMatrixNeverChargesOrConsumesCrystal(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, false);
            ticks(f, 5);
            h.assertTrue(f.coil.getEnergyStorage().getStoredEnergyLong() == 500_000
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1
                            && f.storage.extreme == 0 && f.storage.high == 0 && !f.coil.hasLockedMode(),
                    "missing real collapse matrix prevents all crystal, FE and lightning transactions");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void nonMatrixItemLoadedIntoMatrixSlotCannotAuthorizeCrystalRecipe(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, false);
            // Legacy save loading is intentionally unchecked upstream; require the actual module item.
            f.coil.getInventory().setItemDirect(1, new ItemStack(Items.DIAMOND));
            ticks(f, 5);
            h.assertTrue(!f.coil.hasLockedMode() && f.coil.getEnergyStorage().getStoredEnergyLong() == 500_000
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1 && f.storage.extreme == 0,
                    "an arbitrary nonempty matrix slot cannot replace the installed collapse matrix");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void incompleteFePaymentPausesUntilExactRemainingEnergyArrives(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 499_999, true);
            ticks(f, 5);
            h.assertTrue(f.coil.getProcessingTicksSpent() == 4 && f.coil.getConsumedEnergy() == 400_000
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 99_999
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1 && f.storage.extreme == 0,
                    "499999 FE pays four 100000-FE steps but cannot emit lightning or consume crystal");
            f.coil.getEnergyStorage().receiveEnergy(1, false);
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty()
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "one remaining FE completes the existing batch exactly once");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void fiveNativeTicksConsumeOneCrystalAnd500000FeFor64Ehv(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 2, 500_000, true);
            ticks(f, 4);
            h.assertTrue(f.coil.getProcessingTicksSpent() == 4 && f.coil.getConsumedEnergy() == 400_000
                            && f.coil.getCurrentTotalEnergy() == 500_000
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 100_000
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 2 && f.storage.extreme == 0,
                    "four native processing ticks retain both crystals and produce no premature output");
            var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "transcendent-tesla-progress"));
            var menu = new TeslaCoilMenu(18, player.getInventory(), f.coil);
            player.containerMenu = menu;
            menu.broadcastChanges();
            h.assertTrue(menu.getTotalEnergy() == 500_000 && menu.getConsumedEnergy() == 400_000
                            && menu.getProgress() == 0.8D && menu.isMatrixInstalled(),
                    "native menu reports the crystal recipe's actual FE cost and charged progress");
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.storage.high == 0
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1
                            && f.coil.getInventory().getStackInSlot(1).is(ModItems.LIGHTNING_COLLAPSE_MATRIX.get())
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0 && !f.coil.hasLockedMode(),
                    "fifth native tick consumes one crystal and 500000 total FE for exactly 64 EHV, preserving matrix");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void extremeSelectedModeUsesCrystalWithoutHighVoltageNetworkFuel(GameTestHelper h) {
        withCoil(h, f -> {
            f.coil.cycleMode();
            h.assertTrue(f.coil.getSelectedMode() == TeslaCoilMode.EXTREME_HIGH_VOLTAGE, "native mode switch chooses EHV");
            prepare(h, f, 1, 500_000, true);
            ticks(f, 5);
            h.assertTrue(f.storage.high == 0 && f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty(),
                    "crystal recipe emits 64 EHV in either mode without the ordinary HV conversion input");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void legacyStackedMatricesDoNotMultiplyCrystalCostOrOutput(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 2, 500_000, false);
            f.coil.getInventory().setItemDirect(1, new ItemStack(ModItems.LIGHTNING_COLLAPSE_MATRIX.get(), 32));
            ticks(f, 5);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).getCount() == 1
                            && f.coil.getInventory().getStackInSlot(1).getCount() == 32
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "legacy matrix stacks remain an installed gate for exactly one fixed crystal recipe");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void outputBelow64CapacityPreventsStartingAndResumesWhenCapacityReturns(GameTestHelper h) {
        withCoil(h, f -> {
            f.storage.extremeCapacity = 63;
            prepare(h, f, 1, 500_000, true);
            h.assertTrue(tick(f) == TickRateModulation.SLOWER && !f.coil.hasLockedMode()
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 500_000
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1 && f.storage.extreme == 0,
                    "fewer than 64 output spaces retain all resources and keep native polling awake");
            f.storage.extremeCapacity = 64;
            ticks(f, 5);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty(),
                    "capacity returning lets the same untouched input finish");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void outputFillsDuringChargeRetainsCrystalAndDoesNotPayAgainOnRetry(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            ticks(f, 4);
            f.storage.extremeCapacity = 0;
            ticks(f, 3);
            h.assertTrue(f.coil.isReadyToCommit() && f.coil.getConsumedEnergy() == 500_000
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1 && f.storage.extreme == 0,
                    "completed native charge waits for network without consuming input or repeating FE cost");
            f.storage.extremeCapacity = 64;
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty()
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "charged batch commits once after capacity returns without needing another FE payment");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void shortActualNetworkInsertionRollsBackWithoutLosingFuelOrDuplicatingOutput(GameTestHelper h) {
        withCoil(h, f -> {
            f.storage.shortInsert = true;
            prepare(h, f, 1, 500_000, true);
            ticks(f, 5);
            h.assertTrue(f.coil.isReadyToCommit() && f.coil.getInventory().getStackInSlot(0).getCount() == 1
                            && f.storage.extreme == 0 && f.coil.getConsumedEnergy() == 500_000,
                    "short actual output insertion is undone, leaving the charged batch and crystal intact");
            f.storage.shortInsert = false;
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty(),
                    "network retry produces only the promised 64 EHV without duplicated partial output");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void insertionOnlyPartialOutputPersistsDebtAndConsumesOnlyOneCrystal(GameTestHelper h) {
        withCoil(h, f -> {
            f.storage.shortInsert = true;
            f.storage.refuseExtremeExtraction = true;
            prepare(h, f, 1, 500_000, true);
            ticks(f, 5);
            h.assertTrue(f.coil.isReadyToCommit() && f.storage.extreme == 17
                            && f.coil.getInventory().getStackInSlot(0).isEmpty()
                            && f.coil.getConsumedEnergy() == 500_000,
                    "irreversible partial output owns one paid crystal and retains the outstanding batch");
            var saved = f.coil.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored = new TeslaCoilBlockEntity(f.coil.getBlockPos(), f.coil.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadTag(saved, h.getLevel().registryAccess());
            h.assertTrue(restored.isReadyToCommit() && restored.hasLockedModeLocalPrerequisites()
                            && restored.getCurrentTotalEnergy() == 500_000,
                    "a fresh loaded host remembers the paid input even though its input slot is empty");
            f.coil.loadTag(saved, h.getLevel().registryAccess());
            f.coil.getInventory().setStackInSlot(1, ItemStack.EMPTY);
            tick(f);
            h.assertTrue(f.storage.extreme == 17, "outstanding partial output still requires the installed matrix");
            f.coil.getInventory().setStackInSlot(1, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack());
            f.storage.shortInsert = false;
            f.storage.extremeCapacity = 64;
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && !f.coil.hasLockedMode()
                            && f.coil.getInventory().getStackInSlot(0).isEmpty()
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "after reload insertion-only retry delivers only the remaining 47 EHV without another crystal or FE");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void replacedRefundSlotRetainsOwnedCrystalAcrossReloadAndRetry(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            f.storage.shortInsert = true;
            f.storage.onActualInsert = () -> f.coil.getInventory().setStackInSlot(0,
                    new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            ticks(f, 5);
            h.assertTrue(f.storage.extreme == 0 && f.coil.isReadyToCommit()
                            && f.coil.hasLockedModeLocalPrerequisites()
                            && f.coil.getInventory().getStackInSlot(0).is(ModItems.OVERLOAD_CRYSTAL_DUST.get()),
                    "reversed output retains the owned crystal when a callback replaces its refund destination");
            var saved = f.coil.saveWithoutMetadata(h.getLevel().registryAccess());
            f.coil.loadTag(saved, h.getLevel().registryAccess());
            f.storage.shortInsert = false;
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).getCount() == 2
                            && f.coil.getInventory().getStackInSlot(0).is(ModItems.OVERLOAD_CRYSTAL_DUST.get())
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "reserved crystal survives reload and funds retry without consuming replacement dust");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void destroyingBatchReturnsUndeliveredReservedCrystalOnce(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            f.storage.shortInsert = true;
            f.storage.onActualInsert = () -> f.coil.getInventory().setStackInSlot(0,
                    new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            ticks(f, 5);
            h.getLevel().destroyBlock(f.coil.getBlockPos(), true);
            h.assertTrue(f.storage.extreme == 0 && droppedCrystals(h, f.coil.getBlockPos()) == 1,
                    "normal native block drops return the one undelivered crystal held outside the replaced input slot");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void simulationAndActualCommitCallbacksCannotReenterChargedBatch(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 2, 500_000, true);
            var rejected = new int[1];
            f.storage.onSimulation = () -> {
                if (!f.coil.isReadyToCommit()) return;
                f.storage.onSimulation = null;
                if (!f.coil.commitLockedMode()) rejected[0]++;
            };
            f.storage.onActualInsert = () -> { if (!f.coil.commitLockedMode()) rejected[0]++; };
            ticks(f, 5);
            h.assertTrue(rejected[0] == 2 && f.storage.extreme == 64
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 1
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "both capacity-probe and actual-write callbacks reject reentry, preserving exactly one paid batch");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void simulationRemovingCoilCannotCommitThroughDetachedTile(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            f.storage.onSimulation = () -> {
                if (!f.coil.isReadyToCommit()) return;
                f.storage.onSimulation = null;
                h.getLevel().destroyBlock(f.coil.getBlockPos(), true);
            };
            ticks(f, 5);
            h.assertTrue(f.coil.isRemoved() && f.storage.extreme == 0
                            && !f.coil.commitLockedMode() && droppedCrystals(h, f.coil.getBlockPos()) == 1,
                    "a capacity-probe removal cannot extract fuel or emit output from the detached native host");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void actualInsertionRemovingCoilNeverRefundsAlreadyDeliveredCrystal(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            f.storage.shortInsert = true;
            f.storage.onActualInsert = () -> h.getLevel().destroyBlock(f.coil.getBlockPos(), true);
            ticks(f, 5);
            h.assertTrue(f.coil.isRemoved() && f.storage.extreme == 17
                            && !f.coil.commitLockedMode() && droppedCrystals(h, f.coil.getBlockPos()) == 0,
                    "actual insertion removal leaves the paid partial output without refunding or retrying its crystal");
        });
    }

    private static int droppedCrystals(GameTestHelper h, BlockPos pos) {
        var item = crystal(h, 1).getItem();
        return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
                .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void removingMatrixDuringChargePausesAndRestoringItResumesSameBatch(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            ticks(f, 2);
            f.coil.getInventory().setStackInSlot(1, ItemStack.EMPTY);
            ticks(f, 4);
            h.assertTrue(f.coil.getProcessingTicksSpent() == 2 && f.coil.getConsumedEnergy() == 200_000
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 300_000 && f.storage.extreme == 0,
                    "matrix remains mandatory throughout the charged batch");
            f.coil.getInventory().setStackInSlot(1, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack());
            ticks(f, 3);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getInventory().getStackInSlot(0).isEmpty(),
                    "restoring the matrix resumes the remaining three native steps");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void chargedCrystalBatchSurvivesSaveAndRejectsReplacementDust(GameTestHelper h) {
        withCoil(h, f -> {
            prepare(h, f, 1, 500_000, true);
            ticks(f, 4);
            f.coil.getInventory().setStackInSlot(0, new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            var saved = f.coil.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored = new TeslaCoilBlockEntity(f.coil.getBlockPos(), f.coil.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadTag(saved, h.getLevel().registryAccess());
            h.assertTrue(restored.hasLockedMode() && restored.getCurrentTotalEnergy() == 500_000
                            && restored.getConsumedEnergy() == 400_000 && !restored.hasLockedModeLocalPrerequisites(),
                    "saved crystal batch keeps its recipe and rejects dust substituted before reload");
            f.coil.loadTag(saved, h.getLevel().registryAccess());
            tick(f);
            h.assertTrue(f.coil.getConsumedEnergy() == 400_000 && f.storage.extreme == 0
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 2,
                    "reload cannot reinterpret the charged crystal batch as a dust recipe");
            f.coil.getInventory().setStackInSlot(0, crystal(h, 1));
            tick(f);
            h.assertTrue(f.storage.extreme == 64 && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "restoring crystal after reload completes only the remaining native charge step");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void originalDustAndHighVoltageConversionKeepTheirNativeCostsAndOutput(GameTestHelper h) {
        withCoil(h, f -> {
            f.coil.getInventory().setStackInSlot(0, new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            f.coil.getEnergyStorage().receiveEnergy(25_000, false);
            ticks(f, 5);
            h.assertTrue(f.storage.high == 1 && f.storage.extreme == 0
                            && f.coil.getInventory().getStackInSlot(0).isEmpty()
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "ordinary dust still consumes two dust and 25000 FE to create one HV");
            f.storage.high = 8;
            f.coil.cycleMode();
            f.coil.getInventory().setStackInSlot(1, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack());
            f.coil.getEnergyStorage().receiveEnergy(500_000, false);
            ticks(f, 5);
            h.assertTrue(f.storage.high == 0 && f.storage.extreme == 1
                            && f.coil.getEnergyStorage().getStoredEnergyLong() == 0,
                    "ordinary EHV conversion still consumes eight HV and 500000 FE to create one EHV");
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 80)
    public static void originalDustBatchCannotConsumeCrystalSubstitutedDuringCharge(GameTestHelper h) {
        withCoil(h, f -> {
            f.coil.getInventory().setStackInSlot(0, new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            f.coil.getEnergyStorage().receiveEnergy(25_000, false);
            ticks(f, 4);
            f.coil.getInventory().setStackInSlot(0, crystal(h, 2));
            f.coil.getInventory().setStackInSlot(1, ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack());
            tick(f);
            h.assertTrue(f.coil.getConsumedEnergy() == 20_000 && f.coil.getCurrentTotalEnergy() == 25_000
                            && f.coil.getInventory().getStackInSlot(0).getCount() == 2
                            && f.storage.high == 0 && f.storage.extreme == 0,
                    "inventory acceptance never lets an already-locked ordinary dust batch burn crystals");
            f.coil.getInventory().setStackInSlot(0, new ItemStack(ModItems.OVERLOAD_CRYSTAL_DUST.get(), 2));
            tick(f);
            h.assertTrue(f.storage.high == 1 && f.storage.extreme == 0,
                    "restoring native dust resumes its original batch without reinterpreting it");
        });
    }

    private static final class LightningStorage implements MEStorage {
        long high;
        long extreme;
        long extremeCapacity = 1_024;
        boolean shortInsert;
        boolean refuseExtremeExtraction;
        Runnable onActualInsert;
        Runnable onSimulation;

        @Override public Component getDescription() { return Component.literal("Tesla output backpressure fixture"); }
        @Override public void getAvailableStacks(KeyCounter out) {
            if (high > 0) out.add(LightningKey.HIGH_VOLTAGE, high);
            if (extreme > 0) out.add(LightningKey.EXTREME_HIGH_VOLTAGE, extreme);
        }
        @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
            if (LightningKey.HIGH_VOLTAGE.equals(key)) {
                if (action == Actionable.MODULATE) high += amount;
                return amount;
            }
            if (!LightningKey.EXTREME_HIGH_VOLTAGE.equals(key)) return 0;
            if (action == Actionable.SIMULATE && onSimulation != null) onSimulation.run();
            long accepted = Math.min(amount, Math.max(0, extremeCapacity - extreme));
            if (action == Actionable.MODULATE) {
                if (shortInsert) accepted = Math.min(accepted, 17);
                extreme += accepted;
                if (onActualInsert != null) {
                    var callback = onActualInsert;
                    onActualInsert = null;
                    callback.run();
                }
            }
            return accepted;
        }
        @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
            if (LightningKey.HIGH_VOLTAGE.equals(key)) {
                long taken = Math.min(amount, high);
                if (action == Actionable.MODULATE) high -= taken;
                return taken;
            }
            if (!LightningKey.EXTREME_HIGH_VOLTAGE.equals(key)) return 0;
            if (refuseExtremeExtraction) return 0;
            long taken = Math.min(amount, extreme);
            if (action == Actionable.MODULATE) extreme -= taken;
            return taken;
        }
    }
}
