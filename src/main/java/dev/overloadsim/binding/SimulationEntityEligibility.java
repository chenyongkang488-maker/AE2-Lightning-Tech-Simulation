package dev.overloadsim.binding;

import java.util.*;
import dev.overloadsim.api.SimulationExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class SimulationEntityEligibility {
    private static volatile Set<EntityType<?>> eggs;
    public static void rebuildEggIndex(){rebuildEggIndex(BuiltInRegistries.ITEM);}
    /** Also permits custom egg suppliers to rebuild the index during integration setup. */
    public static void rebuildEggIndex(Iterable<Item> items){
        var found=new HashSet<EntityType<?>>();for(var item:items)if(item instanceof SpawnEggItem egg){try{found.add(egg.getType(item.getDefaultInstance()));}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(SimulationEntityEligibility.class).warn("Cannot resolve spawn egg {}",BuiltInRegistries.ITEM.getKey(item),e);}}
        eggs=Set.copyOf(found);
    }
    public static boolean hasEgg(EntityType<?> type){if(eggs==null)rebuildEggIndex();return eggs.contains(type);}
    public static boolean eligible(LivingEntity entity){return entity.isAlive()&&!(entity instanceof Player)&&(entity instanceof Mob||hasEgg(entity.getType())||SimulationExtensions.entityEligible(entity));}
    private SimulationEntityEligibility(){}
}
