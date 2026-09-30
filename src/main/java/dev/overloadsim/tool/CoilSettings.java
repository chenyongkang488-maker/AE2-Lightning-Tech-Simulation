package dev.overloadsim.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import dev.overloadsim.ModContent;

/** Immutable public configuration; effect getters also check the current loadout. */
public record CoilSettings(boolean extreme, boolean efficiencyEnabled, int efficiency, int fortune, boolean silk) {
    public static final CoilSettings DEFAULT=new CoilSettings(false,true,10,5,true);
    public static final Codec<CoilSettings> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.BOOL.optionalFieldOf("extreme",false).forGetter(CoilSettings::extreme),
        Codec.BOOL.optionalFieldOf("efficiency_enabled",true).forGetter(CoilSettings::efficiencyEnabled),
        Codec.intRange(0,10).optionalFieldOf("efficiency",10).forGetter(CoilSettings::efficiency),
        Codec.intRange(0,5).optionalFieldOf("fortune",5).forGetter(CoilSettings::fortune),
        Codec.BOOL.optionalFieldOf("silk",true).forGetter(CoilSettings::silk)
    ).apply(i,CoilSettings::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CoilSettings> STREAM_CODEC=ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public CoilSettings {efficiency=Math.clamp(efficiency,0,10);fortune=Math.clamp(fortune,0,5);}
    public static CoilSettings read(ItemStack stack){return stack.getOrDefault(ModContent.COIL_SETTINGS.get(),DEFAULT);}
    public boolean natural(ItemStack stack){return extreme&&CoilModules.has(stack,CoilModuleItem.Type.EXTREME);}
    public int efficiencyLevel(ItemStack stack){return efficiencyEnabled&&CoilModules.miningReady(stack)&&CoilModules.has(stack,CoilModuleItem.Type.EFFICIENCY)?efficiency:0;}
    public boolean silkActive(ItemStack stack){return silk&&CoilModules.miningReady(stack)&&CoilModules.has(stack,CoilModuleItem.Type.SILK);}
    public int fortuneLevel(ItemStack stack){return !silkActive(stack)&&CoilModules.miningReady(stack)&&CoilModules.has(stack,CoilModuleItem.Type.FORTUNE)?fortune:0;}
}
