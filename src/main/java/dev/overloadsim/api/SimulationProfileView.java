package dev.overloadsim.api;
import net.minecraft.resources.ResourceLocation;
public record SimulationProfileView(ResourceLocation id, String kind, String translationKey, ResourceLocation icon) {}
