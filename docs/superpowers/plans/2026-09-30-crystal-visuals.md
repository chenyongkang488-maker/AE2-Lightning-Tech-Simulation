# Crystal visuals implementation plan

Goal: simplify crystal items, establish the perfect pink crystal as the addon logo, and show a floating crystal through an open chamber frame.

Target remains MC 1.21.1 / NeoForge 21.1.252, with the existing pinned AE2 dependencies. Existing recipes, power use, inventory indices and speed behavior remain compatible.

1. Use the installed electro chime crystal silhouette as the reference for 16×16 sprites. Ordinary crystals use a grayscale palette. The perfect crystal uses a pink-dominant palette with restrained cyan, lavender and gold facets. Keep the solid body and a separately editable, animated lightning layer. Save the layered textures in Blockbench, export Minecraft animation metadata, and use a static first-frame composite for the mod-list logo.
2. Build an original hollow frame in Blockbench: pale base, four narrow corner posts, open side windows, a pink upper rim, four opposing field emitters, and a crystal-shaped badge. Export its Java block model and editable bbmodel. Match collision/selection geometry to the shell, and disable full-block occlusion so adjacent faces remain visible.
3. Add a client-only block entity renderer: when slot 0 contains a perfect crystal, show its body slowly rotating and floating; render bounded lightning between the field emitters and the crystal, softly idle and stronger while simulating. Sync only crystal presence and operation state to clients, including chunk entry and input removal, without broadcasting remaining ticks every tick.
4. Verify failing/passing game regressions for crystal render-state updates and hollow block shapes. Validate PNG frame sizes, transparent effect layers, model texture references and Blockbench sources. Build, run the server suite, launch a client in a dedicated visual-test world and inspect the actual render. Review, install the jar in the existing PCL instance, preserve the previous jar and save a Git tag.

Artwork derived from the upstream electro chime sprite retains upstream attribution/license. The new chamber geometry and lightning overlay are editable addon sources. No external image-generation service is involved.

User revision: add glass to all six faces. Use clear centers, restrained pale pink edges and sparse reflection pixels on an original cutout glass texture. Add four thin side panes, one roof pane and a protective glass floor above the existing metal base. Match selection/collision to these six panes while keeping face occlusion disabled. Update the previous open-window regression to verify glass protection instead.
