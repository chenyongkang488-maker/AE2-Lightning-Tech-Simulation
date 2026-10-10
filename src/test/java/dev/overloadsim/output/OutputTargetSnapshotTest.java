package dev.overloadsim.output;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputTargetSnapshotTest {
    @Test void reorderedTargetsCannotMakeAStaleDeleteRemoveAnotherConnection() throws Exception {
        var type = Class.forName("dev.overloadsim.output.OutputTargetSnapshot");
        var snapshot = type.getConstructor().newInstance();
        var refresh = type.getMethod("refresh", int.class, List.class);
        var token = type.getMethod("token", int.class);
        var resolve = type.getMethod("resolve", int.class, String.class);
        refresh.invoke(snapshot, 2, List.of("first", "second"));
        String stale = (String) token.invoke(snapshot, 0);
        assertEquals("first", resolve.invoke(snapshot, 2, stale));
        refresh.invoke(snapshot, 2, List.of("second", "first"));
        assertNull(resolve.invoke(snapshot, 2, stale));
        assertNull(resolve.invoke(snapshot, 3, token.invoke(snapshot, 0)));
        assertNull(resolve.invoke(snapshot, 2, "forged"));
    }
    @Test void unchangedVisibleRowsKeepTokensAndChangingSelectedSlotInvalidatesThem() throws Exception {
        var type = Class.forName("dev.overloadsim.output.OutputTargetSnapshot");
        var snapshot = type.getConstructor().newInstance();
        var refresh = type.getMethod("refresh", int.class, List.class);
        var token = type.getMethod("token", int.class);
        var resolve = type.getMethod("resolve", int.class, String.class);
        refresh.invoke(snapshot, 35, List.of("destination"));
        String old = (String) token.invoke(snapshot, 0);
        refresh.invoke(snapshot, 35, List.of("destination"));
        assertEquals(old, token.invoke(snapshot, 0));
        refresh.invoke(snapshot, 0, List.of("destination"));
        assertNull(resolve.invoke(snapshot, 0, old));
        assertNull(resolve.invoke(snapshot, 0, null));
    }
}
