package dev.overloadsim.mixin;

import com.moakiee.ae2lt.network.WirelessConnectorUsePacket;
import dev.overloadsim.output.tool.OutputConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers upstream packet paths as well as addon clients, never allowing an addon source into upstream toggles. */
@Mixin(value = WirelessConnectorUsePacket.class, remap = false)
public abstract class OutputConnectorPacketMixin {
    @Shadow public abstract InteractionHand hand();
    @Shadow public abstract BlockPos pos();

    @Inject(method = "handleOnServer", at = @At("HEAD"), cancellable = true)
    private void outputUse(ServerPlayer player, CallbackInfo ci) {
        if (OutputConnector.handles(player.level(), player.getItemInHand(hand()), pos())) {
            // Upstream packets have no held-slot token. Addon interactions use our validated payload.
            ci.cancel();
        }
    }
}
