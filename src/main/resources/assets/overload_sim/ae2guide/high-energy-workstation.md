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

The workstation processes the currently loaded Overload Processing Factory recipes, including pack and script changes. It consumes the recipe's items and fluids. Laser power replaces its FE and lightning costs.

Its dedicated recipe converts 256 Overload Crystals into 4 [Transcendent Crystals](transcendent-crystal.md). This recipe is available only in the workstation.

Manufacture the controller in a Lightning Assembly Chamber using 64 Overload Processing Factories, 16 Lightning Collapse Matrices and 64 Overload Processors, costing 256 EHV and 2,000,000 FE.

## Construction and laser input

Build a 7×7×7 enclosure with one controller, one laser optical port, 141 laser frames and 75 laser glass blocks. Using local coordinates x=right, y=up and z=inward, each from 0 to 6, place the controller at (3,0,0), facing outward, and the optical port at (3,3,6). Fill the roof, floor and back wall with frames, apart from those two positions. Each of the other three walls has a 5×5 glass area inside its frame border. Keep the 5×5×5 interior empty.

Aim a laser into the back-wall port from outside. The individual beam must be strictly stronger than 1,000,000,000 FE. Separate weak beams aimed directly at the port do not add together; merge them through a deflector first to supply a combined beam. A focus matrix is optional. With materials and output space available, two consecutive valid ticks complete a batch of up to 1024 recipe operations. Losing the beam or invalidating the structure resets unfinished progress without consuming that batch's materials.

Right-click the controller or an owned frame after formation to open the menu. Automatic build uses the player's inventory first, then the controller's connected AE network; creative players need no materials. The projection button previews the construction plan. Obstructions and unloaded positions prevent construction. Creative administrators can use `/overload_sim workstation demo` in empty loaded space to place a workstation and a laser chain supplied with finite resources.

## Storage and transport

The controller has nine input slots and nine output slots, each holding up to 1,000,000 items of one type and component set. Hover a slot for the exact count. Left-click moves a normal stack, right-click moves one item, and Shift-click transfers between the machine and player inventory.

The input and output fluid tanks each hold 1,024,000,000 mB and retain fluid components. Hover for the exact amount. Click the input tank with a filled container to fill it. An empty container can drain either tank; the output tank accepts no incoming fluid. Each transaction handles one container and needs somewhere to put the resulting container.

Every controller face accepts item and fluid inputs and exposes products. Pipes can insert into input buffers and extract from output buffers. They cannot extract input materials or fill output buffers. Other shell blocks provide menu access.

Two independent toolbar switches enable ejection to adjacent containers and export to the connected AE network. Both default to off. Automatic output has separate item and fluid limits of 2,147,483,647 items/tick and 2,147,483,647 mB/tick, shared between AE export and adjacent ejection. It transfers only the amount accepted by the receiving inventory, tank or network. Rejected products stay in the workstation; receiving capacity and insertion rules still apply.

## Data packs and Java API

Add workstation-only recipes at `data/<namespace>/recipe/<path>.json` with type `overload_sim:high_energy_workstation`. For example:

```json
{
  "type": "overload_sim:high_energy_workstation",
  "priority": 10,
  "inputs": [
    {"ingredient": {"tag": "c:ingots/iron"}, "count": 4},
    {"ingredient": {"item": "minecraft:redstone"}, "count": 1}
  ],
  "results": [{"id": "minecraft:gold_ingot", "count": 2}],
  "totalEnergy": 5,
  "lightningCost": 1,
  "lightningTier": "high_voltage"
}
```

This format permits up to nine counted inputs and one item result. Optional `inputFluid` and `resultFluid` use NeoForge FluidStack data. At least one input and one output are required. `totalEnergy` must be at least 5 and `lightningCost` positive, although the workstation does not charge either cost. Ingredients retain tag, alternative-item and component matching. A recipe with only fluid inputs requires all item input slots to be empty. Only declared recipe results are produced.

Selection first favors the largest executable parallel count, then higher `priority`, more input kinds and greater total item demand, then recipe ID. Override `data/overload_sim/recipe/transcendent_crystal.json` to change the dedicated crystal recipe. Reload data packs with `/reload`.

Java integrations can call `WorkstationRecipes.registerSource(id, source)` during common setup; the source returns current executable `Entry(id, recipe)` objects from the final RecipeManager. Preserve their ingredient predicates and components. Call `WorkstationRecipes.invalidate()` after changing a custom source. Buffer counts are available through `WorkstationBuffer.count(slot)`; normal extraction returns legal item stacks. The full reference is `docs/workstation-api.md`.
