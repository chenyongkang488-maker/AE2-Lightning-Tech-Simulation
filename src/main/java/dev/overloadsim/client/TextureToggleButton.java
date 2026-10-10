package dev.overloadsim.client;

import appeng.client.gui.widgets.ITooltip;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/** Delegates visuals, state changes and tooltips to the native button at either package location. */
public final class TextureToggleButton extends Button implements ITooltip {
    public enum ButtonType { MODE, AUTO_EXPORT, SPEED, FREQUENCY_BIND }
    private static final Class<?> NATIVE = AE2LTClientBridge.type(
            "com.moakiee.ae2lt.client.widgets.TextureToggleButton", "com.moakiee.ae2lt.client.TextureToggleButton");
    private static final Constructor<?> CONSTRUCTOR;
    private static final Class<?> TYPE, LISTENER;
    private static final Method STATE, TOOLTIP;
    static {
        try {
            TYPE = Class.forName(NATIVE.getName() + "$ButtonType");
            LISTENER = Class.forName(NATIVE.getName() + "$Listener");
            CONSTRUCTOR = NATIVE.getConstructor(TYPE, LISTENER);
            STATE = NATIVE.getMethod("setState", boolean.class);
            TOOLTIP = NATIVE.getMethod("setTooltipAt", int.class, List.class);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported AE2LT toolbar button API", failure);
        }
    }
    private final Button delegate;

    public TextureToggleButton(ButtonType type, IntConsumer listener) {
        super(0, 0, 18, 18, Component.empty(), button -> {}, DEFAULT_NARRATION);
        try {
            var nativeType = TYPE.getField(type.name()).get(null);
            var nativeListener = Proxy.newProxyInstance(LISTENER.getClassLoader(), new Class<?>[]{LISTENER},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("onChange")) { listener.accept((int) arguments[0]); return null; }
                        if (method.getName().equals("equals")) return proxy == arguments[0];
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("toString")) return "Lightning Tech: Simulation toolbar listener";
                        throw new UnsupportedOperationException(method.toString());
                    });
            delegate = (Button) CONSTRUCTOR.newInstance(nativeType, nativeListener);
            setSize(delegate.getWidth(), delegate.getHeight());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot create AE2LT toolbar button", failure);
        }
    }
    private void call(Method method, Object... arguments) {
        try { method.invoke(delegate, arguments); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot update AE2LT toolbar button", failure); }
    }
    public void setState(boolean state) { call(STATE, state); }
    public void setTooltipAt(int state, List<Component> lines) { call(TOOLTIP, state, lines); }
    public void setTooltipOn(List<Component> lines) { setTooltipAt(1, lines); }
    public void setTooltipOff(List<Component> lines) { setTooltipAt(0, lines); }
    public void setVisibility(boolean visible) { this.visible = visible; }
    private void synchronizeWidget() {
        delegate.setPosition(getX(), getY()); delegate.setSize(getWidth(), getHeight());
        delegate.active = active; delegate.visible = visible; delegate.setFocused(isFocused());
    }
    @Override public void onPress() { synchronizeWidget(); delegate.onPress(); }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        synchronizeWidget(); delegate.render(graphics, mouseX, mouseY, partial);
    }
    @Override public List<Component> getTooltipMessage() { return ((ITooltip) delegate).getTooltipMessage(); }
    @Override public Rect2i getTooltipArea() { synchronizeWidget(); return ((ITooltip) delegate).getTooltipArea(); }
    @Override public boolean isTooltipAreaVisible() { synchronizeWidget(); return ((ITooltip) delegate).isTooltipAreaVisible(); }
}
