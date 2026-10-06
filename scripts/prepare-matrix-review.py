"""Prepare reproducible visual commands in the existing isolated windowed review world."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/overload_sim'
for locale,text in [('zh_cn','当前激光强度：%s FE／次'),('en_us','Current laser: %s FE/pulse')]:
    p=assets/f'lang/{locale}.json';data=json.loads(p.read_text(encoding='utf-8'))
    data['gui.overload_sim.laser.current_strength']=text
    data['gui.overload_sim.laser.exact_strength']='当前激光强度：%s FE／次' if locale=='zh_cn' else 'Current laser strength: %s FE/pulse'
    data['subtitles.overload_sim.collapse_transition']='亚空间收缩并展开' if locale=='zh_cn' else 'Subspace contracts and expands'
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
model=json.loads((assets/'models/block/simulation_energy_orb.json').read_text())
model['textures']['0']='overload_sim:block/collapse_energy_orb'
(assets/'models/block/collapse_energy_orb.json').write_text(json.dumps(model,indent=2)+'\n',newline='\n')
(assets/'textures/block/collapse_energy_orb.png.mcmeta').write_text(json.dumps({'animation':{'width':32,'height':32,'frametime':2,'interpolate':False}},indent=2)+'\n',newline='\n')
(assets/'sounds.json').write_text(json.dumps({'collapse_transition':{'subtitle':'subtitles.overload_sim.collapse_transition','sounds':[{'name':'overload_sim:collapse_transition','attenuation_distance':24}]}},indent=2)+'\n',newline='\n')
folder=root/'run-client/saves/laser-review/datapacks/visual-review/data/overload_sim_review/function'
assert (root/'run-client/saves/laser-review/level.dat').exists()
commands={
    'matrix_site':'gamemode creative @s\ntp @s 5300 300 5300 0 0\ntime set day\nweather clear\n',
    'matrix_build':'overload_sim laser demo\n',
    'matrix_view':'gamemode spectator @s\ntp @s 5317 307 5318 105 15\n',
    'constraint_view':'gamemode spectator @s\ntp @s 5308.5 304 5327.5 138 7\n',
    'focus_view':'gamemode spectator @s\ntp @s 5308.5 304 5373.5 138 7\n',
    'core_view':'gamemode spectator @s\ntp @s 5306.5 304.5 5374.5 116 13\n',
    'core_stop':'data merge block 5300 304 5371 {LaserMachine:{Enabled:0b}}\n',
    'core_start':'data merge block 5300 304 5371 {LaserMachine:{Enabled:1b}}\n',
    'constraint_ui':'gamemode creative @s\ntp @s 5300.5 300 5316 0 3\n',
    'focus_ui':'gamemode creative @s\ntp @s 5300.5 300 5362 0 3\n',
}
positions=[(304,5308),(301,5314)];along=6;remaining=4
for i in range(16):
    if remaining<=2:
        along+=2;positions.append((301,5308+along));remaining=6
    along+=2;remaining-=2;positions.append((301,5308+along))
positions.append((301,5308+along+2))
commands['matrix_refill']=''.join(f'data merge block 5300 {y} {z} {{LaserMachine:{{Energy:1000000000L,ExtremeLightning:640000L}}}}\n' for y,z in positions)
commands['matrix_charge_loop']='function overload_sim_review:matrix_refill\nschedule function overload_sim_review:matrix_charge_loop 30s replace\n'
for name,text in commands.items():(folder/f'{name}.mcfunction').write_text(text,encoding='utf-8')
print('Prepared model, translations, sound registration and developer-world review commands')
