# Upgrade panel and continuous ME energy

Target: Minecraft 1.21.1 / NeoForge, addon alpha.3.

The right-side panel uses AE2's native four independent upgrade slots, with its available-upgrades tooltip. Each slot holds one speed card. Register the machine with AE2's upgrade API and expose `IUpgradeableObject`. Normal held-card right-click fills available slots; an unsuccessful insertion keeps the held cards. A running batch retains its original speed snapshot.

Keep main inventory indices 0, 1, 3 and 4–12 stable. Slot 2 is reserved for migration from the old stacked-card format. Persist upgrades separately, migrate the old stack once, and include cards in machine drops. Automation gains four appended upgrade slots.

Every server tick, draw from an active ME grid to refill the FE buffer, including while idle, running, or output-blocked. Default maximum transfer is 10,000 FE/tick, configurable from 0 to 2,000,000. Use AE2's standard power conversion and retain fractional conversion credit. Stop at the buffer capacity or when the grid cannot supply power. Production continues to pay FE and one HV per operation.

Verification: regressions for independent slots, normal held-card interaction, creative insertion, legacy migration and persistence, continuous charging without a crystal, charging while running, full/offline behavior, finite AE debit, and existing production/refund/output checks. Build and run GameTests, review changes, then install the verified jar into the existing PCL instance and save a Git version.
