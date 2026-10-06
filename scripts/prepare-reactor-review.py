"""Write cosmetic-review commands into the existing isolated developer world."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORLD = ROOT / 'run-client/saves/laser-review'
assert (WORLD / 'level.dat').exists(), 'Use the existing isolated review world'
FOLDER = WORLD / 'datapacks/visual-review/data/overload_sim_review/function'
commands = {
    'reactor_site': 'gamemode creative @s\ntp @s 5100 300 5100 0 0\ntime set day\nweather clear\n',
    'reactor_build': 'gamemode spectator @s\ntp @s 5100 300 5100 0 0\ngamemode creative @s\noverload_sim laser build emitter\n',
    'reactor_front': 'data merge block 5100 304 5108 {LaserMachine:{Enabled:0b}}\ngamemode spectator @s\ntp @s 5100.5 302.9 5119.5 180 0\n',
    'reactor_side': 'gamemode spectator @s\ntp @s 5107.5 305.5 5119 140 17\n',
    'reactor_back': 'gamemode spectator @s\ntp @s 5100.5 302.9 5102.5 0 0\n',
    'reactor_active': 'data merge block 5100 304 5108 {LaserMachine:{Enabled:1b,Energy:100000000L}}\ngamemode spectator @s\ntp @s 5107.5 305.5 5119 140 17\n',
    'reactor_unformed': 'setblock 5102 304 5110 minecraft:air\n',
    'reactor_reformed': 'setblock 5102 304 5110 overload_sim:laser_frame\n',
    'reactor_interface': 'setblock 5102 304 5110 ae2lt:overloaded_interface\n',
}
for name, source in commands.items():
    (FOLDER / (name+'.mcfunction')).write_bytes(source.encode('utf-8'))
print('Prepared pixel reactor review functions; /reload, /function overload_sim_review:reactor_site, then /overload_sim laser build emitter.')
