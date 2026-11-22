package dev.overloadsim.compat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpstreamApiPresenceTest {
    private static final Set<String> COMPLETE=Set.of(
            "com.moakiee.ae2lt.api.lightning.collector.CollectorCrystalApi",
            "com.moakiee.ae2lt.api.device.DeviceWorkbenchApi",
            "com.moakiee.ae2lt.api.lightning.LightningApi");
    @Test void selectsApiOnlyForACompleteExtensionStack() {
        assertTrue(UpstreamApiPresence.available(COMPLETE::contains));
        assertFalse(UpstreamApiPresence.available(name->false));
    }
    @Test void partiallyAvailableApisKeepLegacyHooksTogether() {
        for(String missing:COMPLETE)
            assertFalse(UpstreamApiPresence.available(name->COMPLETE.contains(name)&&!name.equals(missing)));
    }
}
