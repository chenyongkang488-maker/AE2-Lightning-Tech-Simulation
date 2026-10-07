"""Export workstation compatibility assets using the existing laser models and registered IDs."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
DATA = ROOT / 'src/main/resources/data/overload_sim'
CONTROLLER = 'high_energy_workstation'
LEGACY_PARTS = {
    'high_energy_workstation_frame': 'laser_frame',
    'high_energy_workstation_glass': 'laser_glass',
    'high_energy_workstation_receiver': 'laser_optical_port',
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(obj, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))


def model_alias(name):
    return {'parent': f'overload_sim:block/{name}'}


def main():
    models = {CONTROLLER: model_alias('high_energy_laser_emitter')}
    for legacy, shared in LEGACY_PARTS.items():
        models[legacy] = models[f'{legacy}_formed'] = model_alias(shared)
    for name, model in models.items():
        write(ASSETS/f'models/block/{name}.json', model)
    for name in LEGACY_PARTS:
        write(ASSETS/f'blockstates/{name}.json', {'variants': {
            'formed=false': {'model': f'overload_sim:block/{name}'},
            'formed=true': {'model': f'overload_sim:block/{name}_formed'}}})
    write(ASSETS/'blockstates/high_energy_workstation.json', {'variants': {
        f'facing={face}': {'model': 'overload_sim:block/high_energy_workstation', 'y': rotation}
        for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    names = (CONTROLLER, *LEGACY_PARTS)
    for name in names:
        write(ASSETS/f'models/item/{name}.json', {'parent': f'overload_sim:block/{name}'})
        write(DATA/f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'overload_sim:{LEGACY_PARTS.get(name, name)}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    # Keep old inventory items recoverable without exposing new recipes that produce retired parts.
    for legacy, shared in LEGACY_PARTS.items():
        (DATA/f'recipe/{legacy}.json').unlink(missing_ok=True)
        write(DATA/f'recipe/{legacy}_to_{shared}.json', {'type': 'minecraft:crafting_shapeless',
            'ingredients': [{'item': f'overload_sim:{legacy}'}],
            'result': {'id': f'overload_sim:{shared}', 'count': 1}})
    write(DATA/'recipe/high_energy_workstation.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': 'ae2lt:overload_processing_factory'},
                        {'item': 'overload_sim:laser_optical_port'},
                        {'item': 'overload_sim:laser_frame'}],
        'result': {'id': 'overload_sim:high_energy_workstation', 'count': 1}})
    write(ROOT/'src/main/resources/assets/ae2/screens/overload_sim/workstation.json', {
        'includes': ['../common/common.json'], 'generatedBackground': {'width': 200, 'height': 238},
        'slots': {
            'MACHINE_INPUT': {'left': 16, 'top': 73, 'grid': 'BREAK_AFTER_3COLS'},
            'MACHINE_OUTPUT': {'left': 128, 'top': 73, 'grid': 'BREAK_AFTER_3COLS'},
            'PLAYER_INVENTORY': {'left': 20, 'top': 160, 'grid': 'BREAK_AFTER_9COLS'},
            'PLAYER_HOTBAR': {'left': 20, 'top': 218, 'grid': 'BREAK_AFTER_9COLS'},
        }})
    print('Exported workstation controller and legacy model aliases, canonical drops and three 1:1 conversion recipes.')


if __name__ == '__main__':
    main()
