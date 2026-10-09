package dev.overloadsim.laser;

import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.config.MachineSettings;
import java.math.BigInteger;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

/** A connected deflector circuit settles once per server batch, after all its inputs are known. */
final class DeflectorBatchRouter {
    private static final Comparator<LaserMachineBlockEntity> ORDER = Comparator.comparingLong(n -> n.getBlockPos().asLong());
    private record SharedLine(LaserSegment line, Map<LaserMachineBlockEntity, Long> shares) {}
    /** All contributor paths share this lease object, including during reentrant lifecycle callbacks. */
    private static final class SharedTarget {
        private final LaserNetwork.ExternalTarget target;
        private final Map<LaserMachineBlockEntity, Long> shares;
        private boolean revoked;
        SharedTarget(LaserNetwork.ExternalTarget target, Map<LaserMachineBlockEntity, Long> shares) {
            this.target = target; this.shares = shares;
        }
        LaserNetwork.ExternalTarget target() { return target; }
        Map<LaserMachineBlockEntity, Long> shares() { return shares; }
        void deactivate() {
            if (revoked) return;
            revoked = true;
            target.hook().deactivate(target.context());
        }
    }
    private record Route(List<SharedLine> lines, List<SharedTarget> targets,
            Map<LaserMachineBlockEntity, Long> powers, long until) {}
    private static final Map<LaserMachineBlockEntity, Route> ROUTES = new WeakHashMap<>();
    private static final Map<LaserMachineBlockEntity, Long> PROCESSED = new WeakHashMap<>();
    private static final class BatchClaims { long tick = -1; final Set<BlockPos> matrices = new HashSet<>(); }
    private static final Map<ServerLevel, BatchClaims> CLAIMS = new WeakHashMap<>();
    private record Beam(BlockPos start, Direction direction, long energy, int range,
            Set<BlockPos> visited, Map<LaserMachineBlockEntity, Long> shares,
            LaserMachineBlockEntity physicalSource) {}
    private DeflectorBatchRouter() {}

    private static boolean live(LaserMachineBlockEntity root, Route route) {
        return !root.isRemoved() && root.enabled() && root.formed() && root.getLevel() != null
                && route.until() > root.getLevel().getGameTime();
    }
    static boolean retained(LaserMachineBlockEntity node) {
        return ROUTES.entrySet().stream().anyMatch(e -> live(e.getKey(), e.getValue()) && e.getValue().powers().containsKey(node));
    }
    static boolean ownsNode(LaserMachineBlockEntity root, LaserMachineBlockEntity node) {
        var owner = ROUTES.entrySet().stream().filter(e -> live(e.getKey(), e.getValue()) && e.getValue().powers().containsKey(node))
                .map(Map.Entry::getKey).min(ORDER).orElse(null);
        return owner == null || owner == root;
    }
    private static LaserMachineBlockEntity visualOwner(Map<LaserMachineBlockEntity, Long> shares) {
        return shares.keySet().stream().filter(root -> {
            var route = ROUTES.get(root); return route != null && live(root, route);
        }).min(ORDER).orElse(null);
    }
    static void remove(LaserMachineBlockEntity root, boolean stopped) {
        var old = ROUTES.remove(root);
        if (old == null) return;
        refresh();
        if (!stopped) return;
        for (var shared : old.targets()) {
            long remaining = activePower(shared.shares());
            var target = shared.target();
            if (remaining > 0) {
                var c = target.context();
                target.hook().refreshLease(new LaserExtensions.TargetContext(c.level(), c.position(), c.incoming(),
                        remaining, c.batchTick(), c.source(), c.sourceIdentity()));
            } else if (!targetRetained(target)) shared.deactivate();
        }
    }
    private static boolean targetRetained(LaserNetwork.ExternalTarget target) {
        return ROUTES.entrySet().stream().filter(e -> live(e.getKey(), e.getValue()))
                .flatMap(e -> e.getValue().targets().stream()).anyMatch(s -> sameTarget(s.target(), target));
    }
    private static boolean sameTarget(LaserNetwork.ExternalTarget a, LaserNetwork.ExternalTarget b) {
        return a.hook() == b.hook() && a.context().position().equals(b.context().position())
                && Objects.equals(a.context().sourceIdentity(), b.context().sourceIdentity());
    }
    private static long activePower(Map<LaserMachineBlockEntity, Long> shares) {
        long sum = 0;
        for (var e : shares.entrySet()) {
            var route = ROUTES.get(e.getKey());
            if (route != null && live(e.getKey(), route)) sum = Math.addExact(sum, e.getValue());
        }
        return sum;
    }
    private static void refresh() {
        var totals = new HashMap<LaserMachineBlockEntity, Long>();
        var leases = new HashMap<LaserMachineBlockEntity, Long>();
        for (var entry : List.copyOf(ROUTES.entrySet())) {
            var root = entry.getKey(); var route = entry.getValue();
            if (!live(root, route)) continue;
            var lines = new ArrayList<LaserSegment>();
            for (var shared : route.lines()) {
                if (visualOwner(shared.shares()) != root) continue;
                long power = activePower(shared.shares());
                if (power <= 0) continue;
                var l = shared.line();
                lines.add(new LaserSegment(l.start(), l.end(), power, l.focused(), l.particle(), l.particleCount(), l.launchTick()));
            }
            root.segments = List.copyOf(lines); root.refreshLaserLights(); root.sync();
            route.powers().forEach((node, power) -> {
                totals.merge(node, power, Math::addExact);
                leases.merge(node, route.until(), Math::max);
            });
        }
        totals.forEach((node, power) -> { node.lastPower = power; node.activeUntil = leases.get(node); node.status = "working"; node.sync(); });
    }

    static boolean process(LaserMachineBlockEntity requested) {
        if (!(requested.getLevel() instanceof ServerLevel level)) return false;
        requested.checkStructure();
        if (!requested.enabled() || !requested.formed()) return false;
        long now = level.getGameTime();
        if (Objects.equals(PROCESSED.get(requested), now)) {
            var path = LaserNetwork.PATHS.get(requested);
            if (path != null && !LaserNetwork.valid(level, path)) LaserNetwork.clear(requested, true);
            return true;
        }
        var candidates = new ArrayList<>(LaserStructureIndex.controllers(level).stream().limit(256).toList());
        if (!candidates.contains(requested)) candidates.add(requested);
        for (var node : candidates) node.checkStructure();
        var ports = ports(level, candidates);
        var edges = new HashMap<LaserMachineBlockEntity, Set<LaserMachineBlockEntity>>();
        for (var matrix : candidates) if (matrix.kind() == LaserKind.DEFLECTOR && matrix.formed() && matrix.enabled()) {
            var next = new LinkedHashSet<LaserMachineBlockEntity>();
            for (var side : outputs(matrix)) {
                var target = nextMatrix(level, ports, matrix.center().relative(side, 3), side, MachineSettings.intValue(MachineSettings.LASER_DEFLECTOR_RANGE));
                if (target != null) next.add(target);
            }
            edges.put(matrix, next);
        }
        var reaches = new HashMap<LaserMachineBlockEntity, Set<LaserMachineBlockEntity>>();
        for (var source : candidates) if (source.kind() == LaserKind.EMITTER && source.formed() && source.enabled()) {
            var first = nextMatrix(level, ports, source.center().relative(source.forward(), 2), source.forward(), MachineSettings.intValue(MachineSettings.LASER_EMITTER_RANGE));
            var reachable = new LinkedHashSet<LaserMachineBlockEntity>();
            if (first != null) reachable(first, edges, reachable);
            reaches.put(source, reachable);
        }
        var component = new LinkedHashSet<>(reaches.getOrDefault(requested, Set.of()));
        if (component.isEmpty()) {
            if (ROUTES.containsKey(requested)) LaserNetwork.clear(requested, true);
            return false;
        }
        var sources = new LinkedHashSet<LaserMachineBlockEntity>(); sources.add(requested);
        boolean changed;
        do {
            changed = false;
            for (var e : reaches.entrySet()) if (!Collections.disjoint(component, e.getValue())) {
                if (sources.add(e.getKey())) changed = true;
                if (component.addAll(e.getValue())) changed = true;
            }
        } while (changed);
        var claims = CLAIMS.computeIfAbsent(level, ignored -> new BatchClaims());
        if (claims.tick != now) { claims.tick = now; claims.matrices.clear(); }
        // Source replacement or a newly enabled input cannot reopen an already settled physical matrix this tick.
        if (component.stream().anyMatch(matrix -> claims.matrices.contains(matrix.getBlockPos()))) return true;
        component.forEach(matrix -> claims.matrices.add(matrix.getBlockPos().immutable()));
        var order = new ArrayList<LaserMachineBlockEntity>(); var state = new HashMap<LaserMachineBlockEntity, Integer>();
        for (var matrix : component.stream().sorted(ORDER).toList()) sort(matrix, edges, state, order);
        Collections.reverse(order);
        var batch = new Batch(level, now, ports, order);
        var previous = new ArrayList<LaserNetwork.ExternalTarget>();
        var previousCores = new HashSet<LaserMachineBlockEntity>();
        for (var source : sources.stream().sorted(ORDER).toList()) {
            var old = ROUTES.get(source); if (old != null) old.targets().forEach(target -> previous.add(target.target()));
            var oldPath = LaserNetwork.PATHS.get(source);
            if (oldPath != null) {
                previous.addAll(oldPath.targets());
                for (var node : oldPath.nodes()) if (node.kind() == LaserKind.CORE) previousCores.add(node);
            }
            LaserNetwork.clear(source, false);
            PROCESSED.put(source, now);
            source.checkStructure();
            if(LaserSettings.emitterFe()>MachineSettings.beamLimit()){source.status="beam_limit";source.refreshLaserLights();source.sync();continue;}
            if (!source.enabled() || !source.formed() || !source.tryPay(LaserSettings.emitterFe(), 0, false)) {
                source.status = "no_energy"; source.refreshLaserLights(); source.sync(); continue;
            }
            batch.roots.add(source);
            batch.nodes.put(source, new LinkedHashSet<>(Set.of(source)));
            batch.powers.put(source, new HashMap<>(Map.of(source, LaserSettings.emitterFe())));
            batch.queue.add(new Beam(source.center().relative(source.forward(), 2), source.forward(), LaserSettings.emitterFe(), MachineSettings.intValue(MachineSettings.LASER_EMITTER_RANGE),
                    new HashSet<>(Set.of(source.getBlockPos())), Map.of(source, LaserSettings.emitterFe()), source));
        }
        batch.run(); batch.publish();
        for (var core : previousCores) if (!retained(core)) { core.stopCollapseVisuals(); core.sync(); }
        var revoked = new ArrayList<LaserNetwork.ExternalTarget>();
        for (var old : previous) if (!targetRetained(old)
                && batch.targets.stream().noneMatch(t -> sameTarget(t.target(), old))
                && revoked.stream().noneMatch(t -> sameTarget(t, old))) {
            old.hook().deactivate(old.context()); revoked.add(old);
        }
        return true;
    }
    private static void reachable(LaserMachineBlockEntity node, Map<LaserMachineBlockEntity, Set<LaserMachineBlockEntity>> edges, Set<LaserMachineBlockEntity> seen) {
        if (!seen.add(node)) return;
        for (var next : edges.getOrDefault(node, Set.of())) reachable(next, edges, seen);
    }
    /** Removing DFS back edges gives a finite evaluation order even when a shell port points into a cycle. */
    private static void sort(LaserMachineBlockEntity node, Map<LaserMachineBlockEntity, Set<LaserMachineBlockEntity>> edges,
            Map<LaserMachineBlockEntity, Integer> state, List<LaserMachineBlockEntity> order) {
        if (state.containsKey(node)) return;
        state.put(node, 1);
        for (var next : edges.getOrDefault(node, Set.of()).stream().sorted(ORDER).toList()) sort(next, edges, state, order);
        state.put(node, 2); order.add(node);
    }
    private static List<Direction> outputs(LaserMachineBlockEntity matrix) {
        return Arrays.stream(Direction.values()).filter(d -> d.getAxis() != Direction.Axis.Y
                && matrix.sideMode(d) == LaserMachineBlockEntity.SIDE_OUTPUT
                && !matrix.center().relative(d, 3).equals(matrix.getBlockPos())).toList();
    }
    private static Map<BlockPos, LaserMachineBlockEntity> ports(ServerLevel level, List<LaserMachineBlockEntity> candidates) {
        var ports = new HashMap<BlockPos, LaserMachineBlockEntity>();
        for (var node : candidates) {
            if (!node.formed() || !node.enabled()) continue;
            if (node.kind() == LaserKind.CORE) ports.put(node.getBlockPos(), node);
            else if (node.kind() == LaserKind.DEFLECTOR) for (var side : Direction.Plane.HORIZONTAL) {
                var port = node.center().relative(side, 3);
                if (!port.equals(node.getBlockPos()) && level.getBlockState(port).is(LaserContent.PORT.get())) ports.put(port, node);
            }
            else if (node.kind() == LaserKind.RECEIVER) ports.put(node.center().relative(node.forward().getOpposite(), 3), node);
            else if (node.kind() != LaserKind.EMITTER) ports.put(node.center(), node);
        }
        return ports;
    }
    private static LaserMachineBlockEntity nextMatrix(ServerLevel level, Map<BlockPos, LaserMachineBlockEntity> ports,
            BlockPos start, Direction direction, int range) {
        var visited = new HashSet<BlockPos>();
        for (int hops = 0; hops < 128 && range > 0; hops++) {
            boolean continued = false;
            for (int distance = 1; distance <= range; distance++) {
                var pos = start.relative(direction, distance); if (!level.hasChunkAt(pos)) return null;
                var target = ports.get(pos);
                if (target != null) {
                    if (target.kind() == LaserKind.DEFLECTOR) return target.sideMode(direction.getOpposite()) == LaserMachineBlockEntity.SIDE_INPUT ? target : null;
                    if (target.forward() != direction || target.kind() == LaserKind.CORE || target.kind() == LaserKind.RECEIVER || target.kind() == LaserKind.FOCUS
                            || !visited.add(target.getBlockPos())) return null;
                    range = target.kind() == LaserKind.CONSTRAINT ? MachineSettings.intValue(MachineSettings.LASER_CONSTRAINT_RANGE) : range - distance;
                    start = pos; continued = true; break;
                }
                var block = level.getBlockState(pos); var id = BuiltInRegistries.BLOCK.getKey(block.getBlock());
                if (LaserExtensions.target(id) != null) return null;
                if (LaserExtensions.modifier(id) != null) {
                    if (!visited.add(pos)) return null;
                    start = pos; range = 64; continued = true; break;
                }
                if (!block.getCollisionShape(level, pos).isEmpty()) return null;
            }
            if (!continued) return null;
        }
        return null;
    }

    /** Cumulative integer allocation preserves the total exactly without multiplying long energy values. */
    private static Map<LaserMachineBlockEntity, Long> scale(Map<LaserMachineBlockEntity, Long> shares, long total) {
        long original = shares.values().stream().mapToLong(Long::longValue).sum(), seen = 0, allocated = 0;
        var result = new LinkedHashMap<LaserMachineBlockEntity, Long>();
        if (original <= 0 || total <= 0) return result;
        for (var source : shares.keySet().stream().sorted(ORDER).toList()) {
            seen += shares.get(source);
            long next = BigInteger.valueOf(total).multiply(BigInteger.valueOf(seen)).divide(BigInteger.valueOf(original)).longValueExact();
            if (next > allocated) result.put(source, next - allocated);
            allocated = next;
        }
        return Map.copyOf(result);
    }

    private static final class Batch {
        final ServerLevel level; final long now; final Map<BlockPos, LaserMachineBlockEntity> ports; final List<LaserMachineBlockEntity> order;
        final Set<LaserMachineBlockEntity> roots = new LinkedHashSet<>();
        final Map<LaserMachineBlockEntity, Set<LaserMachineBlockEntity>> nodes = new HashMap<>();
        final Map<LaserMachineBlockEntity, Map<LaserMachineBlockEntity, Long>> powers = new HashMap<>();
        final List<SharedLine> lines = new ArrayList<>(); final List<SharedTarget> targets = new ArrayList<>();
        final Map<LaserMachineBlockEntity, Set<BlockPos>> clearCells = new HashMap<>();
        final Map<LaserMachineBlockEntity, Map<BlockPos, BlockState>> watched = new HashMap<>();
        final ArrayDeque<Beam> queue = new ArrayDeque<>();
        final Map<LaserMachineBlockEntity, List<Beam>> inputs = new HashMap<>();
        final Set<BlockPos> claims = new HashSet<>(); final Set<LaserMachineBlockEntity> evaluated = new HashSet<>();
        int hops, steps;
        Batch(ServerLevel level, long now, Map<BlockPos, LaserMachineBlockEntity> ports, List<LaserMachineBlockEntity> order) {
            this.level = level; this.now = now; this.ports = ports; this.order = order;
        }
        void line(BlockPos from, BlockPos to, long energy, Map<LaserMachineBlockEntity, Long> shares) {
            if (lines.size() < 128 && !from.equals(to)) lines.add(new SharedLine(new LaserSegment(from.getCenter(), to.getCenter(), energy, false, false), shares));
        }
        void activity(LaserMachineBlockEntity node, Map<LaserMachineBlockEntity, Long> shares) {
            for (var e : shares.entrySet()) if (nodes.containsKey(e.getKey())) {
                nodes.get(e.getKey()).add(node); powers.get(e.getKey()).merge(node, e.getValue(), Long::sum);
            }
        }
        void cell(BlockPos position, Map<LaserMachineBlockEntity, Long> shares) {
            for (var root : shares.keySet()) clearCells.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).add(position);
        }
        void watch(BlockPos position, BlockState state, Map<LaserMachineBlockEntity, Long> shares) {
            for (var root : shares.keySet()) watched.computeIfAbsent(root, ignored -> new HashMap<>()).put(position.immutable(), state);
        }
        Map<LaserMachineBlockEntity, Long> validShares(Map<LaserMachineBlockEntity, Long> shares) {
            var valid = new HashMap<LaserMachineBlockEntity, Long>();
            for (var e : shares.entrySet()) {
                var root = e.getKey();
                if (root.isRemoved() || !root.enabled() || root.getLevel() != level || !level.hasChunkAt(root.getBlockPos())
                        || level.getBlockEntity(root.getBlockPos()) != root) continue;
                var prefix = new LaserNetwork.Path(List.copyOf(nodes.getOrDefault(root, Set.of())),
                        List.copyOf(clearCells.getOrDefault(root, Set.of())), Map.copyOf(watched.getOrDefault(root, Map.of())), List.of());
                if (LaserNetwork.valid(level, prefix)) valid.put(root, e.getValue());
            }
            return Map.copyOf(valid);
        }
        Beam prune(Beam beam) {
            if (beam.physicalSource().kind() == LaserKind.DEFLECTOR
                    && beam.physicalSource().sideMode(beam.direction()) != LaserMachineBlockEntity.SIDE_OUTPUT) return null;
            var valid = validShares(beam.shares()); long energy = valid.values().stream().mapToLong(Long::longValue).sum();
            return energy <= 0 ? null : new Beam(beam.start(), beam.direction(), energy, beam.range(), beam.visited(), valid, beam.physicalSource());
        }
        void run() {
            drain();
            for (var matrix : order) {
                evaluated.add(matrix);
                var arriving = inputs.getOrDefault(matrix, List.of()).stream().map(this::prune).filter(Objects::nonNull).toList();
                if (arriving.isEmpty()) continue;
                long total = 0; var shares = new HashMap<LaserMachineBlockEntity, Long>(); var visited = new HashSet<BlockPos>();
                boolean overLimit = false;
                for (var beam : arriving) {
                    try { total = LaserMath.boost(total, beam.energy()); } catch (IllegalArgumentException tooStrong) { overLimit = true; }
                    beam.shares().forEach((source, power) -> shares.merge(source, power, Long::sum)); visited.addAll(beam.visited());
                }
                if (overLimit || total>MachineSettings.beamLimit()) { matrix.status = "beam_limit"; matrix.activeUntil = 0; matrix.lastPower = 0; matrix.sync(); continue; }
                var event = new LaserEvents.BeforeModifier(level, matrix.getBlockPos(), BuiltInRegistries.BLOCK.getKey(matrix.getBlockState().getBlock()), total, total, MachineSettings.intValue(MachineSettings.LASER_DEFLECTOR_RANGE));
                NeoForge.EVENT_BUS.post(event); matrix.checkStructure();
                if (event.isCanceled() || !matrix.formed() || !matrix.enabled()
                        || arriving.stream().anyMatch(b -> matrix.sideMode(b.direction().getOpposite()) != LaserMachineBlockEntity.SIDE_INPUT)) continue;
                if(event.outputEnergy()>MachineSettings.beamLimit()){matrix.status="beam_limit";matrix.sync();continue;}
                var allocationShares = validShares(scale(shares, event.outputEnergy()));
                long power = allocationShares.values().stream().mapToLong(Long::longValue).sum();
                var faces = outputs(matrix); if (power <= 0 || faces.isEmpty()) { matrix.status = faces.isEmpty() ? "no_outputs" : "idle"; matrix.sync(); continue; }
                visited.add(matrix.getBlockPos()); activity(matrix, allocationShares);
                var split = LaserMath.split(power, faces.size());
                // Allocate each source across output columns; row and column totals both remain exact.
                var outputShares = new ArrayList<Map<LaserMachineBlockEntity, Long>>();
                for (int i = 0; i < faces.size(); i++) outputShares.add(new HashMap<>());
                long cumulative = 0; long[] previous = new long[faces.size()];
                for (var source : allocationShares.keySet().stream().sorted(ORDER).toList()) {
                    cumulative += allocationShares.get(source); var current = LaserMath.split(cumulative, faces.size());
                    for (int i = 0; i < faces.size(); i++) if (current[i] > previous[i]) outputShares.get(i).put(source, current[i] - previous[i]);
                    previous = current;
                }
                for (int i = 0; i < faces.size(); i++) if (split[i] > 0) {
                    var d = faces.get(i); var out = matrix.center().relative(d, 3); var allocation = Map.copyOf(outputShares.get(i));
                    line(matrix.center(), out, split[i], allocation);
                    if(event.range()>0)queue.add(new Beam(out, d, split[i], event.range(), new HashSet<>(visited), allocation, matrix));
                }
                drain();
            }
        }
        void drain() {
            while (!queue.isEmpty() && hops++ < 128 && steps < 512 && lines.size() < 128) {
                var beam = prune(queue.removeFirst()); if (beam == null) continue;
                var end = beam.start(); boolean drawn = false;
                for (int distance = 1; distance <= beam.range() && steps++ < 512; distance++) {
                    var pos = beam.start().relative(beam.direction(), distance); if (!level.hasChunkAt(pos)) break;
                    var target = ports.get(pos);
                    if (target != null) {
                        end = pos; line(beam.start(), end, beam.energy(), beam.shares()); drawn = true;
                        if (target.kind() == LaserKind.DEFLECTOR) {
                            var face = beam.direction().getOpposite();
                            if (target.sideMode(face) != LaserMachineBlockEntity.SIDE_INPUT || !target.center().relative(face, 3).equals(pos)
                                    || beam.visited().contains(target.getBlockPos()) || evaluated.contains(target) || !claims.add(pos)) break;
                            line(pos, target.center(), beam.energy(), beam.shares());
                            inputs.computeIfAbsent(target, ignored -> new ArrayList<>()).add(beam); break;
                        }
                        if (target.forward() != beam.direction() || target.kind() == LaserKind.CORE || target.kind() == LaserKind.RECEIVER
                                || beam.visited().contains(target.getBlockPos()) || !claims.add(target.getBlockPos())) break;
                        long power = beam.energy(), cost = 0, ehv = 0; int range = beam.range() - distance;
                        try {
                            switch (target.kind()) {
                                case CONSTRAINT -> { cost = LaserSettings.constraintFe(); range = MachineSettings.intValue(MachineSettings.LASER_CONSTRAINT_RANGE); }
                                case ENHANCEMENT -> { cost = target.boost(); power = LaserMath.boost(power, cost); }
                                case BURST -> { ehv = LaserSettings.burstEhv(); power = MachineSettings.amplify(power); }
                                case FOCUS -> range = 7;
                                default -> {}
                            }
                        } catch (IllegalArgumentException tooStrong) { target.status = "beam_limit"; target.sync(); break; }
                        var event = new LaserEvents.BeforeModifier(level, target.getBlockPos(), BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()), beam.energy(), power, range);
                        NeoForge.EVENT_BUS.post(event); target.checkStructure();
                        if (event.isCanceled() || !target.formed() || !target.enabled()) break;
                        if(event.outputEnergy()>MachineSettings.beamLimit()){target.status="beam_limit";target.sync();break;}
                        var allocation = validShares(scale(beam.shares(), event.outputEnergy()));
                        power = allocation.values().stream().mapToLong(Long::longValue).sum(); range = event.range();
                        if (power <= 0 || !target.tryPay(cost, ehv, false)) break;
                        activity(target, allocation);
                        target.lastPower = power; target.activeUntil = now + 22; target.status = "working"; target.sync();
                        var visited = new HashSet<>(beam.visited()); visited.add(target.getBlockPos());
                        if (target.kind() == LaserKind.FOCUS) {
                            var focusLines = new ArrayList<LaserSegment>(); var focusNodes = new ArrayList<LaserMachineBlockEntity>();
                            var focusCells = new ArrayList<BlockPos>(); var focusWatched = new HashMap<BlockPos, BlockState>();
                            LaserNetwork.focus(level, target, power, now, focusLines, focusNodes, focusCells, focusWatched,
                                    () -> validShares(allocation).values().stream().mapToLong(Long::longValue).sum());
                            var focusAllocation = validShares(allocation);
                            for (var posToWatch : focusCells) cell(posToWatch, allocation);
                            focusWatched.forEach((position, state) -> watch(position, state, allocation));
                            for (var l : focusLines) if (lines.size() < 128) lines.add(new SharedLine(l, scale(focusAllocation, l.energy())));
                            for (var node : focusNodes) activity(node, scale(focusAllocation, node.lastPower));
                        } else if (range > 0) queue.add(new Beam(pos, beam.direction(), power, range, visited, allocation, beam.physicalSource()));
                        break;
                    }
                    var state = level.getBlockState(pos); var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    var customTarget = LaserExtensions.target(id);
                    if (customTarget != null) {
                        watch(pos, state, beam.shares());
                        var physical = beam.physicalSource();
                        var context = new LaserExtensions.TargetContext(level, pos.immutable(), beam.direction(), beam.energy(), now, physical.getBlockPos(), physical.identity());
                        if (customTarget.accept(context)) { targets.add(new SharedTarget(new LaserNetwork.ExternalTarget(customTarget, context), beam.shares())); end = pos; break; }
                    }
                    var custom = LaserExtensions.modifier(id);
                    if (custom != null) {
                        watch(pos, state, beam.shares()); end = pos; line(beam.start(), end, beam.energy(), beam.shares()); drawn = true;
                        if (beam.visited().contains(pos)) break;
                        var result = custom.modify(new LaserExtensions.BeamContext(level, pos, beam.direction(), beam.energy(), beam.range() - distance, now));
                        if (result != null && level.hasChunkAt(pos)) {
                            var event = new LaserEvents.BeforeModifier(level, pos, id, beam.energy(), result.energy(), result.range()); NeoForge.EVENT_BUS.post(event);
                            if (!event.isCanceled() && level.hasChunkAt(pos) && level.getBlockState(pos).equals(state) && event.outputEnergy() > 0 && event.outputEnergy()<=MachineSettings.beamLimit() && event.range() > 0) {
                                var visited = new HashSet<>(beam.visited()); visited.add(pos);
                                var allocation = validShares(scale(beam.shares(), event.outputEnergy()));
                                long energy = allocation.values().stream().mapToLong(Long::longValue).sum();
                                if (energy > 0) queue.add(new Beam(pos, beam.direction(), energy, event.range(), visited, allocation, beam.physicalSource()));
                            }
                        }
                        break;
                    }
                    if (!state.getCollisionShape(level, pos).isEmpty()) { end = pos; break; }
                    end = pos; cell(pos, beam.shares());
                    if (distance == beam.range()) { line(beam.start(), end, beam.energy(), beam.shares()); drawn = true; }
                }
                if (!drawn) line(beam.start(), end, beam.energy(), beam.shares());
            }
        }
        void publish() {
            for (var root : roots) {
                if (root.isRemoved() || !root.enabled() || !root.formed()) continue;
                var ownedLines = lines.stream().filter(l -> l.shares().containsKey(root)).toList();
                var ownedTargets = targets.stream().filter(t -> t.shares().containsKey(root)).toList();
                var path = new LaserNetwork.Path(List.copyOf(nodes.get(root)),
                        List.copyOf(clearCells.getOrDefault(root, Set.of())), Map.copyOf(watched.getOrDefault(root, Map.of())), List.of());
                if (!LaserNetwork.valid(level, path)) {
                    root.segments = List.of(); root.lastPower = 0; root.activeUntil = 0; root.refreshLaserLights(); root.sync(); continue;
                }
                ROUTES.put(root, new Route(ownedLines, ownedTargets, Map.copyOf(powers.get(root)), now + 22));
                LaserNetwork.PATHS.put(root, path);
                root.activeUntil = now + 22; root.lastPower = LaserSettings.emitterFe(); root.status = "working";
            }
            refresh();
            for (var shared : targets) {
                long remaining = activePower(shared.shares()); var target = shared.target(); var c = target.context();
                if (remaining <= 0 && !targetRetained(target)) shared.deactivate();
                else if (remaining > 0 && remaining != c.energy()) target.hook().refreshLease(new LaserExtensions.TargetContext(c.level(), c.position(), c.incoming(),
                        remaining, c.batchTick(), c.source(), c.sourceIdentity()));
            }
        }
    }
}
