package dev.overloadsim.machine;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.api.*;
import dev.overloadsim.data.MobSimulationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class SimulationEntityLoot {
    private static final GameProfile ATTACKER=new GameProfile(UUID.fromString("7279cffa-834a-46df-858a-c6ea0f9895cc"),"[SimulationLoot]");
    public static List<ItemStack> roll(ServerLevel level,BlockPos pos,CrystalData data,MobSimulationData.Rule rule){return roll(level,pos,data,rule,level.random,"simulation");}
    public static List<ItemStack> roll(ServerLevel level,BlockPos pos,CrystalData data,MobSimulationData.Rule rule,RandomSource random,String machine){
        if(rule.disabled())throw new IllegalStateException("Entity simulation disabled");
        if(rule.provider().isPresent())return MobLoot.provider(level,pos,data,rule.provider().get(),random,machine,rule.entity());
        if(!rule.outputs().isEmpty()){
            var out=new ArrayList<ItemStack>();for(var o:rule.outputs())if(random.nextDouble()<o.chance())out.add(new ItemStack(BuiltInRegistries.ITEM.getOptional(o.item()).orElseThrow(()->new IllegalStateException("Missing output "+o.item())),o.min()+random.nextInt(o.max()-o.min()+1)));return out;
        }
        var id=data.entityType().orElseThrow();var type=BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow();var created=type.create(level);
        if(!(created instanceof LivingEntity entity))throw new IllegalStateException("Not a living entity "+id);
        entity.setPos(Vec3.atCenterOf(pos));for(var slot:EquipmentSlot.values())entity.setItemSlot(slot,ItemStack.EMPTY);
        if(entity instanceof MagmaCube magma)magma.setSize(2,true);else if(entity instanceof Slime slime)slime.setSize(1,true);
        if(rule.template().isPresent()){
            var initializer=SimulationExtensions.entityTemplate(rule.template().get());if(initializer==null)throw new IllegalStateException("Unknown template "+rule.template().get());initializer.initialize(entity);
            if(entity.isAddedToLevel())throw new IllegalStateException("Template must not spawn an entity");
        }
        var builder=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,entity).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withLuck(0);
        if(rule.context().equals("player")){
            var attacker=FakePlayerFactory.get(level,ATTACKER);attacker.getInventory().clearContent();attacker.removeAllEffects();attacker.setPos(Vec3.atCenterOf(pos));
            builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER,attacker).withParameter(LootContextParams.ATTACKING_ENTITY,attacker).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,attacker).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(attacker));
        }else builder.withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic());
        var params=builder.create(LootContextParamSets.ENTITY);var table=rule.lootTable().map(k->net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,k)).orElse(type.getDefaultLootTable());
        // The normal NeoForge entry point applies GLMs exactly once. Never call live death code.
        return level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params,random);
    }
    private SimulationEntityLoot(){}
}
