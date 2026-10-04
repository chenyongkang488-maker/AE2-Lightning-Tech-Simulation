package dev.overloadsim.core;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/** Client-owned cells with immutable snapshots for concurrent chunk model baking. */
public final class ReceiverShellRegistry {
    public record Point(int x, int y, int z) {}
    private record Chunk(int x, int z) {}
    private record Entry(Object token, Set<Point> cells, Set<Chunk> chunks) {}
    private record Snapshot(Set<Point> cells, Map<Point, Entry> owners) {}
    private final Map<Point, Entry> owners = new LinkedHashMap<>();
    private volatile Snapshot snapshot = new Snapshot(Set.of(), Map.of());

    public boolean hidden(int x, int y, int z) { return snapshot.cells.contains(new Point(x, y, z)); }
    public boolean owns(Object token, int x, int y, int z) {
        var entry = snapshot.owners.get(new Point(x, y, z));
        return entry != null && entry.token == token && !entry.cells.isEmpty();
    }

    public synchronized Set<Point> replace(Object token, int x, int y, int z, Set<Point> cells) {
        var owner = new Point(x, y, z);
        var chunks = new HashSet<Chunk>();
        chunks.add(new Chunk(x >> 4, z >> 4));
        for (var cell : cells) chunks.add(new Chunk(cell.x >> 4, cell.z >> 4));
        var next = new Entry(token, Set.copyOf(cells), Set.copyOf(chunks));
        var previous = owners.get(owner);
        if (previous != null && previous.token == token && previous.cells.equals(next.cells)) return Set.of();
        owners.put(owner, next);
        return rebuild();
    }

    public synchronized Set<Point> remove(Object token, int x, int y, int z) {
        var owner = new Point(x, y, z);
        var previous = owners.get(owner);
        if (previous == null || previous.token != token) return Set.of();
        owners.remove(owner);
        return rebuild();
    }

    public synchronized Set<Point> unloadChunk(int chunkX, int chunkZ) {
        var chunk = new Chunk(chunkX, chunkZ);
        boolean removed = owners.values().removeIf(entry -> entry.chunks.contains(chunk));
        return removed ? rebuild() : Set.of();
    }

    /** View-distance changes can evict client chunks without firing a chunk-unload event. */
    public synchronized Set<Point> pruneUnavailable(BiPredicate<Integer, Integer> loaded) {
        boolean removed = owners.values().removeIf(entry -> entry.chunks.stream().anyMatch(chunk -> !loaded.test(chunk.x, chunk.z)));
        return removed ? rebuild() : Set.of();
    }
    private Set<Point> rebuild() {
        var cells = new HashSet<Point>();
        for (var owner : owners.values()) cells.addAll(owner.cells);
        var changed = new HashSet<>(snapshot.cells);
        for (var point : cells) if (!changed.add(point)) changed.remove(point);
        snapshot = new Snapshot(Set.copyOf(cells), Map.copyOf(owners));
        return Set.copyOf(changed);
    }
}
