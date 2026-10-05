package dev.overloadsim.compat;

import com.moakiee.ae2lt.item.railgun.RailgunExecutionMode;
import com.moakiee.ae2lt.menu.hub.DeviceStatusModel;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/** Bridges the extra EHV-beam flag introduced by AE2LT 2.1.1. */
final class CoilStatusFactory {
    private static final Constructor<DeviceStatusModel> CONSTRUCTOR = resolveConstructor();

    private CoilStatusFactory() {}

    static DeviceStatusModel create(String name, boolean core, boolean powered,
                                    List<DeviceStatusModel.ModuleInfo> modules, int selected,
                                    List<DeviceStatusModel.ModuleConfigInfo> configs) {
        Object[] arguments = {name, core, powered, modules, selected, configs,
                false, false, false, false, RailgunExecutionMode.NORMAL, false};
        if (CONSTRUCTOR.getParameterCount() == 13) {
            arguments = Arrays.copyOf(arguments, 13);
            arguments[12] = false; // A simulation coil never enables the railgun's EHV beam.
        }
        try {
            return CONSTRUCTOR.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create the AE2LT coil status model", exception);
        }
    }

    private static Constructor<DeviceStatusModel> resolveConstructor() {
        Class<?>[] original = {String.class, boolean.class, boolean.class, List.class,
                int.class, List.class, boolean.class, boolean.class, boolean.class,
                boolean.class, RailgunExecutionMode.class, boolean.class};
        Class<?>[] current = Arrays.copyOf(original, 13);
        current[12] = boolean.class;
        try {
            return DeviceStatusModel.class.getConstructor(current);
        } catch (NoSuchMethodException ignored) {
            try {
                return DeviceStatusModel.class.getConstructor(original);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Unsupported AE2LT device status API", exception);
            }
        }
    }
}
