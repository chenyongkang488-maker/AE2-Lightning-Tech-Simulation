package dev.overloadsim.client;

import com.moakiee.ae2lt.client.DeviceHubKeyMappings;
import dev.overloadsim.*;
import dev.overloadsim.tool.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

@EventBusSubscriber(modid=OverloadSimulation.ID,value=Dist.CLIENT)
public final class CoilClient {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void keys(ClientTickEvent.Pre event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null)return;
        if(CoilModules.isCoil(mc.player.getMainHandItem())||CoilModules.isCoil(mc.player.getOffhandItem()))while(DeviceHubKeyMappings.OPEN_CONFIG.consumeClick())PacketDistributor.sendToServer(new CoilPackets.Open());
    }
    @SubscribeEvent
    public static void effects(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null)return;var stack=mc.player.getMainHandItem();
        if(mc.player.isUsingItem()&&CoilModules.isCoil(mc.player.getUseItem())){
            int elapsed=mc.player.getTicksUsingItem();var p=mc.player.getEyePosition().add(mc.player.getLookAngle().scale(.65)).add(.15,-.25,0);
            mc.level.addParticle(new DustParticleOptions(new Vector3f(1,.45f,.78f),.8f),p.x,p.y,p.z,0,0,0);
            if(elapsed==SimulationConfig.COIL_SELF_CHARGE.get())mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.overload_sim.coil.self_ready"),true);
        }
        if(!CoilMining.ready(stack)||!mc.options.keyAttack.isDown()||mc.hitResult==null||mc.hitResult.getType()!=HitResult.Type.BLOCK)return;
        var start=mc.player.getEyePosition().add(mc.player.getLookAngle().scale(.55)).add(.15,-.3,0);var end=mc.hitResult.getLocation();
        for(int i=0;i<=10;i++){var point=start.lerp(end,i/10d).add((mc.level.random.nextDouble()-.5)*.06,(mc.level.random.nextDouble()-.5)*.06,0);mc.level.addParticle(new DustParticleOptions(new Vector3f(1,.48f,.78f),.6f),point.x,point.y,point.z,0,0,0);}
    }
    @SubscribeEvent public static void suppressSwing(InputEvent.InteractionKeyMappingTriggered event){var mc=Minecraft.getInstance();if(event.isAttack()&&mc.player!=null&&CoilMining.ready(mc.player.getMainHandItem())&&mc.hitResult!=null&&mc.hitResult.getType()==HitResult.Type.BLOCK)event.setSwingHand(false);}
}
