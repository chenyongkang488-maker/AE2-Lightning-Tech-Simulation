"""Export workstation compatibility assets using the existing laser models and registered IDs."""
import json
from pathlib import Path
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


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(obj, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))


def model_alias(name):
    return {'parent': f'overload_sim:block/{name}'}


def controller_model():
    source = json.loads((ASSETS/'models/block/high_energy_laser_emitter.json').read_text(encoding='utf-8'))
    elements = source['elements']
    for element in elements:
        if element.get('name') == 'equipment function glyph':
            element['name'] = 'yellow workstation wrench'
            for face in element['faces'].values():
                face['texture'] = '#workstation_wrench'
                face['uv'] = [0, 0, 16, 16]
    return {'parent': 'overload_sim:block/high_energy_laser_emitter',
            'textures': {'workstation_wrench': 'overload_sim:block/workstation_controller_wrench'},
            'elements': elements}


def wrench_texture():
    # Keep the emitter's 8x8 geometry. Only ten texture pixels carry the mark,
    # matching the visual footprint of the nearby matrix and deflector glyphs.
    mask = [
        '.....##.', '....#...', '....#..#', '.....###',
        '....###.', '...##...', '.###....', '##......',
    ]
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    points = {(x+4, y+4) for y, row in enumerate(mask) for x, pixel in enumerate(row) if pixel == '#'}
    for x, y in points:
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            if 0 <= x+dx < 16 and 0 <= y+dy < 16 and (x+dx, y+dy) not in points:
                image.putpixel((x+dx, y+dy), (113, 82, 31, 255))
    for x, y in points:
        image.putpixel((x, y), (255, 215, 85, 255))
    path = ASSETS/'textures/block/workstation_controller_wrench.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def shell_atlas():
    # An independent 8x8 grid of native 16px tiles, with the laser atlas's
    # numbering contract. Formed workstation meshes alone use this sprite.
    palette = {5: (31, 35, 48), 7: (108, 113, 121), 32: (164, 168, 172),
               38: (182, 185, 188), 44: (74, 81, 93)}
    image = Image.new('RGBA', (128, 128))
    for tile in range(64):
        base = palette.get(tile, palette[32])
        for y in range(16):
            for x in range(16):
                # Low-contrast pixel grain, deliberately without tile outlines
                # so the long merged panels read as one continuous housing.
                grain = ((x*13 + y*7 + x*y*3 + tile*11) % 7)-3
                weave = 1 if (x//2 + y//2) % 2 else -1
                image.putpixel(((tile % 8)*16+x, (tile // 8)*16+y),
                               (*[channel+grain+weave for channel in base], 255))
    with Image.open(ASSETS/'textures/block/laser_pixel_atlas.png') as laser:
        # The merged glazing samples native translucent tile 4 from this atlas too.
        image.paste(laser.crop((64, 0, 80, 16)), (64, 0))
        # The receiver intake and internal manifold retain the shared pink light.
        image.paste(laser.crop((16, 80, 32, 96)), (16, 80))
    # Static white tile 42 gives the beacon shader an unmodified RGBA sample;
    # its vertex colour alone sets the optical tube's pink tint and translucency.
    image.paste(Image.new('RGBA', (16, 16), (255, 255, 255, 255)), (32, 80))
    image.save(ASSETS/'textures/block/workstation_shell_atlas.png')
    write(ASSETS/'models/block/workstation_shell_atlas.json', {
        'parent': 'minecraft:block/cube_all', 'ambientocclusion': False,
        'textures': {'all': 'overload_sim:block/workstation_shell_atlas'}})


def main():
    wrench_texture()
    shell_atlas()
    models = {CONTROLLER: controller_model()}
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
            'MACHINE_INPUT': {'left': 33, 'top': 75, 'grid': 'BREAK_AFTER_3COLS'},
            'MACHINE_OUTPUT': {'left': 115, 'top': 75, 'grid': 'BREAK_AFTER_3COLS'},
            'PLAYER_INVENTORY': {'left': 20, 'top': 160, 'grid': 'BREAK_AFTER_9COLS'},
            'PLAYER_HOTBAR': {'left': 20, 'top': 218, 'grid': 'BREAK_AFTER_9COLS'},
        }})
    print('Exported compact yellow wrench, independent grey workstation shell atlas, legacy recovery assets and tall-fluid layout.')


if __name__ == '__main__':
    main()
