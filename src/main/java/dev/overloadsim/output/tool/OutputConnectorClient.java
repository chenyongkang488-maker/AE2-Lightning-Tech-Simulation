package dev.overloadsim.output.tool;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.core.ModeScroll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = OverloadSimulation.ID, value = Dist.CLIENT)
public final class OutputConnectorClient {
    private static final ModeScroll SCROLL = new ModeScroll();
    private static LocalPlayer scrollingPlayer;
    private static ItemStack scrollingItem;
    private static int scrollingSlot = -1;

    public static void use(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null) PacketDistributor.sendToServer(new OutputConnectorPackets.Use(context.getHand(),
                player.getInventory().selected, context.getClickedPos(), context.getClickedFace(), Screen.hasControlDown()));
    }

    private static boolean canScroll(Minecraft mc) {
        return mc.player != null && mc.player.isAlive() && !mc.player.isSpectator() && mc.screen == null
                && mc.getOverlay() == null && mc.player.isShiftKeyDown()
                && OutputConnector.isTool(mc.player.getMainHandItem())
                && OutputSourceSelection.read(mc.player.getMainHandItem()) != null;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        if (!canScroll(Minecraft.getInstance())) reset();
    }

    /** Called before vanilla wheel accumulation; consumes fractional source-selection gestures. */
    public static boolean scroll(long window, double delta) {
        var mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || !canScroll(mc) || delta == 0) return false;
        var stack = mc.player.getMainHandItem();
        if (mc.player != scrollingPlayer || stack != scrollingItem || mc.player.getInventory().selected != scrollingSlot) {
            reset(); scrollingPlayer = mc.player; scrollingItem = stack; scrollingSlot = mc.player.getInventory().selected;
        }
        double adjusted = (mc.options.discreteMouseScroll().get() ? Math.signum(delta) : delta)
                * mc.options.mouseWheelSensitivity().get();
        int steps = SCROLL.scroll(adjusted, mc.gui.getGuiTicks());
        if (steps != 0) PacketDistributor.sendToServer(new OutputConnectorPackets.Cycle(scrollingSlot, steps));
        return true;
    }
    private static void reset() {
        SCROLL.reset(); scrollingPlayer = null; scrollingItem = null; scrollingSlot = -1;
    }
}
