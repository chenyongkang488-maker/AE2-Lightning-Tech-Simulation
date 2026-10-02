package dev.overloadsim.api;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Immutable material identity. A resolver supplies data only, never consumes blocks or fees. */
public record ResolvedMineral(ResourceLocation profile,ResourceLocation material,String binding,Optional<ResourceLocation> item,int min,int max,Optional<ResourceLocation> ore,Optional<Smelting> smelting,boolean sameBlock) {
    public record Smelting(ResourceLocation input,ResourceLocation result,int count){}
    public ResolvedMineral{
        if(min<1||max<min||max>4096||item.isEmpty()&&ore.isEmpty())throw new IllegalArgumentException("Invalid mineral output");
        if(smelting.isPresent()&&(smelting.get().count()<1||smelting.get().count()>64))throw new IllegalArgumentException("Invalid smelt count");
    }
}
