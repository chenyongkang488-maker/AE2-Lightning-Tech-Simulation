# Laser particle prototype implementation plan

> **For agentic workers:** Implement the locked interfaces below; independent server-machine, client-art and pure-accounting domains may run in parallel under superpowers:dispatching-parallel-agents. The root integrates, verifies and installs the result. User requested immediate implementation after reviewing the diagram; do not request another workflow approval.

**Goal:** Add a playable high-energy laser / particle processing chain to the existing mod and install a locally tested JAR into OverloadSim-Test-1.21.1.

**Architecture:** AE-networked controllers share bounded multiblock geometry and an ownership index. Server-authoritative 20-tick beam batches traverse input ports; clients render synchronized segments. Core outputs particles forward into a receiver; exact long energy/particle accounting and reloadable fuel rules govern conversion.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.252, Java 21, AE2 19.2.17, AE2LT Reborn 2.1.0, Thunderbolt Reborn 2.0.0. Applied Flux FE storage is optional and accessed through its AE key when present.

**Spec:** docs/superpowers/specs/2026-10-04-high-energy-laser-particle-proposal.md, with user's correction: particles leave the other side of the core in the laser's forward direction.

## Global constraints

- Preserve all existing IDs and gameplay. Local prototype version: 0.2.0-alpha.1.
- All eight machines connect through AE cabling or an optional upstream overloaded interface in their structure.
- Emitter 100,000 FE / 20 ticks; range 6; damage every 2 ticks = strength / 10,000.
- Ring outer diameter 7 and thickness 1, at least one empty block between laser matrix structures; deflector outer size 7³.
- Constraint renews six-block range, costing 100,000 FE; enhancement adds 100,000–9,000,000 FE and pays that amount; burst doubles and pays 64 EHV. Maximum 1,000,000,000,000,000 FE per beam.
- Focus terminates at block seven, clears breakable cells before the endpoint and respects unbreakable obstruction. Core threshold 1GFE; accumulate 512GFE per particle with remainder.
- Receiver located after core, input face toward core, within seven blocks; capacity 125,000 particles, energy capacity 1,024,000,000,000,000,000 FE.
- Consumable windows 100 effective ticks; particle intervals 100/20/10/2/1/1 and per-particle FE 512G/512G/256G/1024G/2048G/4096G. No extraction of inserted fuel.
- Overflow or breaking a charged receiver triggers a single bounded 3×3-chunk explosion; loaded chunks only, bounded per-tick edits, protection events honored.
- Blockbench-readable simple models reuse existing licensed white/pink textures; no detailed new paintings required.
- No publishing to GitHub or modifying other mod files. Back up the existing addon JAR when installing.

## Review focus

1. Optional Applied Flux / Mekanism absent: no linkage error, existing functionality still works.
2. Beam split, paid-resource failure, recursive optical paths: no duplication, no unpaid downstream work.
3. Broken/unloaded overlapping structures and reload: invalid owners never produce or retain dangerous visible beams.
4. Fuel automation/UI extraction, stale menus, saved high values: reject extraction and malformed operations, preserve exact amounts.
5. Explosion / diagnostic demo commands: once-only destruction, no forced chunk load, demo rejects occupied areas.

## Locked interfaces and file ownership

`dev.overloadsim.laser.LaserKind`: EMITTER, CONSTRAINT, ENHANCEMENT, BURST, DEFLECTOR, FOCUS, CORE, RECEIVER, each with `id()`.

`LaserMachineBlockEntity` exposes `kind()`, `forward()` (opposite block FACING), `center()`, `structure()`, `formed()`, `checkStructure()`, `tick()`, `sync()`, `tryPay(long fe,long ehv,boolean simulate)`, `acceptCore(long fe,Direction incoming,long tick)` returning generated particles, `acceptParticles(long count)` (overflow triggers explosion), `fuelInput()` and `energy()`.

Internal prototype runtime fields: `boolean enabled`; `long boost`, `extremeLightning`, `coreRemainder`, `activeUntil`, `lastPower`; `int outputMask`, `fuelRemaining`, `particleRemaining`; `String activeFuel`, `status`; `Direction particleDirection`; `List<LaserSegment> segments`; `ParticleBuffer particles`. Save/sync validation is mandatory. `energy()` returns `core.LongEnergyBuffer`. `fuelInput()` returns ItemStackHandler; machine automation is insert-only.

`LaserStructure`: `BlockPos center`, `List<BlockPos> members`, nullable `BlockPos networkInterface`; kinds CORE are single-block and always structurally valid. `LaserStructureGeometry.template(LaserKind,Direction)` returns relative required shell offsets/categories; validator and demo must share it.

`LaserContent`: `blocks()` map Kind→DeferredBlock; `machine(kind)` returns Block; `FRAME`, `GLASS`, `SOURCE`, `PORT`; `MACHINE_ENTITY`, `MENU`; `playerBlocks()`; `register(IEventBus)`, `setup()`, `capabilities(RegisterCapabilitiesEvent)`.

`LaserSegment(Vec3 start,Vec3 end,long energy,boolean focused,boolean particle)` has `write(RegistryFriendlyByteBuf)` and static `read`; list max 128.

`core.LaserMath`: exact checked boost/double/split, core accumulation with remainder, unit parsing; `core.LongEnergyBuffer`: stored()/capacity()/insert(amount,simulate)/extract(amount,simulate)/load(value); `core.ParticleBuffer`: stored()/receive(amount) (false on overflow, unchanged)/consume(amount)/load(value).

Root owns `LaserNetwork.tick(be)`, `LaserNetwork.register(be)`, `unregister(be)`, `LaserExplosion.trigger(be)`, `ParticleConversion.tick(be)`, `ParticleFuelData`; machine backend calls these. These owners must not modify client renderer/menu files.

Client owner creates `LaserMenu`, `client.LaserScreen`, `client.LaserRenderer`, placeholder assets, Blockbench project and guides; uses the public interfaces above. Root wires registration and creative tab after integration.

## Task 1: Pure resource accounting

- [ ] Write JUnit tests for split conservation, amplification cap, 512G remainder, 125,000 particle overflow, exact 1024P FE, conversion clocks and pause; run and observe missing feature failure.
- [ ] Implement LaserMath, LongEnergyBuffer, ParticleBuffer and conversion timing helper with checked arithmetic and exact unit parsing.
- [ ] Run targeted and full unit suites; commit changes.

## Task 2: Machines, geometry and AE bridge

- [ ] Add GameTests for rotated ring validation, emitter source, deflector ports, receiver input, structure ownership, long save/load, sealed fuel extraction and paid EHV failure.
- [ ] Implement the locked controller/structure interfaces, loaded-only validation, per-structure ownership and input-only fuel handling.
- [ ] Connect optional overloaded interface; optional FluxKey.of(EnergyType.FE) long storage insertion/extraction with bounded AE-power fallback. Deduct actual acceptance only.
- [ ] Verify GameTests with and without optional mods; commit changes.

## Task 3: Beam graph, core, receiver and explosion

- [ ] Add GameTests proving direction, seven-block focus, paid 20-tick batches, split conservation, break/unload stop, forbidden block obstruction, damage rhythm, fuel processing and once-only explosion.
- [ ] Implement bounded graph traversal and synchronized beam segments; deduplicate entity hits within one source batch; protect core endpoint.
- [ ] Implement core→receiver forward particle route, reloadable fuel rules / extension events, sealed effective fuel window and long FE output.
- [ ] Implement scheduled 3×3-chunk destructive effect and admin-only safe empty-area demonstration builder that precharges finite resources.
- [ ] Run unit and GameTest suite; commit changes.

## Task 4: Playable UI and placeholder visuals

- [ ] Add AE-style controller screen with enable, enhancement value and five output-face toggles; all changes validated server-side; fuel slot cannot be taken from.
- [ ] Render round pink laser, focused cone, active pixel sphere, forward particle marks and receiver containment field; use simple white/pink blocks and white destruction particles.
- [ ] Export Blockbench-readable prototype project/assets; add Chinese/English names and short build instructions / G guide.
- [ ] Verify JSON/model/texture references and real client load; commit changes.

## Task 5: Integration and installation

- [ ] Wire entry point, registries, creative tab, damage type and client registration. Increment to 0.2.0-alpha.1 and write changelog/API usage.
- [ ] Run build, full unit tests, GameTests with optional-mod matrix; request fresh whole-change review and address material defects.
- [ ] Verify generated JAR content and checksum; install via existing backup script into OverloadSim-Test-1.21.1, compare all other mod hashes.
- [ ] Record exact tests, installed path, backup, checksum and remaining visual limitations. Preserve local Git rollback history; no remote release.
