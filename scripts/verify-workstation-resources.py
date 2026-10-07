"""Validate shared workstation parts, legacy recovery assets, model links and compact AE layout."""
from pathlib import Path
import json
import re
from PIL import Image

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

    controller = read(ASSETS / f'models/block/{CONTROLLER}.json')
    assert controller['parent'] == 'overload_sim:block/high_energy_laser_emitter'
    assert controller['textures']['workstation_wrench'] == 'overload_sim:block/workstation_controller_wrench'
    glyphs = [element for element in controller['elements'] if element.get('name') == 'yellow workstation wrench']
    assert len(glyphs) == 1 and all(face['texture'] == '#workstation_wrench' for face in glyphs[0]['faces'].values())
    with Image.open(ASSETS / 'textures/block/workstation_controller_wrench.png') as glyph:
        assert glyph.size == (16, 16) and glyph.mode == 'RGBA', 'workstation wrench must be a native transparent 16px glyph'
        assert glyph.getextrema()[3] == (0, 255), 'workstation glyph must retain clear and opaque pixels'
        bounds = glyph.getchannel('A').getbbox()
        assert bounds and max(bounds[2]-bounds[0], bounds[3]-bounds[1]) <= 10, 'wrench footprint must match the adjacent equipment glyphs'
        assert bounds[0] + bounds[2] == bounds[1] + bounds[3] == 16, 'small wrench must stay centred in its existing glyph geometry'
    with Image.open(ASSETS / 'textures/block/workstation_shell_atlas.png') as atlas:
        assert atlas.size == (128, 128) and atlas.mode == 'RGBA', 'workstation shell needs its own native 8x8 tile atlas'
        # Renderer samples the tile-4 interior at (66.5, 2.5) through (77.5, 13.5).
        # Check actual interior alpha, since tile dimensions alone cannot protect windows.
        glass = atlas.crop((66, 2, 78, 14))
        assert glass.getextrema()[3][1] < 255, 'workstation glazing interior must stay translucent'
        assert glass.getextrema()[3][1] > 0, 'workstation glazing must retain its visible native tint'
        with Image.open(ASSETS / 'textures/block/laser_pixel_atlas.png') as laser:
            assert atlas.crop((64, 0, 80, 16)).tobytes() == laser.crop((64, 0, 80, 16)).tobytes(), 'tile 4 must preserve the native laser glass pixels'
        beam_white = atlas.crop((32, 80, 48, 96))
        assert beam_white.getextrema() == ((255, 255),)*4, 'tile 42 must be uniformly opaque white for stable beam tint and alpha'
        assert atlas.getpixel((40, 88)) == (255, 255, 255, 255), 'beam centre sample must have no texture tint or transparency'
        for tile in (5, 7, 32, 38, 44):
            region = atlas.crop(((tile % 8)*16, (tile // 8)*16, (tile % 8+1)*16, (tile // 8+1)*16))
            assert region.getextrema()[3] == (255, 255), 'shell panels must stay opaque'
            assert all(abs(r-g) <= 22 and abs(g-b) <= 22
                       for r, g, b, a in (region.getpixel((x, y)) for y in range(16) for x in range(16))), 'framework must use grey/navy panels without pink decoration'
    shell_model = resolved_model('overload_sim:block/workstation_shell_atlas')
    assert shell_model['textures']['all'] == 'overload_sim:block/workstation_shell_atlas'
    registration = (ROOT / 'src/main/java/dev/overloadsim/client/ClientRegistration.java').read_text(encoding='utf-8')
    assert '"workstation_shell_atlas"' in registration, 'standalone shell model must be registered for texture stitching'
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
    assert layout['slots']['MACHINE_INPUT']['left'] == 33 and layout['slots']['MACHINE_OUTPUT']['left'] == 115
    assert layout['slots']['MACHINE_INPUT']['top'] == layout['slots']['MACHINE_OUTPUT']['top'] == 75
    grids = [(layout['slots'][slot]['left'], layout['slots'][slot]['top'], 54, 54)
             for slot in ('MACHINE_INPUT', 'MACHINE_OUTPUT')]
    fluids = [(10, 71, 14, 60), (180, 71, 14, 60)]
    panels = [(7, 89), (112, 194)]
    def overlaps(a, b):
        return a[0] < b[0]+b[2] and b[0] < a[0]+a[2] and a[1] < b[1]+b[3] and b[1] < a[1]+a[3]
    for index, (grid, fluid, panel) in enumerate(zip(grids, fluids, panels)):
        assert not overlaps(grid, fluid), f'fluid tank overlaps item grid {index}'
        assert all(panel[0] <= rect[0] and rect[0]+rect[2] <= panel[1] for rect in (grid, fluid))
        assert all(71 <= rect[1] and rect[1]+rect[3] <= 131 for rect in (grid, fluid))
    assert not overlaps(grids[0], grids[1]) and not overlaps(fluids[0], fluids[1])
    for slot in ('PLAYER_INVENTORY', 'PLAYER_HOTBAR'):
        height = 54 if slot == 'PLAYER_INVENTORY' else 18
        assert layout['slots'][slot]['top'] + height <= layout['generatedBackground']['height']
    for key in translations['en_us']:
        if key.startswith('gui.overload_sim.workstation.'):
            assert key in translations['zh_cn'], key
    print('Workstation resources verified: compact yellow wrench, independent grey shell atlas with native translucent glass, shared-part models, legacy recovery and nonoverlapping tall-fluid layout.')


if __name__ == '__main__':
    verify()
