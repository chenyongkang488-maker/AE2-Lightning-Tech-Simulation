package dev.overloadsim.compat;

import com.moakiee.ae2lt.api.frequency.FrequencyBindingAccess;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/** AE2LT 2.1.0/2.1.1 expose a public unload hook outside the public access interface. */
public final class FrequencyBindingLifecycle {
    private static final ClassValue<Optional<Method>> UNLOAD_HOOK = new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            try {
                var method = type.getMethod("onChunkUnloaded");
                return method.getReturnType() == void.class && !Modifier.isStatic(method.getModifiers())
                        ? Optional.of(method) : Optional.empty();
            } catch (NoSuchMethodException | SecurityException absent) {
                return Optional.empty();
            }
        }
    };

    private FrequencyBindingLifecycle() {}

    /** The host must subsequently call its normal AE block-entity unload callback to destroy the node. */
    public static void chunkUnloaded(FrequencyBindingAccess binding) {
        var hook = UNLOAD_HOOK.get(binding.getClass());
        if (hook.isPresent()) {
            try {
                hook.get().invoke(binding);
                return;
            } catch (IllegalAccessException inaccessible) {
                // An unknown provider without an accessible public hook uses the public cleanup contract.
            } catch (InvocationTargetException failed) {
                var cause = failed.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                if (cause instanceof Error error) throw error;
                throw new IllegalStateException("AE2LT frequency unload hook failed", cause);
            }
        }
        binding.setRemoved();
    }
}
