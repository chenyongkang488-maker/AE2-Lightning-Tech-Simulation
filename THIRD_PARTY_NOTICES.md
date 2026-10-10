# Artwork attribution

The 16×16 crystal silhouette in the blank, bound and perfect simulation crystal
textures is adapted from `electro_chime_crystal_stage_0.png` in **AE2 Lightning
Tech Reborn 2.1.0**, by its contributors. The original sprite is embedded in
`art/overload_sim_visuals.js` to retain the editable source. Changes: grayscale
ordinary variants and a pink, cyan, lavender and gold palette for the perfect
variant.

Upstream source: https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn

These derived crystal textures, the corresponding logo and their embedded
copies in the Blockbench projects retain the **CC BY-NC-SA 3.0** asset license.
See `art/licenses/AE2LT-LICENSE_ASSETS.md` and
https://creativecommons.org/licenses/by-nc-sa/3.0/ . The upstream source-code
LGPL license is separate from its asset license.

The Transcendent Crystal (`overload_sim:transcendent_crystal`) is adapted from
AE2 Lightning Tech Reborn 2.1.0's `assets/ae2lt/textures/item/overload_crystal.png`,
its item model and animation metadata, by the same upstream contributors.
Changes: a pale-blue palette, retaining the original pixel silhouette, alpha,
frame order and timing. These derived assets retain **CC BY-NC-SA 3.0** under
the attribution and asset-license links above. The reproducible recolouring
script is addon code under MIT; the reference JAR is not bundled.

The chamber frame geometry, metal atlas and separately drawn lightning overlay
are original addon work under this project's MIT license. The addon Java code
remains MIT except the adapted portions identified below. Reference mod binaries are neither committed nor bundled.

The seven coil module icons are adapted from AE2LT 2.1.0's
`assets/ae2lt/textures/item/overload_module_base.png`. The original beveled case
and pink contacts are retained; the inset panel and seven function symbols are
new. These icons, `art/reference/overload_module_base.png`, their preview sheet
and the embedded base sprite in `art/resonance_coil_workshop.js` retain
**CC BY-NC-SA 3.0** under the same attribution above. The coil geometry, body
atlas and eight-frame converging arcs are original addon artwork (MIT).

The matrix outer collars, pixel rune cartridges, blue-white collapse orb and
procedurally synthesized `collapse_transition.ogg` are original qiqi/addon work
under MIT. Reference images inform the design; no image pixels or recorded audio
were extracted from them. The orb reuses the addon's original voxel sphere geometry.

The alpha.9 six-faceted deflector crystal geometry and native-painted atlas
tiles 52–53, and the independent 64-frame `laser_energy_flow` texture are
original qiqi/addon work under MIT. The crystal reference is used for shape
and palette only; no reference pixels were copied. Existing derived simulation
crystal and coil-module artwork continues to retain its separate asset license.

## Overload Output Interface (0.2.1)

The independent output interface references AE2 Lightning Tech/Reborn's
OverloadedInterfaceLogic/BlockEntity/Menu/Screen and Wireless Overload Connector
implementation. Adapted Java portions preserve LGPL-3.0-only attribution
in their source headers and screen layout comment; the license texts are included at
LICENSES/LGPL-3.0.txt and LICENSES/GPL-3.0.txt, and under META-INF/licenses in both JARs.
Original routing, refund, API and test additions
are credited to qiqi / Lightning Tech: Simulation contributors.

The Blockbench project art/overload_output_interface.bbmodel embeds a pink,
pearl-white and ice-blue adaptation of assets/ae2lt/textures/block/overloaded_interface.png.
The original editing reference remains in art/output-interface/. The recolored
texture is bundled as assets/overload_sim/textures/block/overload_output_interface.png,
retaining CC BY-NC-SA 3.0 with attribution to AE2 Lightning Tech contributors;
qiqi made the color adaptation with the built-in image editing tool. Arrow
geometry from alpha.1 was removed in alpha.2. Overlay rendering geometry is
adapted from WirelessConnectorRenderer and retains LGPL-3.0-only attribution
in OutputConnectorRenderer.java; source-slot coloring and routing are addon
changes. No upstream binary is redistributed.
