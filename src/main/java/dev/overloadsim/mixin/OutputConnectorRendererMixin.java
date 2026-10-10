package dev.overloadsim.mixin;

import com.moakiee.ae2lt.client.WirelessConnectorRenderer;
import dev.overloadsim.client.OutputConnectorRenderer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An addon selection renders its own host; ordinary upstream selections keep their renderer. */
@Mixin(value=WirelessConnectorRenderer.class,remap=false)
public abstract class OutputConnectorRendererMixin {
    @Inject(method="onRenderLevelStage",at=@At("HEAD"),cancellable=true)
    private static void overloadSim$outputSelection(RenderLevelStageEvent event,CallbackInfo ci) {
        if(OutputConnectorRenderer.hasOutputSelection())ci.cancel();
    }
}
