"""Export the output interface's editable Blockbench geometry and game assets.

The base texture is supplied by AE2 Lightning Tech (CC BY-NC-SA 3.0).
Arrow geometry is original; no raster painting is performed here.
"""
import base64
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
NAME = "overload_output_interface"
ID = "overload_sim:" + NAME


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


faces = {face: {"uv": [0, 0, 16, 16], "texture": "#base", "cullface": face}
         for face in ("north", "south", "east", "west", "up", "down")}
elements = [{"name": "Original interface shell", "from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]
# A blocky rightward arrow on all six faces. Thin overlays avoid coincident faces.
rectangles = [(3, 7, 10, 9), (9, 5, 11, 11), (11, 6, 12, 10), (12, 7, 13, 9)]
for face in faces:
    for index, (x0, y0, x1, y1) in enumerate(rectangles):
        if face == "north": lo, hi = [16-x1, 16-y1, -.04], [16-x0, 16-y0, -.02]
        elif face == "south": lo, hi = [x0, 16-y1, 16.02], [x1, 16-y0, 16.04]
        elif face == "west": lo, hi = [-.04, 16-y1, x0], [-.02, 16-y0, x1]
        elif face == "east": lo, hi = [16.02, 16-y1, 16-x1], [16.04, 16-y0, 16-x0]
        elif face == "up": lo, hi = [x0, 16.02, y0], [x1, 16.04, y1]
        else: lo, hi = [16-x1, -.04, y0], [16-x0, -.02, y1]
        elements.append({"name": f"Output arrow {face} {index}", "from": lo, "to": hi,
                         "shade": False, "faces": {face: {"uv": [1, 1, 2, 2], "texture": "#base"}}})
model = {"parent": "minecraft:block/block", "textures": {"base": "ae2lt:block/overloaded_interface", "particle": "ae2lt:block/overloaded_interface"}, "elements": elements}
write(RES / f"assets/overload_sim/models/block/{NAME}.json", model)
write(RES / f"assets/overload_sim/models/item/{NAME}.json", {"parent": "overload_sim:block/"+NAME})
write(RES / f"assets/overload_sim/blockstates/{NAME}.json", {"variants": {"": {"model": "overload_sim:block/"+NAME}}})
write(RES / f"data/overload_sim/recipe/{NAME}.json", {
    "type": "ae2lt:lightning_assembly", "priority": 0,
    "inputs": [{"ingredient": {"item": item}, "count": count} for item, count in
               [("ae2:interface", 4), ("ae2:fluix_pearl", 256), ("ae2lt:overload_machine_frame", 2),
                ("ae2:export_bus", 1), ("ae2lt:overload_singularity", 1)]],
    "result": {"id": ID, "count": 1}, "totalEnergy": 1_000_000,
    "lightningCost": 16, "lightningTier": "extreme_high_voltage"})
write(RES / f"data/overload_sim/loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": ID}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
tag = RES / "data/minecraft/tags/block/mineable/pickaxe.json"
data = json.loads(tag.read_text(encoding="utf-8-sig"))
if ID not in data["values"]: data["values"].append(ID)
write(tag, data)

# Native Blockbench Java block model; embed the licensed reference texture for editing.
texture = ROOT / "art/output-interface/overloaded_interface.png"
if texture.exists():
    import uuid
    bb_elements = []
    for element in elements:
        bb_faces = {face: {"uv": [0, 0, 0, 0], "texture": None} for face in faces}
        bb_faces.update({face: {"uv": info["uv"], "texture": 0} for face, info in element["faces"].items()})
        bb_elements.append({"name": element["name"], "type": "cube", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, ID+element["name"])),
                            "from": element["from"], "to": element["to"], "origin": [8, 8, 8],
                            "faces": bb_faces})
    write(ROOT / f"art/{NAME}.bbmodel", {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False},
        "name": "Lightning Tech: Simulation - Overload Output Interface", "resolution": {"width": 16, "height": 16},
        "elements": bb_elements, "outliner": [el["uuid"] for el in bb_elements],
        "textures": [{"name": "overloaded_interface.png", "id": "0", "uuid": str(uuid.uuid4()), "width": 16, "height": 16,
                      "source": "data:image/png;base64,"+base64.b64encode(texture.read_bytes()).decode()}],
        "credits": "Shell texture AE2 Lightning Tech contributors, CC BY-NC-SA 3.0; output geometry qiqi, MIT."})
print("Output-interface recipe, block resources and Blockbench geometry exported.")
