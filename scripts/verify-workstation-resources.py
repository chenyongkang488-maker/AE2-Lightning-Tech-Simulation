"""Validate shared workstation parts, legacy recovery assets, model links and compact AE layout."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
DATA = ROOT / 'src/main/resources/data/overload_sim'
CONTROLLER = 'high_energy_workstation'
LEGACY_PARTS = {
    'high_energy_workstation_frame': 'laser_frame',
    'high_energy_workstation_glass': 'laser_glass',
    'high_energy_workstation_receiver': 'laser_optical_port',
}
NAMES = (CONTROLLER, *LEGACY_PARTS)


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def resolved_model(reference, ancestors=()):
    assert reference not in ancestors, f'model cycle: {reference}'
    if not reference.startswith('overload_sim:'):
        return {}
    model = read(ASSETS / ('models/' + reference.split(':', 1)[1] + '.json'))
    inherited = resolved_model(model['parent'], (*ancestors, reference)) if 'parent' in model else {}
    return {**inherited, **model, 'textures': {**inherited.get('textures', {}), **model.get('textures', {})}}


def verify():
    translations = {lang: read(ASSETS / f'lang/{lang}.json') for lang in ('en_us', 'zh_cn')}
    mining = read(ROOT / 'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json')['values']
    for name in NAMES:
        for language in translations.values():
            assert language[f'block.overload_sim.{name}'], name
        assert f'overload_sim:{name}' in mining, name
        for variant in read(ASSETS / f'blockstates/{name}.json')['variants'].values():
            model = resolved_model(variant['model'])
            assert model['textures']['particle'] == 'minecraft:block/white_concrete', name
            assert model.get('elements'), f'model has no inherited geometry: {name}'
            for texture in model['textures'].values():
                if texture.startswith('overload_sim:'):
                    assert (ASSETS / ('textures/' + texture.split(':', 1)[1] + '.png')).is_file(), texture
        resolved_model(f'overload_sim:item/{name}')
        assert read(ASSETS / f'models/item/{name}.json')['parent'] == f'overload_sim:block/{name}'
        assert read(DATA / f'loot_table/blocks/{name}.json')['pools'][0]['entries'][0]['name'] == f'overload_sim:{LEGACY_PARTS.get(name, name)}', name

    assert read(ASSETS / f'models/block/{CONTROLLER}.json')['parent'] == 'overload_sim:block/high_energy_laser_emitter'
    for legacy, shared in LEGACY_PARTS.items():
        for suffix in ('', '_formed'):
            assert read(ASSETS / f'models/block/{legacy}{suffix}.json')['parent'] == f'overload_sim:block/{shared}'
        assert not (DATA / f'recipe/{legacy}.json').exists(), f'retired acquisition recipe: {legacy}'
        conversion = read(DATA / f'recipe/{legacy}_to_{shared}.json')
        assert conversion['type'] == 'minecraft:crafting_shapeless'
        assert conversion['ingredients'] == [{'item': f'overload_sim:{legacy}'}], legacy
        assert conversion['result'] == {'id': f'overload_sim:{shared}', 'count': 1}, legacy
    assert resolved_model('overload_sim:block/high_energy_workstation_glass_formed')['render_type'] == 'minecraft:translucent'
    legacy_ids = {f'overload_sim:{name}' for name in LEGACY_PARTS}
    for path in (DATA / 'recipe').rglob('*.json'):
        result = read(path).get('result', {})
        if isinstance(result, dict):
            assert result.get('id', result.get('item')) not in legacy_ids, f'recipe produces a retired part: {path.name}'
    controller_recipe = read(DATA / f'recipe/{CONTROLLER}.json')
    assert controller_recipe['result'] == {'id': f'overload_sim:{CONTROLLER}', 'count': 1}
    assert controller_recipe['ingredients'] == [
        {'item': 'ae2lt:overload_processing_factory'}, {'item': 'overload_sim:laser_optical_port'}, {'item': 'overload_sim:laser_frame'}]

    # Creative visibility is declared through this public export list; IDs stay registered for old saves.
    source = (ROOT / 'src/main/java/dev/overloadsim/workstation/WorkstationContent.java').read_text(encoding='utf-8')
    exported = re.search(r'playerBlocks\s*\(\s*\)\s*\{\s*return\s+List\.of\((.*?)\)\s*;', source, re.S)
    assert exported, 'workstation creative export declaration missing'
    assert re.findall(r'\b(\w+)\.get\(\)', exported.group(1)) == ['CONTROLLER'], 'legacy workstation parts must remain hidden from the creative tab'
    for language in ('', '_zh_cn/'):
        guide = (ASSETS / f'ae2guide/{language}high-energy-workstation.md').read_text(encoding='utf-8')
        assert not any(legacy in guide for legacy in legacy_ids), 'public guide exposes retired part IDs'
        for name in (CONTROLLER, *LEGACY_PARTS.values()):
            assert f'overload_sim:{name}' in guide, f'missing public part: {name}'

    layout = read(ROOT / 'src/main/resources/assets/ae2/screens/overload_sim/workstation.json')
    assert layout['generatedBackground'] == {'width': 200, 'height': 238}, 'compact workstation window dimensions changed'
    assert layout['slots']['MACHINE_INPUT']['grid'] == layout['slots']['MACHINE_OUTPUT']['grid'] == 'BREAK_AFTER_3COLS'
    for slot in ('MACHINE_INPUT', 'MACHINE_OUTPUT'):
        top = layout['slots'][slot]['top']
        assert top + 54 <= 128, 'item grids overlap the fluid controls at y128'
    assert layout['slots']['MACHINE_INPUT']['left'] == 16 and layout['slots']['MACHINE_OUTPUT']['left'] == 128
    for slot in ('PLAYER_INVENTORY', 'PLAYER_HOTBAR'):
        height = 54 if slot == 'PLAYER_INVENTORY' else 18
        assert layout['slots'][slot]['top'] + height <= layout['generatedBackground']['height']
    for key in translations['en_us']:
        if key.startswith('gui.overload_sim.workstation.'):
            assert key in translations['zh_cn'], key
    print('Workstation resources verified: shared-part models, hidden legacy exports, canonical drops, 1:1 conversions, public guides and compact fluid layout.')


if __name__ == '__main__':
    verify()
