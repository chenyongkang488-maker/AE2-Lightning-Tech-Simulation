package dev.overloadsim.api;

import java.util.Optional;
import dev.overloadsim.ModContent;
import net.minecraft.world.item.ItemStack;

public final class CrystalDataAccess {
    private CrystalDataAccess() {}
    public static Optional<CrystalData> read(ItemStack stack) { return Optional.ofNullable(stack.get(ModContent.CRYSTAL_DATA.get())); }
    public static boolean isSimulationCrystal(ItemStack s) { return s.is(ModContent.BLANK.get()) || s.is(ModContent.BOUND.get()) || s.is(ModContent.PERFECT.get()); }
    public static ItemStack bound(CrystalData data) { var s = new ItemStack(ModContent.BOUND.get()); s.set(ModContent.CRYSTAL_DATA.get(),data); return s; }
    public static ItemStack perfect(CrystalData data) { var s = new ItemStack(ModContent.PERFECT.get()); s.set(ModContent.CRYSTAL_DATA.get(),data.perfected()); return s; }
}
