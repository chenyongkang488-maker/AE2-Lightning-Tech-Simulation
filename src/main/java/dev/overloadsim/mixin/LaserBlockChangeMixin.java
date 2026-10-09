package dev.overloadsim.mixin;

import dev.overloadsim.laser.OpticalChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe committed changes, including automation, commands and restored placement snapshots. */
@Mixin(value=Level.class,remap=false)
public abstract class LaserBlockChangeMixin {
    @Inject(method="markAndNotifyBlock",at=@At("RETURN"))
    private void opticalChange(BlockPos pos,LevelChunk chunk,BlockState before,BlockState after,int flags,int depth,CallbackInfo ci){
        var level=(Level)(Object)this;
        if(!level.isClientSide)OpticalChanges.changed(level,pos,before,level.getBlockState(pos));
    }
}
