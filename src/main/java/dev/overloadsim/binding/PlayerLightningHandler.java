package dev.overloadsim.binding;
import java.util.*;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.*;
import dev.overloadsim.core.SimulationRules;
import dev.overloadsim.data.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

public final class PlayerLightningHandler {
    private PlayerLightningHandler(){}
    // The strike event denotes a lightning hit even when damage/vanilla effects are canceled.
    // Observe it without clearing cancellation, so damage protection remains effective.
    public static void struck(EntityStruckByLightningEvent event){if(event.getEntity() instanceof ServerPlayer player)process(player,event.getLightning());}
    public static void process(ServerPlayer player,LightningBolt bolt){
        var stack=player.getOffhandItem();if(!CrystalDataAccess.isSimulationCrystal(stack)||stack.is(ModContent.PERFECT.get()))return;
        var level=player.serverLevel();var ledger=bolt.getPersistentData();String key="overload_sim.hit."+player.getUUID();if(ledger.getBoolean(key))return;ledger.putBoolean(key,true);
        boolean natural=ledger.getBoolean("ae2lt.natural_weather_lightning");
        if(stack.is(ModContent.BOUND.get())){var next=CrystalBinding.cultivate(level,player.blockPosition(),stack,natural);if(player.getOffhandItem()==stack&&next!=stack){player.setItemInHand(InteractionHand.OFF_HAND,next);NeoForge.EVENT_BUS.post(new SimulationEvents.Completed("cultivation",level,player.blockPosition(),CrystalDataAccess.read(next).orElseThrow()));}return;}
        var rules=SimulationData.recipes(level,SimulationRecipe.Kind.MOB_BINDING);
        double max=rules.stream().mapToDouble(h->h.value().data().mob().radius()).max().orElse(0);
        record Candidate(LivingEntity mob,SimulationRecipe recipe){}
        var candidates=new ArrayList<Candidate>();
        for(var mob:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(max),SimulationEntityEligibility::eligible)){
            if(!MobSimulationData.resolve(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())).enabled())continue;
            var matching=rules.stream().filter(h->matches(mob,h.value().data().mob())).toList();var selected=SimulationData.select(matching);if(selected.isEmpty())continue;var r=selected.get().value();
            if(r.data().mob().disabled()||(!natural&&!r.data().allowArtificial())||SimulationData.profile(r.data().profile()).isEmpty()||!SimulationRules.withinRadius(player.distanceToSqr(mob),r.data().mob().radius()))continue;candidates.add(new Candidate(mob,r));
        }
        candidates.sort(Comparator.<Candidate>comparingDouble(c->player.distanceToSqr(c.mob())).thenComparing(c->c.mob().getUUID()));if(candidates.isEmpty())return;
        var target=candidates.getFirst();if(!SimulationRules.chance(level.random.nextDouble(),target.recipe().data().mob().probability()))return;
        var data=new CrystalData(target.recipe().data().profile(),Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(target.mob().getType())),0,1);
        var original=stack.copy();if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeBinding(level,player.blockPosition(),data)).isCanceled())return;
        if(!player.isAlive()||!target.mob().isAlive()||!ItemStack.matches(original,player.getOffhandItem()))return;
        player.setItemInHand(InteractionHand.OFF_HAND,CrystalDataAccess.bound(data));NeoForge.EVENT_BUS.post(new SimulationEvents.Completed("mob_binding",level,player.blockPosition(),data));
    }
    private static boolean matches(LivingEntity mob,SimulationRecipe.MobRule rule){
        String id=rule.entity();boolean match=id.equals("*")||(id.startsWith("#")?mob.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(id.substring(1)))):BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).equals(ResourceLocation.parse(id)));
        return match&&(rule.condition().isEmpty()||mob instanceof Mob m&&SimulationExtensions.mob(rule.condition().get(),m));
    }
}
