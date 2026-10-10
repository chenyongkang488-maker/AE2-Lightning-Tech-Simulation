---
navigation:
  title: Overload Output Interface
  icon: overload_sim:overload_output_interface
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:overload_output_interface
---

# Overload Output Interface

While holding the connector, same-dimension links and target faces use a fixed color for each of the 36 source slots. Selecting a source shows that source's links and a preview on the aimed target face. Multiple slots sharing a face have separate endpoints. Empty slots keep their link overlays; cross-dimension links are not drawn in the current dimension.

Configure 36 virtual resource samples on two pages. No sample is consumed. The paired storage slots show resources in the connected ME grid. This device sends resources out and rejects deposits and collection from targets.

NORMAL mode supplies adjacent inventories. WIRELESS mode supplies the bound face of each target. The modes are mutually exclusive. Supply continues until the grid lacks stock or a target refuses insertion; configured amounts limit each source slot's total transfer per cycle, not target inventory levels. Unlimited mode removes the slot quantity limit. Targets rotate fairly.

In WIRELESS mode, right-click the interface with the Wireless Overload Connector. Hold Shift and scroll to select slots 1–36, then right-click the target's input face. Clicking that face again removes only the selected source slot's link. Ctrl-click adds missing links to matching adjacent targets without removing existing links. The Targets view can remove one link or clear a source slot. Right-click air to clear the tool selection.

Right-click the interface with a supported crafting, fuzzy or induction card to insert it directly. AE2's native Shift-right-click upgrade insertion is also supported.

Changing a sample changes the resource supplied by that slot's existing connections. Clearing a sample pauses its resource output. Empty slots can bind targets. Install an Applied Flux induction card to send FE to all connected target faces, including empty resource slots. Each FE target face is counted once. Without Applied Flux or the card, FE supply is disabled.

Deleting links requires a held connector. Keep it in the offhand, open the interface with an empty main hand, and use the Targets view to remove or clear links.

Up to 1024 source-slot connections are stored. Same-dimension range defaults to 128 blocks; other dimensions are allowed. Unloaded targets pause without forced chunk loading. Successful output can run every tick; idle polling backs off to 80 ticks in NORMAL or 20 ticks in FAST. FAST multiplies idle network power by 1.5. Crafting, fuzzy and induction cards are supported; speed cards are not. Directed output always matches the exact resource.

Manufacture in a Lightning Assembly Chamber: 4 ME Interfaces, 256 Fluix Pearls, 2 Overload Machine Frames, 1 Export Bus and 1 Overload Singularity; 1,000,000 FE and 16 EHV, yielding one interface.

Edit `overload_sim-output.toml` in the native mod configuration screen: `wirelessRange` (0 = unlimited) and `allowCrossDimension`. Server settings govern multiplayer. Java extensions and transfer events are documented in `docs/output-interface-api.md` in the source repository.
