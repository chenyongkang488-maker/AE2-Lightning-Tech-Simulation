package dev.overloadsim.mixin;

import java.util.ArrayDeque;
import java.util.Deque;
import dev.overloadsim.SimulationConfig;
import dev.overloadsim.tool.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Commit FE only after server removal. A stack supports nested break hooks from other mods. */
@Mixin(value=ServerPlayerGameMode.class,remap=false)
public class CoilMiningCommitMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private final Deque<CoilMining.BreakAttempt> overloadSim$attempts=new ArrayDeque<>();
    @Inject(method="destroyBlock",at=@At("HEAD"))
    private void remember(BlockPos pos,CallbackInfoReturnable<Boolean> cir){overloadSim$attempts.push(new CoilMining.BreakAttempt(player.getMainHandItem()));}
    @Inject(method="removeBlock",at=@At("RETURN"))
    private void removed(BlockPos pos,BlockState state,boolean canHarvest,CallbackInfoReturnable<Boolean> cir){
        var attempt=overloadSim$attempts.peek();if(attempt!=null)attempt.removed=cir.getReturnValueZ();
    }
    @Inject(method="destroyBlock",at=@At("RETURN"),cancellable=true)
    private void commit(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        var attempt=overloadSim$attempts.pop();if(!CoilModules.isCoil(attempt.tool)||!cir.getReturnValueZ())return;
        if(!attempt.removed){cir.setReturnValue(false);return;}
        CoilEnergy.INSTANCE.tryConsume(attempt.tool,player,SimulationConfig.COIL_MINING_FE.get());CoilMining.spark(player,pos.getCenter());
    }
}
