---
navigation:
  title: High Energy Workstation
  icon: overload_sim:high_energy_workstation
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_workstation
  - overload_sim:laser_frame
  - overload_sim:laser_glass
  - overload_sim:laser_optical_port
---

# High Energy Workstation

The workstation processes the current **overload processing factory** recipes. Pack changes and recipe removals take effect at runtime. Laser power replaces their FE and lightning costs; the machine consumes only recipe materials, including fluids.

Its built-in dedicated recipe converts **256 Overload Crystals into 4 Transcendent Crystals**. These pale-blue crystals produce EHV in a Tesla Coil with a Lightning Collapse Matrix, or serve as constrainer fuel. See [Transcendent Crystal](transcendent-crystal.md).

Build a **7×7×7** enclosure using one workstation controller, one laser optical port, 141 laser frames and 75 laser glass blocks. Place the controller at the front bottom centre, facing outward. The optical port sits at the centre of the opposite wall, three blocks above the controller. The roof, floor and back wall are solid; the three other walls have 5×5 glass windows. Keep the 5³ interior empty. Formation merges the shell into light-grey pixel panels, narrow dark rims, horizontal ventilation grilles and continuous glass windows. A small yellow wrench marks the controller; four overload crystal clusters sit against the inner receiving wall and emit four translucent pink beams toward the centre of the lower work surface while powered. The cavity prevents natural spawning for every mob category, including bats.

The optical port must face a beam with **individual strength strictly above 1,000,000,000 FE**. Separate beams do not add together. With materials and output space, a batch completes in two valid ticks, at up to 1024 parallel operations. Losing the beam or breaking the structure cancels unfinished progress without consuming materials.

The controller contains nine input slots and nine output slots, each with a capacity of 1,000,000 items. Hover a slot to see the exact count. Left-click moves a normal stack, right-click moves one item, and Shift-click transfers to or from the player inventory. Automation inserts into inputs and extracts only outputs.

The vertical fluid tanks beside the item grids show the actual fluid texture and fill level, with tick marks. Each holds 1,024,000,000 mB and retains fluid components; hover to see the exact amount. Use a filled fluid container on the input tank to fill it, or an empty container on either tank to drain it. The output tank accepts no incoming fluid.

Any controller face accepts materials and exposes products to pipes. Input materials cannot be extracted, and output buffers cannot be filled. Two independent toolbar switches enable automatic ejection to adjacent containers and export to the connected AE network; both start off. Other shell blocks provide menu access, without item or fluid ports. Right-click an owned frame after formation to open the controller screen. Focusing is optional; the individual beam must still exceed 1GFE.

Automatic build uses the backpack first, then the connected AE network; creative players need no materials. The projection beneath it previews missing parts without changing the world. Creative administrators can use `/overload_sim workstation demo` in empty loaded space for a finite, precharged laser chain and workstation.

The controller is the only workstation-specific construction item. Existing dedicated workstation parts convert to their matching laser parts when a complete old structure forms. Breaking a loose old part drops the corresponding laser item; an old part in your inventory can be crafted into one matching laser part without extra materials. Migration preserves the controller's stored items and fluids.

Authors can add workstation-only recipes with type `overload_sim:high_energy_workstation`, using the overload processing factory recipe format. The Java recipe-source API is documented in the repository's `docs/workstation-api.md`. Custom sources must return final executable recipes and preserve ingredient predicates and components.
