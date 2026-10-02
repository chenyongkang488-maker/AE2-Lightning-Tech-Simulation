package dev.overloadsim.data;

import java.util.stream.Stream;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overloadsim.ModContent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.*;
import net.neoforged.neoforge.registries.*;

/** A stored enchantment requirement; names and unrelated enchantments may coexist on the book. */
public record EnchantedBookIngredient(Holder<Enchantment> enchantment,int minLevel) implements ICustomIngredient {
    public static final MapCodec<EnchantedBookIngredient> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
        Enchantment.CODEC.fieldOf("enchantment").forGetter(EnchantedBookIngredient::enchantment),
        Codec.intRange(1,255).optionalFieldOf("min_level",1).forGetter(EnchantedBookIngredient::minLevel)
    ).apply(instance,EnchantedBookIngredient::new));
    private static final DeferredRegister<IngredientType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,"overload_sim");
    private static final DeferredHolder<IngredientType<?>,IngredientType<EnchantedBookIngredient>> TYPE=TYPES.register("enchanted_book",()->new IngredientType<>(CODEC));
    public static void register(IEventBus bus){TYPES.register(bus);}
    @Override public boolean test(ItemStack stack){return stack.is(Items.ENCHANTED_BOOK)&&stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(enchantment)>=minLevel;}
    @Override public Stream<ItemStack> getItems(){var stack=new ItemStack(Items.ENCHANTED_BOOK);stack.enchant(enchantment,minLevel);return Stream.of(stack);}
    @Override public boolean isSimple(){return false;}
    @Override public IngredientType<?> getType(){return TYPE.get();}
}
