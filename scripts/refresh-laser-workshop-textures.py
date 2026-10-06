"""Refresh embedded references after native Blockbench painting; does not edit images."""
from pathlib import Path
import base64
import json

root = Path(__file__).resolve().parents[1]
atlas = root / 'src/main/resources/assets/overload_sim/textures/block/laser_pixel_atlas.png'
source = 'data:image/png;base64,' + base64.b64encode(atlas.read_bytes()).decode()
for path in (root / 'art/blockbench').glob('*.bbmodel'):
    project = json.loads(path.read_text(encoding='utf-8'))
    changed = False
    for texture in project.get('textures', []):
        if texture.get('name') == 'laser_pixel_atlas.png' and texture.get('source') != source:
            texture['source'] = source
            changed = True
    if changed:
        path.write_text(json.dumps(project, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
        print('Updated embedded atlas reference:', path.name)
