# Machine configuration from the title screen

Implement alpha.17 for Lightning Tech: Simulation. Keep `overload_sim`, existing COMMON file keys, namespaces and saved inventories. Register NeoForge's native configuration screen; no new GUI dependency.

1. Add grouped COMMON settings in `overload_sim-machines.toml` with units, bounded values and world-restart metadata. Existing settings remain in their original files. Defaults preserve alpha.16 balance.
2. Add tests with nondefault machine parameters before wiring production behavior. Run the concentrated GameTests to observe the expected failures.
3. Wire ordinary simulation, multiblock simulation, coil and the addon Tesla recipe; freeze already-started recipe costs. Wire workstation limits and export budgets, laser ranges/limits/boost/burst, core threshold, receiver storage and spherical explosion radius.
4. Preserve stored items, fluids and energy above a newly lowered operating limit, reject new additions without negative transfers, and retain extraction and rollback. Keep physical geometry and protocol safety limits fixed. Sync server-owned values used by client menus.
5. Add native config registration and Chinese/English labels, tooltips and instructions. Invalid values use native validation; settings marked world-restart apply upon reentering a world. Multiplayer settings are owned by the server.
6. Run unit tests and GameTests against latest dependencies with and without optional integrations and the older supported dependency profile. Review changes, build alpha.17, back up and replace only this addon's test-instance JAR, and record verification hashes.

Ownership: root owns schema, laser/core buffers and release integration; config_inventory owns simulation/multiblock/coil/Tesla; config_workstation owns workstation; config_ui owns native factory, language resources and user documentation. Agents do not run Gradle or make commits. The user performs the final visual check in Minecraft.
