package dev.overloadsim.api;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Immutable item identity. Entity UUID, inventory and equipment are never stored. */
public record CrystalData(ResourceLocation profile, Optional<ResourceLocation> entityType, int strikes, int format) {
    public static final Codec<CrystalData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("profile").forGetter(CrystalData::profile),
            ResourceLocation.CODEC.optionalFieldOf("entity_type").forGetter(CrystalData::entityType),
            Codec.intRange(0,1_000_000).optionalFieldOf("strikes",0).forGetter(CrystalData::strikes),
            Codec.intRange(1,1).optionalFieldOf("format",1).forGetter(CrystalData::format)
    ).apply(i, CrystalData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CrystalData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public CrystalData { if (strikes < 0 || strikes > 1_000_000 || format != 1) throw new IllegalArgumentException("invalid crystal data"); }
    public CrystalData advance(int count) { return new CrystalData(profile,entityType,Math.min(1_000_000,Math.addExact(strikes,count)),format); }
    public CrystalData perfected() { return new CrystalData(profile,entityType,0,format); }
}
