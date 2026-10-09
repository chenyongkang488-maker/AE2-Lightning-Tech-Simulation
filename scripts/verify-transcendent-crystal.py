"""Verify the shipped crystal keeps AE2LT's silhouette, animation and display model."""
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/overload_sim"
NAME = "transcendent_crystal"


def verify():
    with ZipFile(ROOT / "libs/ae2lt-2.1.0.jar") as upstream:
        original = Image.open(BytesIO(upstream.read("assets/ae2lt/textures/item/overload_crystal.png"))).convert("RGBA")
        original_metadata = upstream.read("assets/ae2lt/textures/item/overload_crystal.png.mcmeta")
        original_model = json.loads(upstream.read("assets/ae2lt/models/item/overload_crystal.json"))

    texture = ASSETS / f"textures/item/{NAME}.png"
    assert texture.is_file(), "the new crystal texture must be shipped"
    with Image.open(texture) as generated:
        generated = generated.convert("RGBA")
        assert generated.size == original.size == (16, 256), "retain all sixteen native 16px frames"
        assert generated.getchannel("A").tobytes() == original.getchannel("A").tobytes(), "retain the exact animated silhouette"
        recolors = {}
        for y in range(original.height):
            for x in range(original.width):
                before, after = original.getpixel((x, y)), generated.getpixel((x, y))
                if before[3] == 0:
                    continue
                assert max(before[:3]) == max(after[:3]), "retain each pixel's brightness and animation pulse"
                assert recolors.setdefault(before, after) == after, "a source color must recolor identically in every frame"
                assert after[2] >= after[1] >= after[0], "the whole crystal uses a blue palette"
                assert after[:3] != before[:3], "every visible source color must be recolored"
        bright = [pixel for pixel in recolors.values() if max(pixel[:3]) >= 200]
        assert bright and all(pixel[0] >= 140 and pixel[1] >= 180 for pixel in bright), "highlights must read as pale blue"
        for frame in range(16):
            area = (0, frame * 16, 16, (frame + 1) * 16)
            assert generated.crop(area).getchannel("A").getbbox() == original.crop(area).getchannel("A").getbbox()

    # Git normalizes text resources to LF; whitespace does not change animation timing or frame order.
    assert json.loads((ASSETS / f"textures/item/{NAME}.png.mcmeta").read_text(encoding="utf-8")) == json.loads(original_metadata), "animation timing and frame order stay identical"
    model = json.loads((ASSETS / f"models/item/{NAME}.json").read_text(encoding="utf-8"))
    original_model["textures"]["layer0"] = f"overload_sim:item/{NAME}"
    assert model == original_model, "retain the original model and inherited item display transforms"
    for language, title in (("zh_cn", "超限水晶"), ("en_us", "Transcendent Crystal")):
        translations = json.loads((ASSETS / f"lang/{language}.json").read_text(encoding="utf-8"))
        assert translations[f"item.overload_sim.{NAME}"] == title
    print("Transcendent Crystal resources verified: pale blue pixels, exact native alpha/brightness, sixteen frames, unchanged animation timing and item model.")


if __name__ == "__main__":
    verify()
