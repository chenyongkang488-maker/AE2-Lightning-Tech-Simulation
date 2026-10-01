package dev.overloadsim.mixin;

import dev.overloadsim.client.CoilClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class CoilMouseScrollMixin {
    @Shadow private double accumulatedScrollX;
    @Shadow private double accumulatedScrollY;

    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void overloadSim$scroll(long window,double deltaX,double deltaY,CallbackInfo ci){
        if(CoilClient.scrollWrench(window,deltaY)){
            // Do not let a partial tool gesture leak into hotbar scrolling after Shift is released.
            accumulatedScrollX=0;accumulatedScrollY=0;ci.cancel();
        }
    }
}
