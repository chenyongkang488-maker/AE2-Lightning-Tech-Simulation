"""Recolor AE2LT 2.1.0's native animated crystal and write its addon resources.

Derived texture/model/animation metadata retain CC BY-NC-SA 3.0.
Source: AE2 Lightning Tech Reborn contributors, assets/ae2lt/*/item/overload_crystal.*
See THIRD_PARTY_NOTICES.md and art/licenses/AE2LT-LICENSE_ASSETS.md.
"""
import colorsys
import hashlib
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/overload_sim"
DATA = ROOT / "src/main/resources/data/overload_sim"
NAME = "transcendent_crystal"
SOURCE_HASHES = {
    "textures/item/overload_crystal.png": "f96c0743e8a43fdd3b8618c3880d1b4b10366b1b776a05965b705fa734512738",
    "textures/item/overload_crystal.png.mcmeta": "7dc3faa599ec91b571abc6036cb1b51e6136dc94dba661eae60e622208978b91",
    "models/item/overload_crystal.json": "ffa698c8980c3ca4926bac5368e9c23050734f55a1c45266c8dbdf48191072d6",
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def pale_blue(pixel):
    red, green, blue, alpha = pixel
    if alpha == 0:
        return pixel
    _, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
    # Keep every source pixel's value (including the travelling pulse), soften
    # saturation, and move pink/magenta hues into a single pale blue palette.
    colored = colorsys.hsv_to_rgb(0.56, 0.025 + saturation * 0.425, value)
    return *(round(channel * 255) for channel in colored), alpha


def generate():
    with ZipFile(ROOT / "libs/ae2lt-2.1.0.jar") as upstream:
        source = {name: upstream.read("assets/ae2lt/" + name) for name in SOURCE_HASHES}
    for name, expected in SOURCE_HASHES.items():
        if hashlib.sha256(source[name]).hexdigest() != expected:
            raise ValueError(f"unexpected upstream asset revision: {name}")

    original = Image.open(BytesIO(source["textures/item/overload_crystal.png"])).convert("RGBA")
    recolored = Image.new("RGBA", original.size)
    for y in range(original.height):
        for x in range(original.width):
            recolored.putpixel((x, y), pale_blue(original.getpixel((x, y))))
    texture = ASSETS / f"textures/item/{NAME}.png"
    texture.parent.mkdir(parents=True, exist_ok=True)
    recolored.save(texture, optimize=False, compress_level=9)
    # Preserve the native frame order and the long rest frame byte-for-byte.
    texture.with_suffix(".png.mcmeta").write_bytes(source["textures/item/overload_crystal.png.mcmeta"])

    model = json.loads(source["models/item/overload_crystal.json"])
    model["textures"]["layer0"] = f"overload_sim:item/{NAME}"
    write_json(ASSETS / f"models/item/{NAME}.json", model)
    write_json(DATA / f"recipe/{NAME}.json", {
        "type": "overload_sim:high_energy_workstation",
        "priority": 10,
        "inputs": [{"ingredient": {"item": "ae2lt:overload_crystal"}, "count": 256}],
        "results": [{"id": f"overload_sim:{NAME}", "count": 4}],
        "totalEnergy": 5,
        "lightningCost": 1,
        "lightningTier": "high_voltage",
    })
    for language, title in (("zh_cn", "超限水晶"), ("en_us", "Transcendent Crystal")):
        path = ASSETS / f"lang/{language}.json"
        translations = json.loads(path.read_text(encoding="utf-8"))
        translations[f"item.overload_sim.{NAME}"] = title
        write_json(path, translations)
    print("Generated Transcendent Crystal: native animated silhouette recolored pale blue; 256 overload crystals -> 4 in the dedicated workstation type.")


if __name__ == "__main__":
    generate()
