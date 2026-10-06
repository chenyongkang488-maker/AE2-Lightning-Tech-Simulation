# Matrix and subspace visual update

Implement the user's five requested visuals on the existing 1.21.1 NeoForge runtime. Preserve resource IDs, laser collision geometry and resource accounting.

* Constraint: a broad voxel collar, pink rune panels and projecting clamps outside its existing armoured ring.
* Focus: paired mirror collars with cyan/pink sight marks, visibly different from constraint, enhancement and burst.
* Both menus display current laser energy in FE per pulse, with an exact integer tooltip and zero after expiry.
* Burst has bounded cosmetic arcs from the ring into the beam while receiving energy. Beam surfaces reuse the simulation orb's animated pixel sprite.
* Core has a server-synchronized 40-tick blue-white contraction/expansion followed by a stable blue sphere and purple/orange electric halo. First accepted input and a restart after input expiry trigger it once; continuous 20-tick pulses retain the original start time. A cancelled emission triggers neither animation nor sound.
* Original native Blockbench pixel assets and editable formed-mesh projects accompany the runtime resources. An original transition sound is played at the core with bounded attenuation.

Verification: pure geometry/animation regressions, server input/menu and persistence tests, full existing suite, resource validation, then windowed Minecraft checks. Build alpha.7, preserve a rollback JAR and install only this addon in the existing PCL test instance. No remote release.
