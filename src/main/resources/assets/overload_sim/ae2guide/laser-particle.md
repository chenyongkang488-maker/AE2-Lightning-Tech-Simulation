---
navigation:
  title: Laser and particles
  icon: overload_sim:high_energy_laser_emitter
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_laser_emitter
  - overload_sim:laser_constraint_matrix
  - overload_sim:laser_enhancement_matrix
  - overload_sim:laser_burst_matrix
  - overload_sim:laser_deflector_matrix
  - overload_sim:laser_focus_matrix
  - overload_sim:collapse_core
  - overload_sim:particle_constrainer
  - overload_sim:laser_frame
  - overload_sim:laser_glass
  - overload_sim:laser_source_crystal
  - overload_sim:laser_optical_port
---

# Laser and particle prototype

Build the chain in this order: **emitter → matrices → focus matrix → collapse core → particle constrainer**. Particles leave the opposite side of the core, continuing in the laser's forward direction. Put the constrainer after the core, with its input facing the core and within seven blocks of that side.

The controller screen faces upstream; the beam travels in the opposite direction. All six directions work. The emitter uses a 5³ shell with a source crystal at its center. Ordinary matrices are seven blocks across and one block thick with an empty center. The deflector uses a 7³ voxel sphere with a central optical port on each face. The constrainer uses three white/pink great-circle ribs supporting a transparent glass sphere. Leave at least one empty block between structure surfaces. The screen reports formation. Connect the controller with AE cable, or place one Overloaded ME Interface in the structure for the overload network.

### Ring front view (screen facing you)

F is laser frame, C is the matrix controller, and dots are air. The center is three blocks along the structure's up direction from its controller. For a vertical beam, that direction is north. All four ordinary rings share this template.

```
..FFF..
.F...F.
F.....F
F.....F
F.....F
.F...F.
..FCF..
```

The receiver is a voxel sphere seven blocks across. Relative to its center, x/y/z range from -3 to 3. Shell cells have squared distance x²+y²+z² from 7 through 12 inclusive. Shell cells with any coordinate equal to zero use laser frame (three intersecting great-circle ribs); remaining shell cells use glass, and every other cell is air. It uses 40 frames, 56 glass, one controller at the bottom pole and one optical port at the upstream pole. For vertical beams, the construction up direction is north. Align the core with the port. Creative administrators can build it with `/overload_sim laser build receiver`. A saved legacy cube safely pauses and retains inventory until rebuilt as this sphere. The emitter controller sits at the back center, its source crystal is two blocks forward, and its output port is another two blocks forward.

The emitter pays 100000 FE every 20 ticks and starts with a six-block range. A constraint matrix pays 100000 FE and renews six blocks of range. An enhancement matrix adds and pays the selected 100000–9000000 FE. A burst matrix pays 64 EHV to double power. A deflector splits power among enabled output faces; its input face is locked, and closing every output stops incoming work. The maximum beam power is 1P FE. Matrices without valid incoming light pay no running cost.

A focus matrix converges on the collapse core at the seventh block ahead. It clears breakable cells on the optical axis before the target, while unbreakable blocks stop the beam. The core activates at 1G FE and creates one particle per accumulated 512G FE, keeping the remainder. Ordinary pink beams are three blocks across; focus cones start at that width and contract toward the core. Entity damage follows the same width. Blue dots show particles travelling forward from the core to the receiver; the server owns the actual count.

## Particles and sealed consumables

The constrainer holds 125000 particles and 1024P FE. Inserted consumables cannot be removed through the screen or automation. Each consumable provides 100 effective working ticks. Processing pauses without particles or space for the next energy yield, preserving the paid window.

|Consumable|Interval per particle|Yield per particle|
|---|---|---|
|Nether Star|100 ticks|512G FE|
|Netherite Block|20 ticks|512G FE|
|Singularity|10 ticks|256G FE|
|Overloaded Singularity|2 ticks|1024G FE|
|Plutonium / Polonium pellet (optional Mekanism)|1 tick|2048G FE|
|Antimatter pellet (optional Mekanism)|1 tick|4096G FE|

The FE buffer debits only energy accepted by the AE network. Particle overflow, or dismantling a constrainer structure containing particles, triggers destruction in the loaded 3×3 chunk area centered on the machine. Stop upstream beams and drain particles before dismantling. Empty constrainers can be adjusted normally.

Right-click a controller for power and controls. Hover over the status area for exact long values, diagnostics and consumable timers. Press G to open this page. Creative administrators can use `/overload_sim laser build <kind>` in an empty area to place one structure, or `/overload_sim laser demo` for a complete chain precharged with finite resources. The demo uses enhancement, 16 burst matrices, range constraints, focus, the core and a forward receiver. Powder-white panels, pink channels and eight equipment glyphs are painted in Blockbench; recessed screens reuse the animated ECG and the sphere uses borderless pale glass.
