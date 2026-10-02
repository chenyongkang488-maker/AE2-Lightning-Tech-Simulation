package dev.overloadsim.gametest;

import java.util.List;
import dev.overloadsim.*;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.*;
import com.moakiee.ae2lt.machine.lightningchamber.LightningSimulationChamberInventory;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.*;
import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class ManufacturingRecipeGameTests {
    private static final List<String> ASSEMBLED=List.of("overload_simulation_chamber","resonance_coil","simulation_controller","simulation_frame","simulation_efficiency_t1","simulation_efficiency_t2","simulation_efficiency_t3","simulation_fortune_module","simulation_overload_module","simulation_smelting_module","extreme_voltage_module","mimic_tool_module","ultimate_destruction_module","efficiency_module","fortune_module","silk_touch_module","wrench_module");
    private static Recipe<?> recipe(GameTestHelper h,String id){return h.getLevel().getRecipeManager().byKey(ModContent.id(id)).orElseThrow().value();}
    private static LightningAssemblyRecipe assembly(GameTestHelper h,String id){var recipe=recipe(h,id);h.assertTrue(recipe instanceof LightningAssemblyRecipe,"recipe must load through native lightning assembly serializer: "+id);return (LightningAssemblyRecipe)recipe;}
    private static ItemStack stack(String id,int count){return new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(),count);}
    @GameTest(template="empty")
    public static void allManufacturingRecipesUseNativeMachines(GameTestHelper h){
        h.assertTrue(recipe(h,"blank_simulation_crystal") instanceof LightningSimulationRecipe,"blank crystal is made in original lightning simulation room");
        for(var id:ASSEMBLED){var recipe=assembly(h,id);h.assertTrue(recipe.getResultStack().is(BuiltInRegistries.ITEM.get(ModContent.id(id)))&&recipe.getResultStack().getCount()==1,"native assembly result is one addon item: "+id);}h.succeed();
    }
    @GameTest(template="empty")
    public static void blankCrystalUsesThreeNativeSlotsAndExactQuantities(GameTestHelper h){
        var raw=recipe(h,"blank_simulation_crystal");h.assertTrue(raw instanceof LightningSimulationRecipe,"blank crystal must be a simulation recipe");var recipe=(LightningSimulationRecipe)raw;var inventory=new LightningSimulationChamberInventory(null);
        inventory.setStackInSlot(0,stack("ae2lt:electro_chime_crystal",1));inventory.setStackInSlot(1,stack("ae2lt:overload_crystal",16));inventory.setStackInSlot(2,new ItemStack(Items.AMETHYST_SHARD,16));
        h.assertTrue(recipe.matches(LightningSimulationRecipeInput.fromInventory(inventory),h.getLevel()),"native three-slot input matches");h.assertTrue(recipe.totalEnergy()==200_000&&recipe.lightningCost()==32&&recipe.lightningTier()==LightningKey.Tier.HIGH_VOLTAGE,"simulation cost is 200k FE and 32 HV");
        for(int slot=0;slot<3;slot++){var original=inventory.getStackInSlot(slot).copy();var insufficient=original.copy();insufficient.shrink(1);inventory.setStackInSlot(slot,insufficient);h.assertTrue(!recipe.matches(LightningSimulationRecipeInput.fromInventory(inventory),h.getLevel()),"one missing ingredient refuses the recipe");inventory.setStackInSlot(slot,original);}h.succeed();
    }
    @GameTest(template="empty")
    public static void fortuneAssemblyAcceptsLargeStacksAndRejectsMissingMaterials(GameTestHelper h){
        var recipe=assembly(h,"simulation_fortune_module");var inventory=new LightningAssemblyChamberInventory(null);
        inventory.setStackInSlot(0,stack("ae2lt:lightning_collapse_matrix",8));inventory.setStackInSlot(1,stack("ae2lt:overload_singularity",64));inventory.setStackInSlot(2,new ItemStack(Items.DRAGON_EGG));inventory.setStackInSlot(3,stack("ae2lt:firmament_alloy_ingot",64));inventory.setStackInSlot(4,stack("overload_sim:simulation_frame",1));
        h.assertTrue(recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"all large native input stacks match");h.assertTrue(recipe.totalEnergy()==1_600_000&&recipe.lightningCost()==256&&recipe.lightningTier()==LightningKey.Tier.EXTREME_HIGH_VOLTAGE,"fortune cost is 1600k FE and 256 EHV");
        for(int slot=0;slot<5;slot++){var original=inventory.getStackInSlot(slot).copy();var insufficient=original.copy();insufficient.shrink(1);inventory.setStackInSlot(slot,insufficient);h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"each listed material and quantity is required");inventory.setStackInSlot(slot,original);}h.succeed();
    }
    private static ItemStack book(GameTestHelper h,boolean silk,boolean extra){var stack=new ItemStack(Items.ENCHANTED_BOOK);var registry=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);if(silk)stack.enchant(registry.getHolderOrThrow(Enchantments.SILK_TOUCH),1);if(extra)stack.enchant(registry.getHolderOrThrow(Enchantments.EFFICIENCY),5);return stack;}
    @GameTest(template="empty")
    public static void silkTouchRequiresActualBookEnchantment(GameTestHelper h){
        var recipe=assembly(h,"silk_touch_module");var inventory=new LightningAssemblyChamberInventory(null);inventory.setStackInSlot(0,stack("ae2lt:overload_module_base",1));
        for(var invalid:List.of(new ItemStack(Items.BOOK),book(h,false,false),book(h,false,true))){inventory.setStackInSlot(1,invalid);h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"ordinary or unrelated enchanted book is rejected");}
        for(var valid:List.of(book(h,true,false),book(h,true,true))){valid.set(DataComponents.CUSTOM_NAME,Component.literal("custom silk book"));inventory.setStackInSlot(1,valid);h.assertTrue(recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"silk-touch books may retain a custom name and other enchantments");}
        var pickaxe=new ItemStack(Items.DIAMOND_PICKAXE);pickaxe.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH),1);inventory.setStackInSlot(1,pickaxe);h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"enchanted tools do not substitute for the book");h.succeed();
    }
    @GameTest(template="empty")
    public static void enchantedIngredientSurvivesRecipeNetworkSync(GameTestHelper h){
        var recipe=assembly(h,"silk_touch_module");var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{var codec=new LightningAssemblyRecipe.Serializer().streamCodec();codec.encode(buffer,recipe);var decoded=codec.decode(buffer);var inventory=new LightningAssemblyChamberInventory(null);inventory.setStackInSlot(0,stack("ae2lt:overload_module_base",1));inventory.setStackInSlot(1,book(h,true,true));h.assertTrue(decoded.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"recipe sync retains enchantment predicate");inventory.setStackInSlot(1,book(h,false,true));h.assertTrue(!decoded.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"recipe sync does not degrade to any enchanted book");h.assertTrue(decoded.getIngredients().stream().flatMap(i->java.util.Arrays.stream(i.getItems())).anyMatch(s->s.is(Items.ENCHANTED_BOOK)&&s.getOrDefault(DataComponents.STORED_ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH))>0),"recipe viewers receive a silk-touch book display stack");}finally{buffer.release();}h.succeed();
    }
}
