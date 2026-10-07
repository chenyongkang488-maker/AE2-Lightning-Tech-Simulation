"""Export JSON models using the existing native pixel textures; never edit bitmap assets."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
DATA = ROOT / 'src/main/resources/data/overload_sim'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(obj, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))


def cube(texture, screen=False, translucent=False):
    textures = {'particle': 'minecraft:block/white_concrete', 'body': texture}
    faces = {f: {'texture': '#body', 'uv': [0, 0, 16, 16], 'cullface': f} for f in FACES}
    if screen:
        textures['screen'] = 'overload_sim:block/simulation_controller_front'
        faces['north']['texture'] = '#screen'
    return {'credit': 'qiqi: existing native powder-white pixel artwork reused for Lightning Tech: Simulation.',
            'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'render_type': 'minecraft:translucent' if translucent else 'minecraft:cutout',
            'textures': textures, 'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}]}


def main():
    models = {
        'high_energy_workstation': cube('overload_sim:block/simulation_frame_formed', screen=True),
        'high_energy_workstation_frame': cube('overload_sim:block/simulation_frame'),
        'high_energy_workstation_frame_formed': cube('overload_sim:block/simulation_frame_formed'),
        'high_energy_workstation_glass': None,
        'high_energy_workstation_glass_formed': cube('overload_sim:block/simulation_glass_north', translucent=True),
        'high_energy_workstation_receiver': cube('overload_sim:block/simulation_frame_formed'),
        'high_energy_workstation_receiver_formed': cube('overload_sim:block/simulation_frame_formed'),
    }
    raw = json.loads((ASSETS/'models/block/laser_glass.json').read_text(encoding='utf-8'))
    models['high_energy_workstation_glass'] = raw
    for name, model in models.items():
        write(ASSETS/f'models/block/{name}.json', model)
    for name in ('high_energy_workstation_frame', 'high_energy_workstation_glass', 'high_energy_workstation_receiver'):
        write(ASSETS/f'blockstates/{name}.json', {'variants': {
            'formed=false': {'model': f'overload_sim:block/{name}'},
            'formed=true': {'model': f'overload_sim:block/{name}_formed'}}})
    write(ASSETS/'blockstates/high_energy_workstation.json', {'variants': {
        f'facing={face}': {'model': 'overload_sim:block/high_energy_workstation', 'y': rotation}
        for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    names = ('high_energy_workstation', 'high_energy_workstation_frame',
             'high_energy_workstation_glass', 'high_energy_workstation_receiver')
    for name in names:
        write(ASSETS/f'models/item/{name}.json', {'parent': f'overload_sim:block/{name}'})
        write(DATA/f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'overload_sim:{name}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # Construction materials stay obtainable from the existing technology progression.
    write(DATA/'recipe/high_energy_workstation_frame.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'overload_sim:simulation_frame'}, {'item': 'overload_sim:laser_frame'}],
        'result': {'id': 'overload_sim:high_energy_workstation_frame', 'count': 2}})
    write(DATA/'recipe/high_energy_workstation_glass.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'overload_sim:laser_glass'}],
        'result': {'id': 'overload_sim:high_energy_workstation_glass', 'count': 1}})
    write(DATA/'recipe/high_energy_workstation_receiver.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'overload_sim:laser_optical_port'}, {'item': 'overload_sim:laser_source_crystal'}],
        'result': {'id': 'overload_sim:high_energy_workstation_receiver', 'count': 1}})
    write(DATA/'recipe/high_energy_workstation.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'ae2lt:overload_processing_factory'},
                        {'item': 'overload_sim:high_energy_workstation_receiver'},
                        {'item': 'overload_sim:high_energy_workstation_frame'}],
        'result': {'id': 'overload_sim:high_energy_workstation', 'count': 1}})
    tags = ROOT/'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json'
    obj = json.loads(tags.read_text(encoding='utf-8'))
    for name in names:
        value = f'overload_sim:{name}'
        if value not in obj['values']:
            obj['values'].append(value)
    write(tags, obj)
    print('Exported 4 workstation blocks, 7 models, recipes and loot with existing native textures.')


if __name__ == '__main__':
    main()
