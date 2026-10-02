package dev.overloadsim.gametest;

import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.data.*;
import dev.overloadsim.machine.MobLoot;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class MobCompatibilityGameTests {
    @GameTest(template="empty")
    public static void ruleConflictsAndDisableAreExplicit(GameTestHelper h){
        var enabled=MobSimulationData.Rule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"entity\":\"minecraft:phantom\"}")).getOrThrow();
        var disabled=MobSimulationData.Rule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"entity\":\"*\",\"priority\":100,\"disabled\":true}")).getOrThrow();
        var id=ResourceLocation.parse("minecraft:phantom");
        h.assertTrue(!MobSimulationData.resolve(id,Map.of(ModContent.id("a"),enabled,ModContent.id("b"),enabled)).enabled(),"equal priority rules fail closed");
        h.assertTrue(MobSimulationData.resolve(id,Map.of(ModContent.id("a"),enabled,ModContent.id("b"),disabled)).error().equals("disabled_entity"),"higher priority deny wins");
        h.assertTrue(MobSimulationData.resolve(ResourceLocation.parse("missing_mod:no_entity")).error().equals("missing_entity"),"removed entity diagnosed");h.succeed();
    }
    @GameTest(template="empty")
    public static void eggIndexAndLivingEntityAdapter(GameTestHelper h){
        var armor=net.minecraft.world.entity.EntityType.ARMOR_STAND.create(h.getLevel());
        var egg=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ModContent.id("test_armor_egg"));
        try{dev.overloadsim.binding.SimulationEntityEligibility.rebuildEggIndex(List.of(egg));h.assertTrue(dev.overloadsim.binding.SimulationEntityEligibility.eligible(armor),"custom egg enables living non-Mob");}
        finally{dev.overloadsim.binding.SimulationEntityEligibility.rebuildEggIndex();}
        SimulationExtensions.registerEntityEligibility(ModContent.id("test_armor_eligibility"),e->e.getType()==net.minecraft.world.entity.EntityType.ARMOR_STAND);
        h.assertTrue(dev.overloadsim.binding.SimulationEntityEligibility.eligible(armor)&&dev.overloadsim.binding.SimulationEntityEligibility.hasEgg(net.minecraft.world.entity.EntityType.FOX),"adapter and registry egg index work");h.succeed();
    }
    @GameTest(template="empty")
    public static void glmRunsOnceWithNeutralPlayerContext(GameTestHelper h){
        try{
            var getter=net.neoforged.neoforge.common.NeoForgeEventHandler.class.getDeclaredMethod("getLootModifierManager");getter.setAccessible(true);var manager=getter.invoke(null);
            var field=manager.getClass().getDeclaredField("registeredLootModifiers");field.setAccessible(true);var old=field.get(manager);int[] calls={0};
            var marker=new net.neoforged.neoforge.common.loot.IGlobalLootModifier(){
                public com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec(){throw new UnsupportedOperationException("in-memory test modifier");}
                public it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> apply(it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> drops,net.minecraft.world.level.storage.loot.LootContext context){
                    calls[0]++;var player=context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER);
                    h.assertTrue(player!=null&&player.getMainHandItem().isEmpty()&&context.getLuck()==0,"neutral player-kill context");drops.add(new ItemStack(Items.AMETHYST_SHARD));return drops;
                }
            };
            field.set(manager,Map.of(ModContent.id("test_modifier"),marker));
            try{var result=roll(h,"minecraft:phantom");h.assertTrue(calls[0]==1&&result.stream().filter(s->s.is(Items.AMETHYST_SHARD)).mapToInt(ItemStack::getCount).sum()==1,"GLM output applied exactly once");}
            finally{field.set(manager,old);}
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}h.succeed();
    }
    @GameTest(template="empty")
    public static void auditedPlayerGatedDropsExist(GameTestHelper h){
        var checks=Map.ofEntries(Map.entry("bogged",Items.TIPPED_ARROW),Map.entry("cave_spider",Items.SPIDER_EYE),Map.entry("drowned",Items.COPPER_INGOT),Map.entry("elder_guardian",Items.WET_SPONGE),Map.entry("evoker",Items.EMERALD),Map.entry("guardian",Items.COD),Map.entry("husk",Items.IRON_INGOT),Map.entry("rabbit",Items.RABBIT_FOOT),Map.entry("spider",Items.SPIDER_EYE),Map.entry("stray",Items.TIPPED_ARROW),Map.entry("vindicator",Items.EMERALD),Map.entry("wither_skeleton",Items.WITHER_SKELETON_SKULL),Map.entry("zombie",Items.IRON_INGOT),Map.entry("zombie_villager",Items.IRON_INGOT),Map.entry("zombified_piglin",Items.GOLD_INGOT));
        for(var entry:checks.entrySet()){h.getLevel().random.setSeed(entry.getKey().hashCode());boolean found=false;for(int i=0;i<1024&&!found;i++)found=roll(h,"minecraft:"+entry.getKey()).stream().anyMatch(s->s.is(entry.getValue()));h.assertTrue(found,"audited player drop "+entry.getKey());}h.succeed();
    }
    static List<ItemStack> roll(GameTestHelper h,String entity){
        var crystal=new CrystalData(ModContent.id("mob"),Optional.of(ResourceLocation.parse(entity)),0,1);
        var production=SimulationData.recipes(h.getLevel(),SimulationRecipe.Kind.PRODUCTION).stream().filter(r->r.value().data().profile().equals(ModContent.id("mob"))).findFirst().orElseThrow().value().data().production();
        return MobLoot.roll(h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(5,1,5)),crystal,production);
    }
    @GameTest(template="empty")
    public static void playerOnlyLootIsAvailable(GameTestHelper h){
        var checks=Map.of("minecraft:phantom",Items.PHANTOM_MEMBRANE,"minecraft:blaze",Items.BLAZE_ROD,"minecraft:breeze",Items.BREEZE_ROD);
        h.getLevel().random.setSeed(4401);
        for(var entry:checks.entrySet()){
            boolean found=false;for(int i=0;i<128;i++)if(roll(h,entry.getKey()).stream().anyMatch(s->s.is(entry.getValue())))found=true;
            h.assertTrue(found,"player-only drop missing: "+entry.getKey());
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void magmaAndSlimeUseSeparateSizes(GameTestHelper h){
        h.getLevel().random.setSeed(3301);boolean cream=false,ball=false;
        for(int i=0;i<64;i++){cream|=roll(h,"minecraft:magma_cube").stream().anyMatch(s->s.is(Items.MAGMA_CREAM));ball|=roll(h,"minecraft:slime").stream().anyMatch(s->s.is(Items.SLIME_BALL));}
        h.assertTrue(cream&&ball,"magma size2 yields cream; slime size1 yields balls");h.succeed();
    }
    @GameTest(template="empty")
    public static void bossOverridesReplaceEmptyOrPartialTables(GameTestHelper h){
        var star=roll(h,"minecraft:wither");h.assertTrue(star.size()==1&&star.getFirst().is(Items.NETHER_STAR)&&star.getFirst().getCount()==1,"wither star override");
        var warden=roll(h,"minecraft:warden");for(var item:List.of(Items.SCULK_CATALYST,Items.ECHO_SHARD,Items.SCULK))h.assertTrue(warden.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum()==1,"warden explicit output "+item);
        h.succeed();
    }
    @GameTest(template="empty")
    public static void dragonPoolsAreIndependent(GameTestHelper h){
        h.getLevel().random.setSeed(9701);var combos=new HashSet<Integer>();
        for(int i=0;i<256;i++){
            var drops=roll(h,"minecraft:ender_dragon");boolean egg=drops.stream().anyMatch(s->s.is(Items.DRAGON_EGG)),head=drops.stream().anyMatch(s->s.is(Items.DRAGON_HEAD));
            h.assertTrue(drops.stream().filter(s->s.is(Items.DRAGON_BREATH)).mapToInt(ItemStack::getCount).sum()==1,"dragon breath guaranteed once");combos.add((egg?1:0)|(head?2:0));
        }h.assertTrue(combos.size()==4,"rare dragon pools permit all four independent combinations");h.succeed();
    }
}
