"""Write reproducible text fixtures into the ignored native review world (Mek required)."""
from pathlib import Path
import json, subprocess, sys

root = Path(__file__).resolve().parents[1]
subprocess.run([sys.executable, '-X', 'utf8', str(root/'scripts/prepare-multiblock-review.py'), '5', 'combo'], check=True)
folder = root/'run-client/saves/ui-review/datapacks/visual-review/data/overload_sim_review/function'
def nbt(value):
    if isinstance(value, dict): return '{'+','.join(json.dumps(k)+':'+nbt(v) for k,v in value.items())+'}'
    if isinstance(value, list): return '['+','.join(nbt(v) for v in value)+']'
    return json.dumps(value)
def crystal(profile, entity=None):
    data={'profile':profile}
    if entity: data['entity_type']=entity
    return {'id':'overload_sim:perfect_simulation_crystal','count':1,'components':{'overload_sim:crystal_data':data}}

inputs=[crystal('overload_sim:mob', 'minecraft:'+entity) for entity in ['phantom','wither','ender_dragon','warden']]
inputs += [crystal('overload_sim:'+material) for material in ['netherite','redstone','lapis']]
inputs += [crystal('overload_sim:auto/mineral/c/osmium')]
machine={'SimulationMachine':{'Crystals':{'Size':49,'Items':[{'Slot':i,**stack} for i,stack in enumerate(inputs)]},'Energy':2000000,'Bulk':{'Entries':[]}}}
commands=['function overload_sim_review:multiblock_5_combo',
 'setblock 1003 180 1010 overload_sim:simulation_frame',
 'setblock 1003 180 1012 overload_sim:simulation_efficiency_t3',
 'data merge block 1001 180 1010 '+nbt(machine),
 'clear @s',
 'give @s overload_sim:perfect_simulation_crystal[overload_sim:crystal_data={profile:"overload_sim:mob",entity_type:"minecraft:phantom"}]',
 'tp @s 1002.5 182.5 1005 0 8']
(folder/'compatibility.mcfunction').write_text('\n'.join(commands)+'\n',encoding='utf8')
(folder/'compatibility_inspect.mcfunction').write_text('overload_sim audit\ndata get block 1001 180 1010 SimulationMachine.Bulk\ntp @s 1001.5 180 1008 0 29\n',encoding='utf8')

p=(1020,180,1010)
commands=['fill 1017 179 1007 1023 179 1013 minecraft:smooth_quartz','fill 1017 180 1007 1023 185 1013 minecraft:air']
for x in range(-2,3):
    for z in range(-2,3):
        if x or z: commands.append(f'setblock {p[0]+x} {p[1]} {p[2]+z} mekanism:block_raw_osmium')
commands += ['setblock 1020 180 1010 ae2lt:lightning_collector','setblock 1020 181 1010 minecraft:lightning_rod','setblock 1020 179 1010 ae2:creative_energy_cell','setblock 1020 178 1010 ae2:drive',
 'data merge block 1020 178 1010 {inv:{item0:{id:"ae2lt:infinite_storage_cell",count:1}}}',
 'data merge block 1020 180 1010 {Inventory:[{Slot:0,CountInt:1,Stack:{id:"overload_sim:blank_simulation_crystal",count:1}}],CooldownTicks:0}',
 'tp @s 1020.5 182 1006 0 35']
(folder/'osmium_collector.mcfunction').write_text('\n'.join(commands)+'\n',encoding='utf8')
print('Native compatibility + automatic Osmium collector fixtures:',folder)
