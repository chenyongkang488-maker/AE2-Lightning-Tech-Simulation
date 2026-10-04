package dev.overloadsim.core;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiverShellRegistryTest {
    @Test void hidesOnlyPublishedCellsAndReportsBothEndsOfAMove() throws Exception {
        var registry = new Registry(); var token = new Object();
        var oldCell = registry.point(10, 64, 11); var newCell = registry.point(30, 64, 31);
        assertEquals(Set.of(oldCell), registry.replace(token, 10, 64, 10, Set.of(oldCell)));
        assertTrue(registry.hidden(10, 64, 11)); assertFalse(registry.hidden(10, 64, 12));
        assertEquals(Set.of(oldCell, newCell), registry.replace(token, 10, 64, 10, Set.of(newCell)));
        assertFalse(registry.hidden(10, 64, 11)); assertTrue(registry.hidden(30, 64, 31));
        assertEquals(Set.of(), registry.replace(token, 10, 64, 10, Set.of(newCell)));
    }

    @Test void lateRemovalOfOldControllerCannotEraseItsReplacement() throws Exception {
        var registry = new Registry(); var oldToken = new Object(); var replacement = new Object();
        var cell = registry.point(0, 3, 0);
        registry.replace(oldToken, 0, 0, 0, Set.of(cell));
        registry.replace(replacement, 0, 0, 0, Set.of(cell));
        assertEquals(Set.of(), registry.remove(oldToken, 0, 0, 0));
        assertTrue(registry.hidden(0, 3, 0));
        assertEquals(Set.of(cell), registry.remove(replacement, 0, 0, 0));
        assertFalse(registry.hidden(0, 3, 0));
    }

    @Test void invalidationRestoresItsShellWhileOtherStructuresRemainOwned() throws Exception {
        var registry = new Registry(); var first = new Object(); var second = new Object();
        var firstCell = registry.point(1, 3, 0); var shared = registry.point(2, 3, 0); var secondCell = registry.point(3, 3, 0);
        registry.replace(first, 0, 0, 0, Set.of(firstCell, shared));
        registry.replace(second, 4, 0, 0, Set.of(shared, secondCell));
        assertEquals(Set.of(firstCell), registry.replace(first, 0, 0, 0, Set.of()));
        assertFalse(registry.hidden(1, 3, 0)); assertTrue(registry.hidden(2, 3, 0)); assertTrue(registry.hidden(3, 3, 0));
        assertEquals(Set.of(shared, secondCell), registry.remove(second, 4, 0, 0));
    }

    @Test void rendererSnapshotCannotBeChangedByTheCaller() throws Exception {
        var registry = new Registry(); var token = new Object(); var cell = registry.point(1, 2, 3);
        var cells = new HashSet<>(); cells.add(cell);
        registry.replace(token, 0, 0, 0, cells); cells.clear();
        assertTrue(registry.hidden(1, 2, 3));
    }

    @Test void unloadingOneMemberChunkRestoresTheWholeOwnerShell() throws Exception {
        var registry = new Registry(); var crossing = new Object(); var neighbor = new Object();
        var near = registry.point(15, 64, 0); var far = registry.point(17, 64, 0); var other = registry.point(40, 64, 0);
        registry.replace(crossing, 15, 61, 0, Set.of(near, far));
        registry.replace(neighbor, 40, 61, 0, Set.of(other));
        assertEquals(Set.of(near, far), registry.type.getMethod("unloadChunk", int.class, int.class).invoke(registry.value, 1, 0));
        assertFalse(registry.hidden(15, 64, 0)); assertFalse(registry.hidden(17, 64, 0));
        assertTrue(registry.hidden(40, 64, 0));
        registry.replace(crossing, 15, 61, 0, Set.of(near, far));
        assertTrue(registry.hidden(15, 64, 0)); assertTrue(registry.hidden(17, 64, 0));
    }

    @Test void losingChunkAvailabilityWithoutAnUnloadEventRestoresTheShell() throws Exception {
        var registry = new Registry(); var crossing = new Object(); var neighbor = new Object();
        var near = registry.point(15, 64, 0); var far = registry.point(17, 64, 0); var other = registry.point(40, 64, 0);
        registry.replace(crossing, 15, 61, 0, Set.of(near, far));
        registry.replace(neighbor, 40, 61, 0, Set.of(other));
        var calls = new java.util.HashMap<String, Integer>();
        java.util.function.BiPredicate<Integer, Integer> available = (x, z) -> { calls.merge(x + ":" + z, 1, Integer::sum); return true; };
        var prune = registry.type.getMethod("pruneUnavailable", java.util.function.BiPredicate.class);
        assertEquals(Set.of(), prune.invoke(registry.value, available));
        assertEquals(java.util.Map.of("0:0", 1, "1:0", 1, "2:0", 1), calls, "Availability checks use unique chunk identities");
        java.util.function.BiPredicate<Integer, Integer> loaded = (x, z) -> x != 1;
        assertEquals(Set.of(near, far), prune.invoke(registry.value, loaded));
        assertFalse(registry.hidden(15, 64, 0)); assertFalse(registry.hidden(17, 64, 0));
        assertTrue(registry.hidden(40, 64, 0));
    }

    private static final class Registry {
        final Class<?> type = Class.forName("dev.overloadsim.core.ReceiverShellRegistry");
        final Object value = type.getConstructor().newInstance();
        final Class<?> pointType = Class.forName("dev.overloadsim.core.ReceiverShellRegistry$Point");
        Registry() throws Exception {}
        Object point(int x, int y, int z) throws Exception { return pointType.getConstructor(int.class, int.class, int.class).newInstance(x, y, z); }
        Object replace(Object token, int x, int y, int z, Set<?> cells) throws Exception {
            return type.getMethod("replace", Object.class, int.class, int.class, int.class, Set.class).invoke(value, token, x, y, z, cells);
        }
        Object remove(Object token, int x, int y, int z) throws Exception {
            return type.getMethod("remove", Object.class, int.class, int.class, int.class).invoke(value, token, x, y, z);
        }
        boolean hidden(int x, int y, int z) throws Exception { return (boolean) type.getMethod("hidden", int.class, int.class, int.class).invoke(value, x, y, z); }
    }
}
