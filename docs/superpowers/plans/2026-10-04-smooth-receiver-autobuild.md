# Smooth receiver and automatic construction implementation plan

> **For agentic workers:** use dispatching-parallel-agents for the independent receiver visuals and chamber integration; root owns the shared material transaction, laser controls, verification and installation.

**Goal:** render the formed particle receiver as a continuous curved powder-white/pink sphere and automatically build all eight laser/particle devices plus the existing simulation chamber.

**Architecture:** preserve all construction templates, registry IDs and inventories. One server-side construction helper prechecks bounded space and materials, consumes player inventory first and the controller's connected AE storage second, places missing parts with normal protection events, and restores placements/materials on failure. The client replaces formed receiver voxel shell rendering with a continuous spherical mesh and three curved ribs. Chamber size selection spans external sizes 3..7.

**Tech stack:** Minecraft 1.21.1, NeoForge 21.1.252, Java 21, AE2 19.2.17.

**Spec:** the current user request and clarification in this chat; include the original simulation controller, prioritize backpack materials, preserve already placed modules and interfaces.

## Constraints and review focus

- No changes to production costs, receiver geometry validation, explosion policy or existing resource state.
- No replacement of obstacles, fluids, block entities or other controllers' reserved cells; no forced chunk loading.
- Missing materials or cancelled placement must preserve resources; AE short extraction must refund, including a full player inventory.
- Server menus validate distance, current host and current player menu; repeated clicks do not consume extra materials.
- Continuous receiver skin must disappear when broken/unloaded, without hiding neighboring structures or persisting hidden blocks.
- Native visual inspection and existing gameplay regression tests remain required.

## Shared interface

`dev.overloadsim.construction.AutomaticConstruction`

- `record Plan(Map<BlockPos, BlockState> cells, BooleanSupplier available)` — all required world cells; AIR means reserved empty space. Existing accepted optional parts are represented by their current states.
- `record Result(boolean successful, int placed, String reason, List<ItemStack> missing)` with `Component message()`.
- `static Result build(ServerPlayer player, BlockEntity host, Plan plan, IGrid grid)` — grid may be null; callers pass only the host's active connected grid. Host must still exist and player must be within eight blocks.

### Task 1: root — shared transaction and laser controls

Files: new `construction/AutomaticConstruction.java`, new `laser/LaserAutoConstruction.java`, `laser/LaserMenu.java`, `client/LaserScreen.java`, `gametest/AutomaticConstructionGameTests.java`.

- [x] Add RED GameTests for insufficient materials, obstacles, partial completion, backpack priority, AE supply/short extraction, protection cancellation and repeated builds.
- [x] Implement bounded plan preflight, exact debits/refunds and protected placement.
- [x] Adapt all laser templates, preserving legal optional interfaces; core is already complete as a single block.
- [x] Add an automatic-build toolbar action and result feedback to the laser menu.

### Task 2: delegated — continuous receiver skin

Files: `client/LaserRenderer.java`, `client/ClientRegistration.java`, client shell model/helper files, restricted visual lifecycle changes to `laser/LaserMachineBlockEntity.java`.

- [x] Add numeric/lifecycle regression tests before implementing smooth geometry and shell ownership.
- [x] Render a radius approximately 3.3 sphere with translucent glass and three continuous powder-white/pink structural ribs, reusing the current atlas.
- [x] Suppress only the receiver's formed shell voxels, retain the controller/optical equipment and restore normal parts immediately after invalidation/unload.
- [x] Keep large render bounds and all existing beam/particle effects.

### Task 3: delegated — simulation chamber automatic construction

Files: new `multiblock/SimulationAutoConstruction.java`, `multiblock/MultiblockSimulationMenu.java`, `client/MultiblockSimulationScreen.java`, new chamber construction GameTests.

- [x] Add regression cases for sizes 3..7 and four facings, existing upgrades/interface, obstruction and duplicate special modules.
- [x] Build around the controller's bottom front edge, preserve accepted parts, fill roof/floor with frames and sides with AE vibrant quartz glass.
- [x] Add a size selector plus automatic-build toolbar action; consume through the shared helper and check formation after success.
- [x] Reject invalid/stale menu actions and report missing resources/protected positions.

### Task 4: root — delivery

- [x] Integrate language strings, instructions and alpha.3 changelog/version.
- [x] Run unit/GameTests, asset and standalone JAR checks; obtain an independent review.
- [x] Inspect the sphere and automatic-build UI in the native game client.
- [ ] Save a Git rollback point, build the integrated checkout and install into the existing test instance with prior JAR backup and unchanged other-mod hashes.
