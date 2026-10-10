"""Export the output interface's editable Blockbench geometry and game assets.

The shell texture is a pink/white/ice-blue adaptation of AE2 Lightning Tech
(CC BY-NC-SA 3.0), retained with its editable Blockbench project. No arrow overlay.
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
elements = [{"name": "Pink white blue interface shell", "from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]
model = {"parent": "minecraft:block/block", "textures": {"base": "overload_sim:block/overload_output_interface", "particle": "overload_sim:block/overload_output_interface"}, "elements": elements}
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
texture = RES / "assets/overload_sim/textures/block/overload_output_interface.png"
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
        "textures": [{"name": "overload_output_interface.png", "id": "0", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, ID+":texture")), "width": 16, "height": 16,
                      "source": "data:image/png;base64,"+base64.b64encode(texture.read_bytes()).decode()}],
        "credits": "Shell texture adapted from AE2 Lightning Tech contributors, pink/white/ice-blue color edit qiqi, CC BY-NC-SA 3.0; cube geometry MIT."})
print("Output-interface recipe, block resources and Blockbench geometry exported.")
