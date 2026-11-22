package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.CrystalData;
import dev.overloadsim.api.CrystalDataAccess;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.data.MineralSimulationData;
import dev.overloadsim.data.SimulationData;
import dev.overloadsim.data.SimulationResolvers;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.BulkOutputBuffer;
import dev.overloadsim.multiblock.PrimordialOreResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class MekanismMineralGameTests {
    @GameTest(template = "empty")
    public static void datapackRuleOverridesBuiltInFluoriteAtDefaultPriority(GameTestHelper h) throws Exception {
        var storage = BuiltInRegistries.BLOCK.getOptional(id("mekanism:block_fluorite"));
        if (storage.isEmpty()) {
            h.succeed();
            return;
        }
        var manager = h.getLevel().getServer().getResourceManager();
        var resources = new java.util.HashMap<ResourceLocation, com.google.gson.JsonElement>();
        manager.listResources("simulation_mineral", key -> key.getPath().endsWith(".json")).forEach((key, value) -> {
            try (var reader = value.openAsReader()) {
                var path = key.getPath();
                resources.put(ResourceLocation.fromNamespaceAndPath(key.getNamespace(),
                        path.substring("simulation_mineral/".length(), path.length() - 5)),
                        com.google.gson.JsonParser.parseReader(reader));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        var apply = MineralSimulationData.class.getDeclaredMethod("apply", java.util.Map.class,
                net.minecraft.server.packs.resources.ResourceManager.class, net.minecraft.util.profiling.ProfilerFiller.class);
        apply.setAccessible(true);
        var edited = new java.util.HashMap<>(resources);
        edited.put(id("example:pack_fluorite"), com.google.gson.JsonParser.parseString("""
                {"material":"c:fluorite","binding":{"id":"#c:storage_blocks/fluorite"},"item":"minecraft:emerald"}
                """));
        try {
            apply.invoke(new MineralSimulationData(), edited, manager, null);
            var selected = MineralSimulationData.resolveBinding(h.getLevel(), storage.get().defaultBlockState());
            h.assertTrue(selected.valid() && selected.mineral().orElseThrow().item().orElseThrow().equals(id("minecraft:emerald")),
                    "a normal datapack override outranks a built-in Mekanism default");
        } finally {
            apply.invoke(new MineralSimulationData(), resources, manager, null);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void fluoriteBindsAndProducesGemsOrNaturalOre(GameTestHelper h) {
        var storage = BuiltInRegistries.BLOCK.getOptional(id("mekanism:block_fluorite"));
        if (storage.isEmpty()) {
            h.assertTrue(!MineralSimulationData.resolveProfile(h.getLevel(), profile("fluorite")).valid(),
                    "fluorite profile is absent without Mekanism");
            h.succeed();
            return;
        }
        var crystal = bind(h, storage.get(), storage.get(), "fluorite");
        verifyMachines(h, crystal, item("mekanism:fluorite_gem"), 2, 4, "mekanism:fluorite_ore");
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void sulfurOreBindsAndProducesMekanismDustOrNaturalOre(GameTestHelper h) {
        var sulfur = BuiltInRegistries.ITEM.getOptional(id("mekanism:dust_sulfur"));
        if (sulfur.isEmpty()) {
            h.assertTrue(!MineralSimulationData.resolveProfile(h.getLevel(), profile("sulfur")).valid(),
                    "Mekanism sulfur profile is absent without Mekanism");
            h.succeed();
            return;
        }
        var stone = BuiltInRegistries.BLOCK.getOptional(id("alltheores:sulfur_ore"));
        if (stone.isEmpty()) {
            h.assertTrue(!MineralSimulationData.resolveBinding(h.getLevel(), net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState()).valid(),
                    "Mekanism alone does not make unrelated rocks bind sulfur");
            h.succeed();
            return;
        }
        var deep = BuiltInRegistries.BLOCK.getOptional(id("alltheores:deepslate_sulfur_ore")).orElseThrow();
        var crystal = bind(h, stone.get(), deep, "sulfur");
        verifyMachines(h, crystal, sulfur.get(), 1, 1, "alltheores:sulfur_ore");
    }

    private static CrystalData bind(GameTestHelper h, Block first, Block second, String material) {
        BlockPos center = h.absolutePos(new BlockPos(5, 1, 5));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue;
                h.getLevel().setBlockAndUpdate(center.offset(x, 0, z),
                        ((x + z) % 2 == 0 ? first : second).defaultBlockState());
            }
        }
        var bound = CrystalBinding.bindStructure(h.getLevel(), center, new ItemStack(ModContent.BLANK.get()), true);
        h.assertTrue(bound.is(ModContent.BOUND.get()), "the 24 tagged blocks bind " + material);
        h.assertTrue(h.getLevel().getBlockState(center.offset(1, 0, 0)).isAir(), "binding consumes the ring");
        var crystal = CrystalDataAccess.read(bound).orElseThrow();
        h.assertTrue(crystal.profile().equals(profile(material)), "mineral profile ID is stable");
        return crystal;
    }

    private static void verifyMachines(GameTestHelper h, CrystalData crystal, Item expected, int min, int max,
            String oreId) {
        SimulationData.invalidate();
        var production = SimulationResolvers.production(h.getLevel(), crystal).orElseThrow();
        for (int i = 0; i < 32; i++) {
            var drops = production.roll(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), crystal, "test");
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(expected)
                    && drops.getFirst().getCount() >= min && drops.getFirst().getCount() <= max,
                    "unmodified base yield matches the mineral mapping");
        }
        h.assertTrue(PrimordialOreResolver.resolve(h.getLevel(), crystal).output().is(item(oreId)),
                "the ordinary stone ore is the primordial output");

        var single = SimulationGameTests.capacityMachine(h);
        single.inventory().setStackInSlot(0, CrystalDataAccess.perfect(crystal));
        h.runAtTickTime(80, () -> {
            for (int i = 0; i < 250; i++) single.tick();
            boolean singleProduced = false;
            for (int slot = 4; slot < 13; slot++) {
                singleProduced |= single.inventory().getStackInSlot(slot).is(expected);
            }
            h.assertTrue(singleProduced, "single chamber produces the mapped mineral");
            var multi = MultiblockGameTests.powered(h, 7);
            var minPos = multi.structure().min();
            h.runAfterDelay(25, () -> {
            multi.crystals().setStackInSlot(0, CrystalDataAccess.perfect(crystal));
            for (int i = 0; i < 190; i++) multi.tick();
            h.assertTrue(multi.outputs().prototype(0).is(expected), "multiblock produces the mapped mineral");
            multi.invalidateStructure();
            h.getLevel().setBlockAndUpdate(minPos.offset(1, 0, 1), MultiblockContent.PRIMORDIAL.get().defaultBlockState());
            multi.checkStructure();
            for (int i = 0; i < 370; i++) multi.tick();
            boolean oreProduced = false;
            for (int slot = 0; slot < BulkOutputBuffer.SLOTS; slot++) {
                oreProduced |= multi.outputs().prototype(slot).is(item(oreId));
            }
            h.assertTrue(oreProduced, "primordial chamber produces a natural ore block");
            h.succeed();
            });
        });
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    private static ResourceLocation profile(String material) {
        return ModContent.id("auto/mineral/c/" + material);
    }

    private static Item item(String name) {
        return BuiltInRegistries.ITEM.getOptional(id(name)).orElseThrow();
    }
}
