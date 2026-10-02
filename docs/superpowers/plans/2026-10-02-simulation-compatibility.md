# Simulation Compatibility Implementation Plan

> Execute inline with superpowers:executing-plans. The user approved the written design with “yes”; continue the established D-drive checkout, Blockbench authoring and PCL backup/install workflow. One fresh whole-branch reviewer at the end.

**Goal:** Deliver alpha.11 implementing the seven approved compatibility, interaction and visual corrections.

**Architecture:** Keep stable crystal IDs and external slots. Controller settlement becomes an input-revision guarded completion transaction. Data-driven mob/mineral resolvers feed the existing shared output path. Visual presence is independent of batch progress, with an owned invisible server light and directional corner atlas.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.252, AE2 19.2.17, AE2LT 2.1.0, Java 21, Blockbench desktop.

**Spec:** ../specs/2026-10-02-simulation-compatibility-design.md

## Global Constraints

- N=3..7, N² input capacity, 49 stable input slots, 128 bulk outputs / 1024 per slot, base180tick /1000FE+1HV per participant; keep upgrades and current fees.
- Change crystals at any time in GUI/Shift click; actual mutation aborts the whole job, simulated/failed mutation does not. New jobs charge only at completion. Preserve legacy paid jobs and refund on cancellation.
- External slot0..48 insert-only; output49..176 extract-only. Keep slot numbering and native AE2LT interfaces.
- Ball present whenever formed and an active perfect crystal exists, light radius approximately5 unobstructed (initial emission6); pink/orange and overload based on installed modules.
- Dragon independent egg25%×1/head25%×1/breath100%×1; wither star×1; warden catalyst/echo/sculk each×1; player-kill context with looting0; magma size2/slime size1; no live entity kill or equipment copying.
- Netherite profile -> debris×1, smelt ->scrap×2. Redstone4..5/lapis4..9 base yield; tagged mod raw blocks auto-bind; explicit rules override automatic discovery. Preserve CrystalData format1/API compatibility.
- All raster assets authored in Blockbench; other mods and 46 recorded PCL settings must remain unchanged. Local commits/tag, no push.

## Review Focus

- Input callbacks/commit events/network extraction reentrancy: no stale output or double debit, including failed extraction refunds and full FE credit.
- Reload/server restart/mod removal: automatic profile identity and fixed jobs remain stable; absent targets retain crystals and stop safely.
- Loot context/GLM/preset isolation: exactly one modifier pass, independent chances and neutral equipment, no state leaking across machines.
- Ambiguous multi-mod material tags and heterogeneous ores: fail with diagnostics instead of silent arbitrary output or consumed mixed materials.
- Light cleanup/cache corner orientation across unloaded chunks and replaced controllers: never delete user replacement blocks or force-load chunks.

## Tasks

### Task 1 — Completion settlement, inventory mutation and automation

Files: SimulationControllerBlockEntity, SimulationBatch, SimulationBatchPlanner, MultiblockSimulationMenu, MultiblockSimulationEvents, gametest/MultiblockGameTests.

Interfaces: `busy()` means a planned/in-progress batch; `paid` only means legacy/commit journal payment. `inputRevision()` guards callbacks; `abortBatch(String)` discards outcomes/refunds legacy paid expenses; persisted `feCredit` offsets future fees. `BeforeBatchCommit(controller,batch)` cancellable; `BatchAborted(controller,batch,reason)` notification.

- [x] RED real GameTests: automationCannotExtractTemplates; workingCrystalEditAbortsWithoutPayment; crystalQuickMoveDuringWork; simulatedAndFailedEditsKeepProgress; completionDebitsFeesExactlyOnce; legacyPaidEditRefundsIncludingFullFEBuffer; beforeCommitInputChangePreventsStaleOutput.
- [x] Run `test runGameTestServer --console=plain`, capture expected behavioral failures before production edits.
- [x] Implement actual-change hooks/load guard, unrestricted GUI recovery and completion charge transaction with revision/capacity checks and partial-payment refunds; new persisted SettlementVersion=2, legacy absent=1. Preserve start/completion events and add commit/abort events.
- [x] Adapt old prepayment-only assertions to the new approved completion contract; keep manually saved paid migration tests. Validate paused/reloaded processing, sparse fairness, full output and channel behavior.
- [x] GREEN full JUnit/GameTest suite, ledger and checkpoint commit.

### Task 2 — Mob rules, context, presets and egg eligibility

Files: new data/MobSimulationData, machine/SimulationEntityLoot, binding/SimulationEntityEligibility; MobLoot, SimulationExtensions, PlayerLightningHandler, OverloadSimulation; simulation_mob JSON and binding deny rules; new gametest/CompatibilityGameTests.

Interfaces: `MobSimulationData.resolve(ResourceLocation entity)` returns immutable rule or ambiguity/disabled diagnostic; `SimulationEntityLoot.roll(ServerLevel,BlockPos,CrystalData,Rule)` returns new legal stacks. Named `EntityTemplateInitializer` accepts unspawned LivingEntity. Eligibility preserves old Mob predicates and adds LivingEntity adapters. Existing OutputProvider remains; new OutputProviderV2 gets explicit RandomSource and machine context.

- [x] RED GameTests: phantom/blaze/breeze produce player-only loot across fixed trials; magma cream exists at size2 while slime balls remain; dragon independently permits egg+head+breath and empty rare pools; wither/warden overrides; egg-enabled non-Mob eligibility and explicit disabled precedence; GLM result not duplicated.
- [x] Run suite, inspect named missing/drop failures.
- [x] Implement reloadable Codec rules with outputs min/max/chance, entity/tag/* match, priority/conflicts and contexts; player fake attacker has no looting and is never spawned. Add preset adapters and stable egg index. Remove builtin wither/dragon denials via explicit enabled rules.
- [x] GREEN full suite and JSON rule override tests, ledger and checkpoint.

### Task 3 — Tagged minerals and shared output resolver

Files: new data/MineralSimulationData, data/SimulationResolvers, api/ResolvedMineral, machine/SimulationMineralLoot; CrystalBinding, SimulationData, MobLoot, SimulationBatchPlanner, SimulationChamberBlockEntity, SimulationCrystalItem, SimulationExtensions, MultiblockData; builtin binding/profile/production resources; CompatibilityGameTests.

Interfaces: `SimulationResolvers.production(ServerLevel,CrystalData)` returns immutable resolved production supporting existing recipe, explicit mineral, auto-tag fallback; `SimulationData.profile(id)` includes stable generated descriptors; explicit recipes still win. `registerMineralResolver(id, resolver)` provides Optional<ResolvedMineral> without mutating world. Automatic ID `overload_sim:auto/mineral/<tagnamespace>/<material>`.

- [x] RED: redstone/lapis binding and base loot ranges; netherite debris and scrap2; fixture third-party raw storage tags -> raw item1; ambiguous material/output rejects without destruction; gem block loot; active reload/profile stability, both machines, smelt and fortune only once.
- [x] Run suite and observe failures before implementation.
- [x] Implement server tag/recipe/rule reload cache; scan block and item tag namespaces separately. Explicit mineral JSON supports binding selector, material, raw output or block_loot, preferred ore/output, quantity and smelting. Conservative multi-source ambiguity; no arbitrary registry-order selection. Keep all old profiles and crystal format1.
- [x] Connect shared resolve/roll to both chambers, collector binding and cultivation; synchronize generated display descriptor as needed without changing item identity. Add builtin coal/emerald/quartz/redstone/lapis and netherite correction.
- [x] GREEN full suite with fixture third-party resources and max buffer bounds, ledger and checkpoint.

### Task 4 — Presence, owned light and Blockbench corners

Files: SimulationOrbState/renderer/controller visual streams, new SimulationLightBlock/Entity, MultiblockContent/validator/index, corner topology/model/authoringJS/PNG/bbmodels, resource verifier; core tests and MultiblockGameTests.

Interfaces: flags bit1=ball visible, bit2=overload, bit4=smelt, bit8=processing; flags mask15. Light owner UUID/controller pos persisted; only valid expected center light allowed in structure; no visible item/drop. Atlas corner geometry derives world orientation with matching pixel seam contracts.

- [x] RED: ball stays visible through completion/offline/blocked states; inactive-only crystal stays hidden; light emission6/cleanup/replacement/unloaded recovery; corner neighbor directions and tile seam checks.
- [x] Run suite, inspect failures before edits.
- [x] Implement presence independent of job, working arcs/ECG use bit8; phase uses world time. Light transition writes only on presence/structure/emission changes, no force loading; cleanup only own block. Add explicit owner check in validator.
- [x] Author L-shaped rails/corner plates in native Blockbench, export editable projects/runtime PNGs. Preserve glass/roof/controller/port seams and full collision cube.
- [x] GREEN unit/GameTests + resource parity; native all-size/all-corner, idle/working/lighting/reload screenshots, ledger and checkpoint.

### Task 5 — Diagnostics, author samples and alpha.11 delivery

Files: new command/SimulationDiagnostics, translations/guides/API docs/samples, README/CHANGELOG/verification, build/meta/install script; fixture script/resources.

Interfaces: `/overload_sim explain` describes held crystal resolution without rolling or charging; `/overload_sim audit` emits current mineral/mob mappings and unsupported/ambiguous reasons. Existing named extensions and format1 preserved.

- [x] RED narrow integration checks for explaining valid/ambiguous/missing rules without inventory/energy side effects.
- [x] Implement diagnostics and author examples (tagged metal, mob override, independent outputs, API v2); document cancellation/settlement migration/GLM and code-only drop boundaries.
- [x] Full build/unit/GameTests with and without optional Mek. Verify resource bytes/metadata/no bundled dependencies; native Shift operations, artificial lightning tagged binding, wireless output protection, ball/light/corners and new mob outputs.
- [x] One fresh read-only whole-branch reviewer per executing-plans. Critical/Important fixes one TDD pass; record decisions/minors.
- [x] Install only alpha.11 into PCL, retain alpha.10 backup; verify all other mod/settings hashes. Commit/tag v0.1.0-alpha.11, preserve local branch and report outcome.

Commands use `D:/DevTools/Java/jdk-21.0.12.1+1`, `D:/DevTools/Gradle-8.8/bin/gradle.bat`, Gradle cache `D:/DevTools/GradleCache`. Logs redirected to build/verification and each task's ignored workspace; inspect tails and JUnit XML. A passing named test never substitutes for the full suite at a task checkpoint.
