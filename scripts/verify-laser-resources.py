"""Verify the playable prototype assets, Blockbench parity and all local references."""
from pathlib import Path
import base64
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
MACHINES = ['high_energy_laser_emitter', 'laser_constraint_matrix', 'laser_enhancement_matrix',
            'laser_burst_matrix', 'laser_deflector_matrix', 'laser_focus_matrix', 'collapse_core', 'particle_constrainer']
PARTS = ['laser_frame', 'laser_glass', 'laser_source_crystal', 'laser_optical_port']

def read(path):
    return json.loads(path.read_text(encoding='utf-8'))

def verify():
    project = read(ROOT / 'art/blockbench/laser-prototype.bbmodel')
    assert project['meta']['model_format'] == 'free'
    assert len(project['outliner']) == len(MACHINES + PARTS)
    for texture in project['textures']:
        path = (ROOT / 'art/blockbench' / texture['relative_path']).resolve()
        assert path.is_relative_to(ROOT), path
        assert path.exists(), path
        embedded = base64.b64decode(texture['source'].split(',', 1)[1])
        assert hashlib.sha256(path.read_bytes()).digest() == hashlib.sha256(embedded).digest(), path
    groups = {group['name']: group for group in project['outliner']}
    project_elements = {element['uuid']: element for element in project['elements']}
    for name in MACHINES + PARTS:
        model = read(ASSETS / f'models/block/{name}.json')
        assert model['elements'], name
        assert model['textures']['particle'] == 'minecraft:block/white_concrete', name
        for ref in model['textures'].values():
            if ref.startswith('overload_sim:'):
                assert (ASSETS / f'textures/{ref.split(":", 1)[1]}.png').exists(), ref
        for element in model['elements']:
            assert all(0 <= c <= 16 for c in element['from'] + element['to']), name
            for face in element['faces'].values():
                assert face['texture'][1:] in model['textures'], (name, face)
                assert all(0 <= value <= 16 for value in face['uv']), (name, face)
        assert len(groups[name]['children']) == len(model['elements']), name
        # The workshop preview and exported voxel geometry must be identical, up to group placement.
        offset = groups[name]['origin']
        for child, element in zip(groups[name]['children'], model['elements']):
            authored = project_elements[child]
            for edge in ('from', 'to'):
                assert authored[edge] == [element[edge][i] + offset[i] for i in range(3)], (name, edge)
            for face_name, face in element['faces'].items():
                painted = authored['faces'][face_name]
                texture = project['textures'][painted['texture']]
                scales = [texture['uv_width']/16,texture['uv_height']/16]*2
                assert painted['uv'] == [v*s for v,s in zip(face['uv'],scales)], (name, face_name)
        state = read(ASSETS / f'blockstates/{name}.json')['variants']
        if name in MACHINES:
            assert set(state) == {f'facing={face}' for face in ('north', 'south', 'east', 'west', 'up', 'down')}, name
        for variant in state.values():
            assert variant['model'] == f'overload_sim:block/{name}', name
        assert read(ASSETS / f'models/item/{name}.json')['parent'] == f'overload_sim:block/{name}'
        loot = read(ROOT / f'src/main/resources/data/overload_sim/loot_table/blocks/{name}.json')
        assert loot['pools'][0]['entries'][0]['name'] == f'overload_sim:{name}', name
    assert read(ASSETS / 'models/block/laser_glass.json')['render_type'] == 'minecraft:translucent'
    glass_model = read(ASSETS / 'models/block/laser_glass.json')
    assert glass_model['textures']['glass'] == 'overload_sim:block/laser_pixel_atlas'
    assert all(face['uv'] == [8,0,10,2] for face in glass_model['elements'][0]['faces'].values()), 'Framed glass must use native painted tile4'
    atlas_project=read(ROOT / 'art/blockbench/laser-pixel-atlas.bbmodel')
    assert atlas_project['textures'][0]['layers'], 'Native Blockbench painted layer missing'
    native_atlas=base64.b64decode(atlas_project['textures'][0]['source'].split(',',1)[1])
    assert native_atlas == (ASSETS / 'textures/block/laser_pixel_atlas.png').read_bytes(), 'Native painted project differs from runtime atlas'
    formed=read(ROOT / 'art/blockbench/particle-constrainer-formed.bbmodel')
    assert formed['meta']['model_format'] == 'free' and len(formed['outliner']) == 3
    assert 1000 < len(formed['elements']) < 4000, 'Formed workshop mesh must remain bounded'
    assert base64.b64decode(formed['textures'][0]['source'].split(',',1)[1]) == native_atlas
    for element in formed['elements']:
        assert element['type'] == 'mesh'
        for coordinates in element['vertices'].values():
            assert all(math.isfinite(value) for value in coordinates)
            assert -56.1 <= coordinates[0] <= 56.1 and -88.1 <= coordinates[1] <= 56.1 and -56.1 <= coordinates[2] <= 56.1
        for face in element['faces'].values():
            assert len(set(face['vertices'])) >= 3
            assert all(vertex in element['vertices'] for vertex in face['vertices'])
            assert all(0 <= value <= 128 for pair in face['uv'].values() for value in pair)
    for texture in project['textures']:
        if texture['name'] == 'simulation_controller_ecg.png':
            assert (texture['uv_width'],texture['uv_height']) == (32,16), 'Animated ECG UV must use one frame'
    style = read(ROOT / 'src/main/resources/assets/ae2/screens/overload_sim/laser.json')
    assert style['helpTopic'] == 'overload_sim:laser-particle.md'
    for folder in ('', '_zh_cn/'):
        guide = (ASSETS / f'ae2guide/{folder}laser-particle.md').read_text(encoding='utf-8')
        for name in MACHINES + PARTS:
            assert f'overload_sim:{name}' in guide, name
    for locale in ('en_us', 'zh_cn'):
        snippets = read(ROOT / f'art/blockbench/laser-lang-{locale}.json')
        for name in MACHINES + PARTS:
            assert f'block.overload_sim.{name}' in snippets
    print(f'Laser resources verified: {len(MACHINES + PARTS)} block/item/state/loot sets, '
          f'{len(project["elements"])} matching Blockbench elements, {len(project["textures"])} embedded textures; UV parity, native paint layer, six facing variants, guides and language snippets.')

if __name__ == '__main__':
    verify()
