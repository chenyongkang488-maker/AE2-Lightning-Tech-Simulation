---
navigation:
  title: High Energy Workstation
  icon: overload_sim:high_energy_workstation
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_workstation
  - overload_sim:high_energy_workstation_frame
  - overload_sim:high_energy_workstation_glass
  - overload_sim:high_energy_workstation_receiver
---

# High Energy Workstation

The workstation processes the current **overload processing factory** recipes. Pack changes and recipe removals take effect at runtime. Laser power replaces their FE and lightning costs; the machine consumes only recipe materials, including fluids.

Build a **7×7×7** enclosure using one controller, one receiver, 141 workstation frames and 75 workstation glass blocks. Place the controller at the front bottom centre, facing outward. The receiver sits at the centre of the opposite wall, three blocks above the controller. The roof, floor and back wall are solid; the three other walls have 5×5 glass windows. Keep the 5³ interior empty. The formed machine displays four pointed crystals and a nine-position work pedestal.

The receiver must face a beam with **individual strength strictly above 1,000,000,000 FE**. Separate beams do not add together. With materials and output space, a batch completes in two valid ticks, at up to 1024 parallel operations. Losing the beam or breaking the structure cancels unfinished progress without consuming materials.

The controller contains nine input slots and nine output slots, each with a capacity of 1,000,000 items. Hover a slot to see the exact count. Left-click moves a normal stack, right-click moves one item, and Shift-click transfers to or from the player inventory. Automation inserts into inputs and extracts only outputs. The two fluid buffers each hold 1,024,000,000 mB and retain fluid components.

Use the output configuration button to set each controller face to closed, input, output or both. All faces start closed. Adjacent ejection and export to the connected AE network are separate switches and also start off. Other shell blocks provide menu access, without item or fluid ports. Right-click an owned frame after formation to open the controller screen.

Automatic build uses the backpack first, then the connected AE network; creative players need no materials. The projection beneath it previews missing parts without changing the world. Creative administrators can use `/overload_sim workstation demo` in empty loaded space for a finite, precharged laser chain and workstation.

Authors can add workstation-only recipes with type `overload_sim:high_energy_workstation`, using the overload processing factory recipe format. The Java recipe-source API is documented in the repository's `docs/workstation-api.md`. Custom sources must return final executable recipes and preserve ingredient predicates and components.
