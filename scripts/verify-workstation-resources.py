"""Validate workstation model links, obtainable parts, guides and compact AE layout."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
DATA = ROOT / 'src/main/resources/data/overload_sim'
NAMES = ['high_energy_workstation', 'high_energy_workstation_frame',
         'high_energy_workstation_glass', 'high_energy_workstation_receiver']


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


translations = {lang: read(ASSETS / f'lang/{lang}.json') for lang in ('en_us', 'zh_cn')}
mining = read(ROOT / 'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json')['values']
for name in NAMES:
    for language in translations.values():
        assert language[f'block.overload_sim.{name}'], name
    assert f'overload_sim:{name}' in mining, name
    for variant in read(ASSETS / f'blockstates/{name}.json')['variants'].values():
        model = read(ASSETS / ('models/' + variant['model'].split(':', 1)[1] + '.json'))
        assert model['textures']['particle'] == 'minecraft:block/white_concrete', name
        for texture in model['textures'].values():
            if texture.startswith('overload_sim:'):
                assert (ASSETS / ('textures/' + texture.split(':', 1)[1] + '.png')).is_file(), texture
    assert read(ASSETS / f'models/item/{name}.json')['parent'] == f'overload_sim:block/{name}'
    assert read(DATA / f'loot_table/blocks/{name}.json')['pools'][0]['entries'][0]['name'] == f'overload_sim:{name}'
    assert read(DATA / f'recipe/{name}.json')['result']['id'] == f'overload_sim:{name}'
    for language in ('', '_zh_cn/'):
        assert f'overload_sim:{name}' in (ASSETS / f'ae2guide/{language}high-energy-workstation.md').read_text(encoding='utf-8')

layout = read(ROOT / 'src/main/resources/assets/ae2/screens/overload_sim/workstation.json')
assert layout['generatedBackground']['height'] <= 240, 'Default window must show the entire inventory'
assert layout['slots']['MACHINE_INPUT']['grid'] == layout['slots']['MACHINE_OUTPUT']['grid'] == 'BREAK_AFTER_3COLS'
for slot in ('PLAYER_INVENTORY', 'PLAYER_HOTBAR'):
    height = 54 if slot == 'PLAYER_INVENTORY' else 18
    assert layout['slots'][slot]['top'] + height <= layout['generatedBackground']['height']
for key in translations['en_us']:
    if key.startswith('gui.overload_sim.workstation.'):
        assert key in translations['zh_cn'], key
assert read(ASSETS / 'models/block/high_energy_workstation_glass_formed.json')['render_type'] == 'minecraft:translucent'
print('Four workstation parts: state/model/texture/loot/recipe links, bilingual guides and compact AE layout verified.')
