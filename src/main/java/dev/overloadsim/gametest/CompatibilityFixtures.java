package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;

/** Registry fixtures exist only in the dedicated GameTest JVM, never in player releases. */
public final class CompatibilityFixtures {
    public static void register(){
        var crop=ModContent.BLOCKS.register("test_farmland_crop",()->new net.minecraft.world.level.block.CropBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)){
            @Override public java.util.List<ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){return isMaxAge(state)?java.util.List.of(new ItemStack(Items.APPLE,2),new ItemStack(Items.WHEAT_SEEDS)):java.util.List.of(new ItemStack(Items.WHEAT_SEEDS));}
        });
        ModContent.ITEMS.register("test_farmland_crop_seeds",()->new ItemNameBlockItem(crop.get(),new Item.Properties()));
        for(var id:java.util.List.of("test_explicit_crop","test_natural_crop","test_blacklisted_crop","test_stateful_api_crop"))ModContent.BLOCKS.register(id,()->new net.minecraft.world.level.block.CropBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
        ModContent.BLOCKS.register("test_supported_soil",()->new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1)){
            @Override public net.neoforged.neoforge.common.util.TriState canSustainPlant(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction direction,net.minecraft.world.level.block.state.BlockState plant){return plant.getBlock() instanceof net.minecraft.world.level.block.CropBlock?net.neoforged.neoforge.common.util.TriState.TRUE:net.neoforged.neoforge.common.util.TriState.FALSE;}
        });
        ModContent.BLOCKS.register("test_farmland",()->new net.minecraft.world.level.block.FarmBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.FARMLAND)));
        ModContent.BLOCKS.register("test_growth_crop",()->new net.minecraft.world.level.block.BushBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)){
            {registerDefaultState(defaultBlockState().setValue(GROWTH,0));}
            @Override public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BushBlock> codec(){return net.minecraft.world.level.block.DeadBushBlock.CODEC;}
            @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.state.BlockState> builder){builder.add(GROWTH);}
            @Override public java.util.List<ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){return state.getValue(GROWTH)==4?java.util.List.of(new ItemStack(Items.CARROT,3)):java.util.List.of(new ItemStack(Items.WHEAT_SEEDS));}
        });
        for(var id:java.util.List.of("test_api_crop","test_raw_block","test_raw_block_other","test_refined_block","test_cold_block","test_conflict_block_a","test_conflict_block_b","test_java_override_block","test_ambiguous_block","test_gem_storage","test_gem_ore","test_gem_ore_other","test_heterogem_storage","test_explicit_block","test_api_block","test_natural_block","test_denied_block"))ModContent.BLOCKS.register(id,()->new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2)));
        dev.overloadsim.api.SimulationExtensions.registerBindingCondition(ModContent.id("test_binding_denied"),(level,pos)->false);
        for(var id:java.util.List.of("test_raw_material","test_raw_material_conflict","test_ingot","test_gem"))ModContent.ITEMS.register(id,()->new Item(new Item.Properties()));
        ModContent.ITEMS.register("test_armor_egg",()->new SpawnEggItem(EntityType.PIG,0,0,new Item.Properties()){
            @Override public EntityType<?> getType(ItemStack stack){return EntityType.ARMOR_STAND;}
        });
    }
    private CompatibilityFixtures(){}
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty GROWTH=net.minecraft.world.level.block.state.properties.IntegerProperty.create("growth",0,4);
}
