"""Check authored Blockbench exports and their runtime texture references."""
from pathlib import Path
import base64, hashlib, json
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/overload_sim'
names=['simulation_controller','simulation_frame','simulation_efficiency_t1','simulation_efficiency_t2','simulation_efficiency_t3','simulation_fortune_module','simulation_overload_module','simulation_smelting_module','simulation_frame_formed','simulation_emitter']+['simulation_glass_'+f for f in ('north','south','east','west','up','down')]
checked=0
for name in names+['multiblock_simulation_assembly']:
    project=json.loads((ROOT/'art'/f'{name}.bbmodel').read_text(encoding='utf8'))
    assert project['elements'],name
    for texture in project['textures']:
        path=ASSETS/'textures/block'/texture['name']
        actual=path.read_bytes()
        embedded=base64.b64decode(texture['source'].split(',',1)[1])
        assert hashlib.sha256(actual).digest()==hashlib.sha256(embedded).digest(),f'texture parity: {name}/{path.name}'
        checked+=1
for name in names:
    model=json.loads((ASSETS/'models/block'/f'{name}.json').read_text(encoding='utf8'))
    assert model['elements'] and model['render_type'] in ('minecraft:cutout','minecraft:translucent'),name
    for ref in model['textures'].values():
        if ref.startswith('overload_sim:'):
            assert (ASSETS/'textures'/f'{ref.split(":",1)[1]}.png').exists(),ref
    if name.startswith('simulation_glass_'):
        assert model['texture_size']==[16,16]
        assert model['elements'][0]['faces']['north']['uv']==[0,0,16,16]
for path in (ROOT/'src/main/resources').rglob('*.json'):
    json.loads(path.read_text(encoding='utf8'))
print(f'{len(names)+1} Blockbench projects, {checked} embedded PNGs and {len(names)} runtime models verified; resource JSON parses.')
