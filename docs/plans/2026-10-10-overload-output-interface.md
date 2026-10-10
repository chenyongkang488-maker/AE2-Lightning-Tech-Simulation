# 0.2.1 overload output interface — approved implementation plan

User-approved plan, 2026-10-10. Public name 闪电科技：模拟 / Lightning Tech: Simulation. New block ID `overload_sim:overload_output_interface`, version `0.2.1-alpha.1`. Baseline `241ca88`.

## Binding requirements

Independent AE networked entity (never inherit original interface input paths). 36 virtual configuration slots plus 36 ME proxy storage slots, two pages of 18 pairs. Original NORMAL/WIRELESS mutually exclusive modes, OFF/AUTO export, NORMAL/FAST IO, energy output direction, unlimited quantities, crafting/fuzzy/induction upgrades (four slots; no speed/redstone cards). No import/filter slot. External insertion always rejected; export refusals safely refund or persist privately for refund. Exact resource export, all registered AE resource types except AppFlux FE handled by induction card.

Source slot → multiple target dimension/pos/face connections; a target face may receive several source slots, exact duplicate ignored. 1024 total connections. Round-robin targets share each source slot's cycle quantity budget. GUI single remove/clear slot. Empty slots may bind. Template replacement follows slot, clear pauses resources but retains FE. FE targets deduplicated by dim/pos/face, induction enabled only, every bound target eligible even with empty resource slot. Normal neighbor outputs use original directions; cross-dimensional wireless enabled default, same-dimension range128 (0 unlimited), no forced chunk loads. Unloaded targets preserved; loaded invalid target removed. Server validates tool, slot, interaction distances, connection limits and config before mutation. Original connector behavior unaffected for original hosts.

Tool: right-click source, Shift+wheel cycles all36 slots; right-click destination face adds binding; Ctrl batch; repeat only reports existing; right-click air clears. Source selection and notification server-owned. GUI target list includes slot/resource/dimension/position/face and deletion.

Visual: original pink-white interface plus outward arrow; two pages, upgrades, no reclaim filter, target management view. Guide only gameplay numbers/API/config. Recipe lightning assembly: 4 ae2:interface,256 ae2:fluix_pearl,2 ae2lt:overload_machine_frame,1 ae2:export_bus,1 ae2lt:overload_singularity →1,1MFE,16EHV. Preserve historical names/IDs/hashes.

## Task ownership and shared contract

All work happens at `D:/MinecraftDev/OverloadSimulation/.worktrees/overload-output-interface`. Agents must not commit/stage others' files or run simultaneous Gradle commands; coordinator owns builds/commits. Meaningful tests first, capture RED evidence, then implement. No remote publish. Upstream 1.21.1 source at `D:/MinecraftDev/reference/AE2-Lightning-Tech-Reborn-main`; declared2.1.0 snapshot drifted, actual libs2.1.0/2.1.1 are binary authorities. Local `mod/lightning-tech` is1.19.2 port, do not use.

### 1. Core entity/logic/API — core agent

Own `dev/overloadsim/output/OutputInterfaceBlockEntity.java`, `OutputInterfaceLogic.java`, `OutputConnection.java`, routing helpers, output block class, and public output event/API; related core JUnit and GameTests. Core must expose a contract immediately to UI/tool agents. Use public frequency binding API and original logic as LGPL-attributed reference. Support proxy GenericStack slots, upgrades, safe source extraction/target insertion/refund, optional FE distributor. No registry/config edits (coordinator).

Fixed public shape: `OutputInterfaceBlockEntity.SLOT_COUNT=36`, `MAX_CONNECTIONS=1024`; `OutputConnection(int sourceSlot, ResourceKey<Level> dimension, BlockPos pos, Direction face)` record. `getConnections()` immutable list; `addConnection(OutputConnection)` boolean (false duplicates/invalid); `removeConnection(OutputConnection)` boolean; `clearConnections(int sourceSlot)` int; `getConfiguredKey(int)` nullable AEKey; `getFrequencyId()`; enums `InterfaceMode NORMAL/WIRELESS`, `IOSpeedMode NORMAL/FAST`, `ExportMode OFF/AUTO`. Expose setters/getters, `isUnlimited(int)`/`setUnlimited(int,boolean)`, `getEnergyOutputDirection()`/setter, last viewed page getter/setter. Prefer `getOutputLogic().getConfig()`, `.getStorage()`, `.getUpgrades()` following AE GenericStackInv/upgrade APIs; announce exact types before UI codes. Constructor uses `OutputContent.ENTITY.get()`. Menu opened by `OutputMenu.TYPE`, registered under `OutputContent.MENU`.

### 2. Menu/screen — UI agent

Own `OutputMenu.java`, `OutputScreen.java`, UI helper classes, screen style JSON/textures and client-registration hook method inside own output class (coordinator wires existing ClientRegistration). Two paged slots, original upgrade cards and amount/unlimited interactions, network proxy read/extract only, refuse arbitrary player insertion. AEBaseMenu/client actions and frequency menu host. Target management paginated per-slot list with server-authoritative removal. Expose `OutputMenu.TYPE` built unregistered so coordinator registers it. Agent tests UI contract/resource existence after core is available. Coordinate core method signatures directly.

### 3. Connector — tooling agent

Own output tool packets/client handler and connector mixins under its namespace; no shared mixin JSON/client global files (coordinator wires). Source CUSTOM_DATA addon entry preserves existing tool data. Intercept new source/selected output-source paths only; original host selection clears addon state and proceeds original. Server selection, cycle, add, CTRL batch, clear and actionbar slot/key feedback; no duplicate connection toggle removal. Mouse hook added method for coordinator to call from existing pre-vanilla scroll path; 36slots incl empty, reset fractional accumulator when item/player changes. Capture relevant RED tests then implement. Exact OutputConnection shared contract above. Communicate needed mixin names and hooks.

### 4. Integration/resources/config/docs — coordinator

OutputContent block/entity/item/menu registration and capabilities, Creative tab/client screen registration, range/crossdim native config, wiring mixins and mouse, recipe/model/loot/tags/Guide/localization, license notices, version/changelog/API docs. Supply dependencies through ignored libs junction (no tracked binary). Check pure tests, old/new/runtime optional combinations and resource checks. Final fresh independent branch review, resolve important findings with regression evidence. Window-mode client QA of pages, selection, targeting and visuals; backup/install only after required checks, preserving other files by hashes; commit/tag/install receipt, integrate ff-only into primary source checkout.

## Evidence/acceptance

Core tests: output-only item/fluid caps; no target extraction; exact budgets across multiple targets; callback refusal/refund persistence/readback; slot switch/clear/empty; duplicate/max/roundrobin/range/crossdim/unload; FE induction gating and target dedup. Tool tests: source selection,36slot cycles, batch/dedup, clear/old behavior, invalid packet rejection. UI paging/storage extraction versus rejected insertion, target actions authoritative. Legacy source compatibility preserved. Run JUnit +497 existing GameTests +new tests on original and latest AE2LT and optional flux/no flux. No visual claims without captured evidence.

## Preflight

Core output logic creates inventories consumed by UI; method types must be communicated before UI implementation. Core OutputConnection consumed by packets and menu actions; record fields fixed above. Coordinator OutputContent ENTITY/MENU consumed by core/menu constructor; TYPE available even before registration. All tasks share working tree but disjoint owned files. Coordinator compiles only at stable milestones to prevent transient partial-file failures.
