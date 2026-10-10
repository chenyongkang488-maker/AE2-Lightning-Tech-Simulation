package dev.overloadsim.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;

/** Client class locations changed in AE2LT 2.1.2; resolve once while retaining older runtimes. */
public final class AE2LTClientBridge {
    private AE2LTClientBridge() {}

    static Class<?> type(String current, String previous) {
        try {
            return Class.forName(current);
        } catch (ClassNotFoundException absent) {
            try {
                return Class.forName(previous);
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Unsupported AE2LT client API: " + current, failure);
            }
        }
    }

    private static final class Keys {
        static final KeyMapping OPEN_CONFIG = resolve();
        private static KeyMapping resolve() {
            try {
                return (KeyMapping) type("com.moakiee.ae2lt.client.hub.DeviceHubKeyMappings",
                        "com.moakiee.ae2lt.client.DeviceHubKeyMappings").getField("OPEN_CONFIG").get(null);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot read AE2LT device configuration key", failure);
            }
        }
    }
    public static KeyMapping openConfigKey() { return Keys.OPEN_CONFIG; }

    private static final class Faces {
        static final MethodHandle TYPE = resolve();
        private static MethodHandle resolve() {
            try {
                var type = type("com.moakiee.ae2lt.client.render.Ae2ltRenderTypes",
                        "com.moakiee.ae2lt.client.Ae2ltRenderTypes");
                return MethodHandles.publicLookup().unreflect(type.getMethod("getFaceSeeThrough"));
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot resolve AE2LT connector face material", failure);
            }
        }
    }
    public static RenderType faceSeeThrough() {
        try {
            return (RenderType) Faces.TYPE.invokeExact();
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot obtain AE2LT connector face material", failure);
        }
    }
}
