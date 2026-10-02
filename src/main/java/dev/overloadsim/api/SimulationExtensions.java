package dev.overloadsim.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Register named Java extensions during common setup; JSON references their names. */
public final class SimulationExtensions {
    private SimulationExtensions() {}
    public record OutputContext(ServerLevel level, BlockPos position, CrystalData crystal) {}
    @FunctionalInterface public interface OutputProvider { List<ItemStack> generate(OutputContext context); }
    @FunctionalInterface public interface BindingCondition { boolean test(ServerLevel level, BlockPos position); }
    @FunctionalInterface public interface MobPredicate { boolean test(Mob mob); }
    public record OutputContextV2(ServerLevel level,BlockPos position,CrystalData crystal,RandomSource random,String machine,String rule){}
    @FunctionalInterface public interface OutputProviderV2 { List<ItemStack> generate(OutputContextV2 context); }
    @FunctionalInterface public interface EntityEligibility { boolean test(LivingEntity entity); }
    @FunctionalInterface public interface EntityTemplateInitializer { void initialize(LivingEntity unspawnedEntity); }
    @FunctionalInterface public interface MineralResolver { java.util.Optional<ResolvedMineral> resolve(ServerLevel level,net.minecraft.world.level.block.state.BlockState state); }
    private static final Map<ResourceLocation,MineralResolver> MINERALS=new ConcurrentHashMap<>();
    public static void registerMineralResolver(ResourceLocation id,MineralResolver resolver){if(MINERALS.putIfAbsent(id,resolver)!=null)throw new IllegalArgumentException("duplicate mineral resolver "+id);dev.overloadsim.data.SimulationData.invalidate();}
    public static List<ResolvedMineral> minerals(ServerLevel level,net.minecraft.world.level.block.state.BlockState state){return MINERALS.entrySet().stream().sorted(Map.Entry.comparingByKey()).flatMap(e->e.getValue().resolve(level,state).stream()).toList();}
    public static boolean hasMineralResolvers(){return !MINERALS.isEmpty();}
    private static final Map<ResourceLocation,OutputProvider> OUTPUTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,BindingCondition> BINDINGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,MobPredicate> MOBS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,OutputProviderV2> OUTPUTS_V2=new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,EntityEligibility> ELIGIBILITY=new ConcurrentHashMap<>();
    private static final Map<ResourceLocation,EntityTemplateInitializer> TEMPLATES=new ConcurrentHashMap<>();
    public static synchronized void registerOutputV2(ResourceLocation id,OutputProviderV2 provider){if(OUTPUTS_V2.containsKey(id)||OUTPUTS.containsKey(id))throw new IllegalArgumentException("duplicate output "+id);OUTPUTS_V2.put(id,java.util.Objects.requireNonNull(provider));}
    public static OutputProviderV2 outputV2(ResourceLocation id){return OUTPUTS_V2.get(id);}
    public static void registerEntityEligibility(ResourceLocation id,EntityEligibility provider){if(ELIGIBILITY.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("duplicate eligibility "+id);}
    public static boolean entityEligible(LivingEntity entity){return ELIGIBILITY.values().stream().anyMatch(p->p.test(entity));}
    public static void registerEntityTemplate(ResourceLocation id,EntityTemplateInitializer provider){if(TEMPLATES.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("duplicate template "+id);}
    public static EntityTemplateInitializer entityTemplate(ResourceLocation id){return TEMPLATES.get(id);}
    public static synchronized void registerOutput(ResourceLocation id, OutputProvider provider) { if(OUTPUTS_V2.containsKey(id)||OUTPUTS.containsKey(id))throw new IllegalArgumentException("duplicate output "+id);OUTPUTS.put(id,java.util.Objects.requireNonNull(provider)); }
    public static void registerBindingCondition(ResourceLocation id, BindingCondition condition) { if(BINDINGS.putIfAbsent(id,condition)!=null)throw new IllegalArgumentException("duplicate binding "+id); }
    public static void registerMobPredicate(ResourceLocation id, MobPredicate predicate) { if(MOBS.putIfAbsent(id,predicate)!=null)throw new IllegalArgumentException("duplicate mob predicate "+id); }
    public static OutputProvider output(ResourceLocation id) { return OUTPUTS.get(id); }
    public static boolean binding(ResourceLocation id, ServerLevel level, BlockPos pos) { var c=BINDINGS.get(id); return c!=null && c.test(level,pos); }
    public static boolean mob(ResourceLocation id,Mob mob) { var c=MOBS.get(id); return c!=null && c.test(mob); }
}
