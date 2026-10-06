package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageCells;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.blockentity.AdvancedWirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.network.FrequencyCardUsePacket;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.compat.LightningNetwork;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlock;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.machine.SimulationChamberBlockEntity;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises the addon boundary through AE2LT's real card action, grid and lightning cell. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class InstrumentNetworkGameTests {
    @GameTest(template = "laser_empty", timeoutTicks = 130)
    public static void everyLaserKindHasPersistentNativeCardBinding(GameTestHelper h) {
        var source = source(h, h.absolutePos(new BlockPos(3, 2, 3)));
        var machines = new ArrayList<LaserMachineBlockEntity>();
        for (var kind : LaserKind.values()) {
            int index = machines.size();
            var pos = h.absolutePos(new BlockPos(8 + index % 4 * 3, 2, 9 + index / 4 * 3));
            h.getLevel().setBlockAndUpdate(pos, LaserContent.machine(kind).defaultBlockState());
            machines.add((LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos));
        }
        h.runAtTickTime(30, () -> {
            for (var machine : machines) {
                useCard(h, source, machine);
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).orElse(-1) == source.frequency(),
                        machine.kind() + " card action binds the machine's own frequency settings");
                h.assertTrue(machine.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("FrequencyId") == source.frequency(),
                        machine.kind() + " saves its bound frequency with the block entity");
            }
        });
        h.runAtTickTime(85, () -> {
            for (var machine : machines) {
                assertWirelessChannel(h, source, machine);
                h.assertTrue(machine.bridge().extractExtreme(1, false) == 1,
                        machine.kind() + " can transact EHV through the overloaded controller network");
                var settings = machine.exportSettings(SettingsFrom.MEMORY_CARD, source.player());
                useCard(h, source, machine);
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).isEmpty(),
                        machine.kind() + " second card action clears its own binding");
                machine.importSettings(SettingsFrom.MEMORY_CARD, settings, source.player());
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).orElse(-1) == source.frequency(),
                        machine.kind() + " restores the saved native frequency through an AE memory card");
                useCard(h, source, machine);
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).isEmpty(),
                        machine.kind() + " can clear a memory-card-restored native binding");
            }
        });
        h.runAtTickTime(110, () -> {
            for (var machine : machines) {
                h.assertTrue(machine.getMainNode().getGrid() != source.controller().getMainNode().getGrid(),
                        machine.kind() + " clearing a native binding releases the wireless grid connection");
            }
            cleanup(h, source);
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 110)
    public static void bothSimulationChambersUseTheOverloadedFrequencyNetwork(GameTestHelper h) {
        var source = source(h, h.absolutePos(new BlockPos(3, 2, 3)));
        var smallPos = h.absolutePos(new BlockPos(9, 2, 9));
        var largePos = h.absolutePos(new BlockPos(13, 2, 9));
        h.getLevel().setBlockAndUpdate(smallPos, ModContent.CHAMBER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(largePos, MultiblockContent.CONTROLLER.get().defaultBlockState());
        var small = (SimulationChamberBlockEntity) h.getLevel().getBlockEntity(smallPos);
        var large = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(largePos);
        h.runAtTickTime(30, () -> {
            for (var machine : List.of(small, large)) {
                useCard(h, source, machine);
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).orElse(-1) == source.frequency(),
                        "both simulation chamber types expose the card binding to their settings UI");
            }
        });
        h.runAtTickTime(85, () -> {
            assertWirelessChannel(h, source, small);
            assertWirelessChannel(h, source, large);
            h.assertTrue(LightningNetwork.extract(small, 10, false) == 10 && large.bridge().extract(false, 20, false) == 20,
                    "single-block and multiblock chambers consume HV from the same real network");
            h.assertTrue(source.controller().getMainNode().getGrid().getStorageService().getInventory()
                            .extract(LightningKey.HIGH_VOLTAGE, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty()) == 970,
                    "native connections debit exactly the requested HV");
            cleanup(h, source);
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 180)
    public static void laserNativeBindingReloadsAndRecreatesItsWirelessChannel(GameTestHelper h) {
        var source = source(h, h.absolutePos(new BlockPos(3, 2, 3)));
        var pos = h.absolutePos(new BlockPos(12, 2, 12));
        var original = LaserMachineGameTests.build(h, pos, LaserKind.BURST, Direction.NORTH);
        original.checkStructure();
        original.setEnabled(false);
        var restored = new LaserMachineBlockEntity[1];
        h.runAtTickTime(30, () -> {
            useCard(h, source, original);
            h.assertTrue(FrequencyApi.getBoundFrequencyId(original).orElse(-1) == source.frequency(),
                    "card binding belongs to the burst matrix before reload");
        });
        h.runAtTickTime(85, () -> {
            assertWirelessChannel(h, source, original);
            h.assertTrue(original.bridge().extractExtreme(4, false) == 4, "original wireless channel transacts EHV");
            var saved = original.saveWithoutMetadata(h.getLevel().registryAccess());
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, LaserContent.machine(LaserKind.BURST).defaultBlockState().setValue(LaserMachineBlock.FACING, Direction.NORTH));
            restored[0] = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            restored[0].loadTag(saved, h.getLevel().registryAccess());
            restored[0].checkStructure();
            h.assertTrue(FrequencyApi.getBoundFrequencyId(restored[0]).orElse(-1) == source.frequency(),
                    "restored machine retains the persisted native frequency");
        });
        h.runAtTickTime(150, () -> {
            var machine = restored[0];
            assertWirelessChannel(h, source, machine);
            h.assertTrue(machine.formed() && machine.tryPay(0, 64, false),
                    "reloaded burst matrix can pay its EHV cost using the recreated native channel");
            h.assertTrue(machine.bridge().extractExtreme(Long.MAX_VALUE, true) == 932,
                    "reloading neither loses nor duplicates the prior four EHV debit and new 64 EHV payment");
            cleanup(h, source);
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 180)
    public static void unloadedInstrumentsRemainInTheOfflineFrequencyDeviceList(GameTestHelper h) {
        var source = source(h, h.absolutePos(new BlockPos(3, 2, 3)));
        var machines = new ArrayList<AENetworkedBlockEntity>();
        var restoredMachines = new ArrayList<AENetworkedBlockEntity>();
        for (var kind : LaserKind.values()) {
            int index = machines.size();
            var pos = h.absolutePos(new BlockPos(8 + index % 4 * 3, 2, 9 + index / 4 * 3));
            h.getLevel().setBlockAndUpdate(pos, LaserContent.machine(kind).defaultBlockState());
            machines.add((AENetworkedBlockEntity) h.getLevel().getBlockEntity(pos));
        }
        var smallPos = h.absolutePos(new BlockPos(8, 2, 15));
        var largePos = h.absolutePos(new BlockPos(11, 2, 15));
        h.getLevel().setBlockAndUpdate(smallPos, ModContent.CHAMBER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(largePos, MultiblockContent.CONTROLLER.get().defaultBlockState());
        machines.add((AENetworkedBlockEntity) h.getLevel().getBlockEntity(smallPos));
        machines.add((AENetworkedBlockEntity) h.getLevel().getBlockEntity(largePos));
        h.runAtTickTime(30, () -> {
            for (var machine : machines) useCard(h, source, machine);
        });
        h.runAtTickTime(85, () -> {
            var manager = WirelessFrequencyManager.get();
            var transmitterNode = source.controller().getMainNode().getNode();
            var unloadedNodes = new HashMap<AENetworkedBlockEntity, appeng.api.networking.IGridNode>();
            var savedTags = new HashMap<AENetworkedBlockEntity, net.minecraft.nbt.CompoundTag>();
            var isolatedChunks = new HashMap<ChunkPos, LevelChunk>();
            for (var machine : machines) {
                assertWirelessChannel(h, source, machine);
                savedTags.put(machine, machine.saveWithFullMetadata(h.getLevel().registryAccess()));
                unloadedNodes.put(machine, machine.getMainNode().getNode());
                var chunkPos = new ChunkPos(machine.getBlockPos());
                isolatedChunks.computeIfAbsent(chunkPos, p -> new LevelChunk(h.getLevel(), p))
                        .getBlockEntities().put(machine.getBlockPos(), machine);
            }
            // Execute NeoForge's real unload sequence: onChunkUnloaded for every entity, then setRemoved.
            // Separate chunk containers isolate these live hosts from the transmitter and other GameTests.
            for (var chunk : isolatedChunks.values()) {
                chunk.clearAllBlockEntities();
                h.assertTrue(chunk.getBlockEntities().isEmpty(), "the real chunk unload clears its block entity map");
            }
            for (var machine : machines) {
                var unloadedNode = unloadedNodes.get(machine);
                h.assertTrue(machine.isRemoved() && machine.getMainNode().getNode() == null && unloadedNode.getConnections().isEmpty()
                                && transmitterNode.getConnections().stream().noneMatch(c -> c.getOtherSide(transmitterNode) == unloadedNode),
                        machine.getBlockState().getBlock().getDescriptionId() + " full chunk unload releases the node and its wireless connection");
                h.assertTrue(FrequencyApi.getBoundFrequencyId(machine).orElse(-1) == source.frequency()
                                && manager.getDevices(source.frequency()).stream().anyMatch(entry ->
                                    entry.dimension().equals(h.getLevel().dimension()) && entry.pos().equals(machine.getBlockPos())),
                        machine.getBlockState().getBlock().getDescriptionId() + " remains a bound offline device after the complete chunk unload sequence");
                // Match LevelChunk's pending-NBT promotion. AE2 managed nodes can only be initialized once.
                var restored = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(),
                        savedTags.get(machine), h.getLevel().registryAccess());
                h.assertTrue(restored instanceof AENetworkedBlockEntity && restored != machine,
                        "normal chunk reload constructs a fresh network host from the saved block entity tag");
                restored.setLevel(h.getLevel());
                h.getLevel().getChunkAt(machine.getBlockPos()).addAndRegisterBlockEntity(restored);
                restoredMachines.add((AENetworkedBlockEntity) restored);
            }
        });
        h.runAtTickTime(150, () -> {
            var manager = WirelessFrequencyManager.get();
            for (var machine : restoredMachines) {
                assertWirelessChannel(h, source, machine);
                h.assertTrue(!machine.isRemoved() && FrequencyApi.getBoundFrequencyId(machine).orElse(-1) == source.frequency(),
                        "fresh chunk registration restores the persisted instrument binding");
                h.getLevel().setBlockAndUpdate(machine.getBlockPos(), Blocks.AIR.defaultBlockState());
                h.assertTrue(machine.isRemoved() && machine.getMainNode().getNode() == null,
                        "actual in-world deletion releases the restored instrument node");
                h.assertTrue(manager.getDevices(source.frequency()).stream().noneMatch(entry ->
                                entry.dimension().equals(h.getLevel().dimension()) && entry.pos().equals(machine.getBlockPos())),
                        machine.getBlockState().getBlock().getDescriptionId() + " actual in-world deletion after reload unregisters the device");
            }
            cleanup(h, source);
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 30)
    public static void pendingInstrumentLoadResetsItsUnloadRemovalReason(GameTestHelper h) {
        var source = source(h, h.absolutePos(new BlockPos(3, 2, 3)));
        var states = new ArrayList<BlockState>();
        for (var kind : LaserKind.values()) states.add(LaserContent.machine(kind).defaultBlockState());
        states.add(ModContent.CHAMBER.get().defaultBlockState());
        states.add(MultiblockContent.CONTROLLER.get().defaultBlockState());
        for (int index = 0; index < states.size(); index++) {
            var pos = h.absolutePos(new BlockPos(8 + index % 4 * 3, 2, 9 + index / 4 * 3));
            var state = states.get(index);
            h.getLevel().setBlockAndUpdate(pos, state);
            var pending = (AENetworkedBlockEntity) h.getLevel().getBlockEntity(pos);
            ((FrequencyBindingHost) pending).getFrequencyBindingAccess().setFrequency(source.frequency());
            h.assertTrue(pending.getMainNode().getNode() == null && deviceListed(h, source, pending),
                    "a bound pending host has not reached its first AE node initialization");
            pending.onChunkUnloaded();
            pending.setRemoved();
            h.assertTrue(deviceListed(h, source, pending), "early unload keeps the pending device offline");
            h.getLevel().getChunkAt(pos).addAndRegisterBlockEntity(pending);
            h.assertTrue(!pending.isRemoved() && pending.getMainNode().getNode() == null,
                    "clearRemoved registers the still-uninitialized host before its first tick");
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.assertTrue(pending.isRemoved() && !deviceListed(h, source, pending),
                    state.getBlock().getDescriptionId() + " clearRemoved resets unload state before an immediate real block deletion");

            // Exercise onLoad independently while the new host still awaits its first AE initialization.
            h.getLevel().setBlockAndUpdate(pos, state);
            var loading = (AENetworkedBlockEntity) h.getLevel().getBlockEntity(pos);
            ((FrequencyBindingHost) loading).getFrequencyBindingAccess().setFrequency(source.frequency());
            loading.onChunkUnloaded();
            loading.onLoad();
            h.assertTrue(loading.getMainNode().getNode() == null && deviceListed(h, source, loading),
                    "onLoad precedes the pending host's first AE initialization and preserves its binding");
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.assertTrue(loading.isRemoved() && !deviceListed(h, source, loading),
                    state.getBlock().getDescriptionId() + " onLoad resets unload state before an immediate real block deletion");
        }
        // AE2's first-tick queue skips removed hosts; no destroyed initialized node is ever recreated.
        h.runAtTickTime(5, () -> { cleanup(h, source); h.succeed(); });
    }

    @GameTest(template = "laser_empty", timeoutTicks = 110)
    public static void shellInterfaceKeepsItsExistingWiredOverloadedConnection(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(12, 2, 12));
        var machine = LaserMachineGameTests.build(h, pos, LaserKind.CONSTRAINT, Direction.NORTH);
        machine.setEnabled(false);
        var port = pos.above(3).east(3);
        h.getLevel().setBlockAndUpdate(port, block("ae2lt:overloaded_interface").defaultBlockState());
        var controllerPos = port.east();
        h.getLevel().setBlockAndUpdate(controllerPos, block("ae2lt:overloaded_controller").defaultBlockState());
        h.getLevel().setBlockAndUpdate(controllerPos.above(), block("ae2:creative_energy_cell").defaultBlockState());
        installCell(h, controllerPos.east());
        machine.checkStructure();
        h.runAtTickTime(85, () -> {
            var controller = (OverloadedControllerBlockEntity) h.getLevel().getBlockEntity(controllerPos);
            String stage = "checking the live wired network";
            try {
            h.assertTrue(machine.formed() && machine.networkActive()
                            && machine.getMainNode().getGrid() == controller.getMainNode().getGrid(),
                    "formed shell overloaded interface joins the existing wired controller grid");
            stage = "extracting EHV from the live wired network";
            h.assertTrue(machine.bridge().extractExtreme(64, false) == 64,
                    "the existing shell-interface path still supplies EHV");
            stage = "removing the overloaded interface block";
            h.getLevel().setBlockAndUpdate(port, Blocks.AIR.defaultBlockState());
            stage = "validating the broken shell and releasing its owned connection";
            machine.checkStructure();
            stage = "checking the disconnected grid";
            h.assertTrue(!machine.formed() && machine.getMainNode().getGrid() != controller.getMainNode().getGrid()
                            && machine.bridge().extractExtreme(1, true) == 0,
                    "breaking the sole interface removes the owned bridge immediately");
            h.succeed();
            } catch (RuntimeException error) {
                org.slf4j.LoggerFactory.getLogger(InstrumentNetworkGameTests.class)
                        .error("Shell-interface regression failed while {}", stage, error);
                throw error;
            }
        });
    }

    private static Source source(GameTestHelper h, BlockPos pos) {
        var player = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "instrument-network-test"));
        var manager = WirelessFrequencyManager.get();
        h.assertTrue(manager != null, "AE2LT initializes its real server frequency manager");
        var frequency = manager.createFrequency(player, "Instrument test", 0xEBC0EF, FrequencySecurityLevel.PUBLIC, "");
        h.assertTrue(frequency != null, "test frequency creation succeeds");
        h.getLevel().setBlockAndUpdate(pos, block("ae2lt:advanced_wireless_overloaded_controller").defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.west(), block("ae2:creative_energy_cell").defaultBlockState());
        installCell(h, pos.east());
        var controller = (AdvancedWirelessOverloadedControllerBlockEntity) h.getLevel().getBlockEntity(pos);
        controller.setFrequency(frequency.getId());
        var card = new ItemStack(blockItem("ae2lt:overloaded_frequency_card"));
        OverloadedFrequencyCardItem.bindFrequency(card, frequency.getId(), h.getLevel().dimension(), pos, player.getUUID());
        return new Source(controller, frequency.getId(), player, card);
    }

    @GameTest(template = "laser_empty", timeoutTicks = 110)
    public static void removingTheSimulationShellInterfaceReleasesOnlyItsOwnedConnection(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(8, 2, 8));
        var machine = MultiblockGameTests.build(h, min, 3);
        var port = min.south();
        h.getLevel().setBlockAndUpdate(port, block("ae2lt:overloaded_interface").defaultBlockState());
        var controllerPos = port.west();
        h.getLevel().setBlockAndUpdate(controllerPos, block("ae2lt:overloaded_controller").defaultBlockState());
        h.getLevel().setBlockAndUpdate(controllerPos.below(), block("ae2:creative_energy_cell").defaultBlockState());
        installCell(h, controllerPos.west());
        machine.checkStructure();
        h.runAtTickTime(85, () -> {
            var controller = (OverloadedControllerBlockEntity) h.getLevel().getBlockEntity(controllerPos);
            h.assertTrue(machine.structure() != null && machine.getMainNode().isActive()
                            && machine.getMainNode().getGrid() == controller.getMainNode().getGrid(),
                    "simulation shell interface joins the real overloaded controller network");
            h.assertTrue(machine.bridge().extract(false, 20, false) == 20,
                    "simulation shell interface supplies the exact requested HV");
            h.getLevel().setBlockAndUpdate(port, Blocks.AIR.defaultBlockState());
            machine.checkStructure();
            h.assertTrue(machine.structure() == null && machine.getMainNode().getGrid() != controller.getMainNode().getGrid()
                            && machine.bridge().extract(false, 1, true) == 0,
                    "removed interface invalidates the chamber and releases its already-detached owned edge");
            h.succeed();
        });
    }

    private static void installCell(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos, block("ae2:drive").defaultBlockState());
        var drive = (DriveBlockEntity) h.getLevel().getBlockEntity(pos);
        var cell = new ItemStack(com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.get());
        var inventory = StorageCells.getCellInventory(cell, drive::saveChanges);
        inventory.insert(LightningKey.HIGH_VOLTAGE, 1000, Actionable.MODULATE, IActionSource.empty());
        inventory.insert(LightningKey.EXTREME_HIGH_VOLTAGE, 1000, Actionable.MODULATE, IActionSource.empty());
        inventory.persist();
        drive.getInternalInventory().setItemDirect(0, cell);
    }

    private static void useCard(GameTestHelper h, Source source, AENetworkedBlockEntity machine) {
        FrequencyCardUsePacket.tryLinkWithCard(source.player(), source.card(), h.getLevel(), machine.getBlockPos(), Direction.UP, machine.getBlockPos().getCenter());
    }

    private static void assertWirelessChannel(GameTestHelper h, Source source, AENetworkedBlockEntity machine) {
        h.assertTrue(machine.getMainNode().isActive()
                        && machine.getMainNode().getGrid() == source.controller().getMainNode().getGrid()
                        && machine.getMainNode().getNode().meetsChannelRequirements(),
                machine.getBlockState().getBlock().getDescriptionId() + " receives a live channel from the wireless overloaded controller");
    }

    private static void cleanup(GameTestHelper h, Source source) {
        WirelessFrequencyManager.get().deleteFrequency(source.frequency(), h.getLevel().getServer());
    }

    private static boolean deviceListed(GameTestHelper h, Source source, AENetworkedBlockEntity machine) {
        return WirelessFrequencyManager.get().getDevices(source.frequency()).stream().anyMatch(entry ->
                entry.dimension().equals(h.getLevel().dimension()) && entry.pos().equals(machine.getBlockPos()));
    }

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)); }
    private static net.minecraft.world.item.Item blockItem(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
    private record Source(AdvancedWirelessOverloadedControllerBlockEntity controller, int frequency, ServerPlayer player, ItemStack card) {}
}
