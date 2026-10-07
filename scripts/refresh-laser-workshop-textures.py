"""Refresh embedded references after native Blockbench painting; does not edit images."""
from pathlib import Path
import base64
import json

root = Path(__file__).resolve().parents[1]
atlas = root / 'src/main/resources/assets/overload_sim/textures/block/laser_pixel_atlas.png'
source = 'data:image/png;base64,' + base64.b64encode(atlas.read_bytes()).decode()
for path in (root / 'art/blockbench').glob('*.bbmodel'):
    original = path.read_text(encoding='utf-8')
    project = json.loads(original)
    changed = False
    for texture in project.get('textures', []):
        if texture.get('name') == 'laser_pixel_atlas.png' and texture.get('source') != source:
            texture['source'] = source
            changed = True
    if changed:
        compact = original.count('\n') <= 2
        options = {'separators': (',', ':')} if compact else {'indent': 2}
        path.write_text(json.dumps(project, ensure_ascii=False, **options) + '\n', encoding='utf-8', newline='\n')
        print('Updated embedded atlas reference:', path.name)
