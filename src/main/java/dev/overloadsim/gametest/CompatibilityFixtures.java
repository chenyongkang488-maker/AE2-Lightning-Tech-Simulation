package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;

/** Registry fixtures exist only in the dedicated GameTest JVM, never in player releases. */
public final class CompatibilityFixtures {
    public static void register(){
        for(var id:java.util.List.of("test_raw_block","test_raw_block_other","test_refined_block","test_cold_block","test_conflict_block_a","test_conflict_block_b","test_java_override_block","test_ambiguous_block","test_gem_storage","test_gem_ore","test_gem_ore_other","test_heterogem_storage","test_explicit_block","test_api_block","test_natural_block","test_denied_block"))ModContent.BLOCKS.register(id,()->new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2)));
        dev.overloadsim.api.SimulationExtensions.registerBindingCondition(ModContent.id("test_binding_denied"),(level,pos)->false);
        for(var id:java.util.List.of("test_raw_material","test_raw_material_conflict","test_ingot","test_gem"))ModContent.ITEMS.register(id,()->new Item(new Item.Properties()));
        ModContent.ITEMS.register("test_armor_egg",()->new SpawnEggItem(EntityType.PIG,0,0,new Item.Properties()){
            @Override public EntityType<?> getType(ItemStack stack){return EntityType.ARMOR_STAND;}
        });
    }
    private CompatibilityFixtures(){}
}
