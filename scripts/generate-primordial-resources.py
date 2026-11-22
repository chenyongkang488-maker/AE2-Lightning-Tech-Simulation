"""Author the primordial module's pixel geometry and synchronize editable/runtime resources."""
import base64
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/overload_sim'
NAME = 'simulation_primordial_module'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')

# Keep the same pink-and-white ceramic frame as the other chamber modules.
# The center is an upright green diamond with a restrained light-green motif.
image = Image.open(ASSETS / 'textures/block/simulation_smelting_module.png').convert('RGBA')
draw = ImageDraw.Draw(image)
draw.rectangle((9, 9, 22, 22), fill='#fdf6fc')
draw.polygon([(16, 8), (22, 16), (16, 24), (10, 16)], fill='#4a985b')
draw.polygon([(16, 10), (20, 16), (16, 22), (12, 16)], fill='#78c86c')
draw.line([(16, 12), (14, 15), (16, 17), (18, 15)], fill='#c1e99b', width=1)
draw.line([(16, 17), (16, 20)], fill='#b1df8c', width=1)
texture = ASSETS / 'textures/block' / f'{NAME}.png'
image.save(texture)
source = 'data:image/png;base64,' + base64.b64encode(texture.read_bytes()).decode()
model = json.loads((ASSETS / 'models/block/simulation_smelting_module.json').read_text(encoding='utf8'))
model['textures']['0'] = f'overload_sim:block/{NAME}'
write(ASSETS / 'models/block' / f'{NAME}.json', model)
write(ASSETS / 'models/item' / f'{NAME}.json', {'parent': f'overload_sim:block/{NAME}'})
write(ASSETS / 'blockstates' / f'{NAME}.json', {'variants': {
    f'formed={value}': {'model': f'overload_sim:block/{NAME}'} for value in ('false', 'true')}})
project = json.loads((ROOT / 'art/simulation_smelting_module.bbmodel').read_text(encoding='utf8'))
project['name'] = NAME
for tex in project['textures']:
    tex.update(name=f'{NAME}.png', path=str(texture), source=source, layers_enabled=False, layers=[])
write(ROOT / 'art' / f'{NAME}.bbmodel', project)
write(RES / 'data/overload_sim/loot_table/blocks' / f'{NAME}.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'overload_sim:{NAME}'}],
    'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
tag = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
data = json.loads(tag.read_text(encoding='utf8'))
if f'overload_sim:{NAME}' not in data['values']:
    data['values'].append(f'overload_sim:{NAME}')
write(tag, data)
default = RES / 'data/overload_sim/multiblock_simulation/default.json'
data = json.loads(default.read_text(encoding='utf8'))
data['policy']['primordial_hv'] = 2
data.setdefault('primordial', [])
write(default, data)
print('Primordial model, editable Blockbench project, texture, loot, mining tag and default policy written.')
