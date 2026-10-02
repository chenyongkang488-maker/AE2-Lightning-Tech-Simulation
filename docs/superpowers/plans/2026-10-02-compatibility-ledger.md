# SDD ledger — plan: docs/superpowers/plans/2026-10-02-simulation-compatibility.md

User approved the saved spec and in-chat design with “yes”. Inline execution continues in the existing independent D-drive checkout with no concurrent implementers. Base d85cc7b (alpha.10 plus approved design); new branch codex/simulation-compatibility. Existing session preference for work in this checkout and Blockbench fixed ROOT is retained; no redundant worktree consent. No AGENTS.md found in D:/, D:/MinecraftDev or project root.

Pre-flight interfaces: Task1 busy/paid/visual flags consumed by Task4; bit8 processing separates presence and activity, paid reserved for settlement. Task2/3 both feed shared output path via SimulationResolvers; preserve legacy OutputProvider and existing format1. Task3 generated profiles consumed by collector/cultivation/single/multi and diagnostics; refresh at tags+rules+recipes, retain stable IDs. Task4 light modifies interior validator; only correctly owned expected center accepted. Task5 diagnostics reads the same immutable resolution and never rolls outcomes.

Tasks 1–5 pending. Whole-branch review only at the end. Tests and native screenshots are gates before PCL installation.

Baseline: test runGameTestServer passed 19 JUnit / 81 GameTests, compatibility-baseline.log exit0. Bundled Git has no Bash; plan workspace/briefs/ledger operations use native PowerShell equivalents, preserving the same paths and task bases.

Task 1: complete (base 1a55d49; tests: test runGameTestServer -> 19 JUnit / 90 GameTests, compatibility-task1-green.log exit0). Initial RED reproduced six interaction/payment failures; callback RED reproduced both commit mutation/veto failures. FE/HV/EHV completion payment, revision guard, abort/refund journal and insert-only external templates implemented. Shift fixture corrected using PLAYER_HOTBAR semantics; legacy prepayment assertion updated to approved completion contract.

