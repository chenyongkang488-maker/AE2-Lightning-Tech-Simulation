package dev.overloadsim.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/** Register named Java extensions during common setup; JSON references their names. */
public final class SimulationExtensions {
    private SimulationExtensions() {}
    public record OutputContext(ServerLevel level, BlockPos position, CrystalData crystal) {}
    @FunctionalInterface public interface OutputProvider { List<ItemStack> generate(OutputContext context); }
    @FunctionalInterface public interface BindingCondition { boolean test(ServerLevel level, BlockPos position); }
    @FunctionalInterface public interface MobPredicate { boolean test(Mob mob); }
    private static final Map<ResourceLocation,OutputProvider> OUTPUTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,BindingCondition> BINDINGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,MobPredicate> MOBS = new ConcurrentHashMap<>();
    public static void registerOutput(ResourceLocation id, OutputProvider provider) { if(OUTPUTS.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("duplicate output "+id); }
    public static void registerBindingCondition(ResourceLocation id, BindingCondition condition) { if(BINDINGS.putIfAbsent(id,condition)!=null)throw new IllegalArgumentException("duplicate binding "+id); }
    public static void registerMobPredicate(ResourceLocation id, MobPredicate predicate) { if(MOBS.putIfAbsent(id,predicate)!=null)throw new IllegalArgumentException("duplicate mob predicate "+id); }
    public static OutputProvider output(ResourceLocation id) { return OUTPUTS.get(id); }
    public static boolean binding(ResourceLocation id, ServerLevel level, BlockPos pos) { var c=BINDINGS.get(id); return c!=null && c.test(level,pos); }
    public static boolean mob(ResourceLocation id,Mob mob) { var c=MOBS.get(id); return c!=null && c.test(mob); }
}
