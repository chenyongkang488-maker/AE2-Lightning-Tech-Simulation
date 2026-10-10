package dev.overloadsim.mixin;

import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import dev.overloadsim.output.tool.OutputConnector;
import dev.overloadsim.output.tool.OutputConnectorClient;
import dev.overloadsim.output.tool.OutputSourceSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OverloadedWirelessConnectorItem.class, remap = false)
public abstract class OutputConnectorItemMixin {
    @Inject(method = "handleBlockUse", at = @At("HEAD"), cancellable = true)
    private void outputBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() == null
                || !OutputConnector.handles(context.getLevel(), context.getItemInHand(), context.getClickedPos())) return;
        if (context.getLevel().isClientSide()) {
            OutputConnectorClient.use(context);
            cir.setReturnValue(InteractionResult.SUCCESS_NO_ITEM_USED);
        } else cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "selectHost", at = @At("HEAD"))
    private static void originalSelection(ItemStack stack, Level level, BlockPos pos, String hostType, CallbackInfo ci) {
        OutputSourceSelection.clear(stack);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void clearOutput(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        if (OutputSourceSelection.read(stack) == null) return;
        if (level.isClientSide()) cir.setReturnValue(InteractionResultHolder.success(stack));
        else if (player instanceof ServerPlayer server && OutputConnector.clear(server, hand))
            cir.setReturnValue(InteractionResultHolder.success(stack));
    }
}
