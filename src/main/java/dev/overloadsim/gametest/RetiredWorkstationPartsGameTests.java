package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class RetiredWorkstationPartsGameTests {
    @GameTest(template="laser_empty")
    public static void removedWorkstationPartsHaveNoItemOrBlockRegistryEntry(GameTestHelper h) {
        for (String name : new String[]{"high_energy_workstation_frame", "high_energy_workstation_glass", "high_energy_workstation_receiver"}) {
            var id = ModContent.id(name);
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(id), "removed item is absent: " + id);
            h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(id), "removed block is absent: " + id);
            h.assertTrue(h.getLevel().getRecipeManager().getRecipes().stream()
                    .noneMatch(recipe -> recipe.id().getPath().startsWith(name + "_to_")), "removed recovery recipe is absent: " + id);
        }
        h.succeed();
    }
}
