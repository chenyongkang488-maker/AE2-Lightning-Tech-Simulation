"""Verify shipped output-interface manufacturing and client resources."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "overload_sim:overload_output_interface"


def read(path):
    return json.loads((RES / path).read_text(encoding="utf-8-sig"))


recipe = read("data/overload_sim/recipe/overload_output_interface.json")
assert recipe["type"] == "ae2lt:lightning_assembly"
assert recipe["result"] == {"id": ID, "count": 1}
materials = {entry["ingredient"]["item"]: entry["count"] for entry in recipe["inputs"]}
assert materials == {"ae2:interface": 4, "ae2:fluix_pearl": 256,
                     "ae2lt:overload_machine_frame": 2, "ae2:export_bus": 1,
                     "ae2lt:overload_singularity": 1}
assert recipe["totalEnergy"] == 1_000_000 and recipe["lightningCost"] == 16
assert recipe["lightningTier"] == "extreme_high_voltage"
assert read("data/overload_sim/loot_table/blocks/overload_output_interface.json")["pools"][0]["entries"][0]["name"] == ID
assert ID in read("data/minecraft/tags/block/mineable/pickaxe.json")["values"]
assert read("assets/overload_sim/models/item/overload_output_interface.json")["parent"] == "overload_sim:block/overload_output_interface"
blockstate = read("assets/overload_sim/blockstates/overload_output_interface.json")
assert all(v["model"] == "overload_sim:block/overload_output_interface" for v in blockstate["variants"].values())
model = read("assets/overload_sim/models/block/overload_output_interface.json")
assert len(model["elements"]) > 1, "Must contain distinct outward arrow geometry"
native = json.loads((ROOT / "art/overload_output_interface.bbmodel").read_text(encoding="utf-8"))
assert len(native["elements"]) == len(model["elements"]) == 25
assert len(set(native["outliner"])) == 25
for editable, runtime in zip(native["elements"], model["elements"]):
    assert editable["from"] == runtime["from"] and editable["to"] == runtime["to"]
    assert set(editable["faces"]) == {"north", "south", "east", "west", "up", "down"}
for locale in ("zh_cn", "en_us"):
    language = read(f"assets/overload_sim/lang/{locale}.json")
    assert language["block.overload_sim.overload_output_interface"]
    guide = RES / "assets/overload_sim/ae2guide" / ("_zh_cn" if locale == "zh_cn" else "") / "overload-output-interface.md"
    assert ID in guide.read_text(encoding="utf-8")
print("Output interface recipe, loot, model, mining tag, translations and both guides verified.")
