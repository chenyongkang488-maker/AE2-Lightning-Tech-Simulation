package dev.overloadsim.tool;

import java.util.Optional;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.device.network.RailgunNetworkBinding;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.SimulationConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Public server-side targeting and strict lightning transaction. */
public final class CoilLightning {
    public enum Result {SUCCESS,NO_CORE,UNBOUND,NO_TARGET,NO_LIGHTNING,COOLDOWN,SPAWN_FAILED,DISABLED}
    private CoilLightning(){}
    public static Optional<Vec3> target(ServerPlayer player,boolean self){
        if(self)return Optional.of(player.position());
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(SimulationConfig.COIL_RANGE.get()));
        var block=player.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        var clipped=block.getType()==HitResult.Type.MISS?end:block.getLocation();
        Vec3 best=null;double distance=start.distanceToSqr(clipped);
        for(var entity:player.level().getEntities(player,new AABB(start,clipped).inflate(1),e->e.isAlive()&&e.isPickable()&&!e.isSpectator())){
            var hit=entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start,clipped);
            if(hit.isPresent()&&start.distanceToSqr(hit.get())<distance){distance=start.distanceToSqr(hit.get());best=entity.position();}
        }
        if(best!=null)return Optional.of(best);
        return block.getType()==HitResult.Type.BLOCK?Optional.of(block.getBlockPos().above().getBottomCenter()):Optional.empty();
    }
    public static Result fire(ServerPlayer player,ItemStack stack,boolean self){
        if(CoilWrench.active(stack))return Result.DISABLED;
        if(!CoilModules.isCoil(stack)||!CoilModules.hasCore(stack)||player.isSpectator()||!player.isAlive())return Result.NO_CORE;
        if(player.getCooldowns().isOnCooldown(stack.getItem()))return Result.COOLDOWN;
        var target=target(player,self);if(target.isEmpty()||!player.serverLevel().isLoaded(net.minecraft.core.BlockPos.containing(target.get())))return Result.NO_TARGET;
        var binding=RailgunNetworkBinding.INSTANCE.resolve(stack,player);if(!binding.success()||binding.grid()==null)return Result.UNBOUND;
        boolean natural=CoilSettings.read(stack).natural(stack);var key=natural?LightningKey.EXTREME_HIGH_VOLTAGE:LightningKey.HIGH_VOLTAGE;
        var inventory=binding.grid().getStorageService().getInventory();var source=IActionSource.ofPlayer(player);
        if(inventory.extract(key,10,Actionable.SIMULATE,source)!=10)return Result.NO_LIGHTNING;
        var bolt=EntityType.LIGHTNING_BOLT.create(player.serverLevel());if(bolt==null)return Result.SPAWN_FAILED;
        long got=inventory.extract(key,10,Actionable.MODULATE,source);
        if(got!=10){if(got>0)inventory.insert(key,got,Actionable.MODULATE,source);return Result.NO_LIGHTNING;}
        bolt.moveTo(target.get());bolt.setCause(player);bolt.setVisualOnly(false);
        bolt.getPersistentData().putBoolean("ae2lt.natural_weather_lightning",natural);
        bolt.getPersistentData().putBoolean("overload_sim.coil_lightning",true);
        if(!player.serverLevel().addFreshEntity(bolt)){inventory.insert(key,10,Actionable.MODULATE,source);return Result.SPAWN_FAILED;}
        player.getCooldowns().addCooldown(stack.getItem(),SimulationConfig.COIL_COOLDOWN.get());return Result.SUCCESS;
    }
}
