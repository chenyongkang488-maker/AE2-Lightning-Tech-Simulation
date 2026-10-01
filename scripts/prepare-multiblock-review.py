"""Create a native powered production scene only in the ignored developer world."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'run-client/saves/ui-review/datapacks/visual-review/data/overload_sim_review/function/multiblock.mcfunction'
def nbt(value):
    if isinstance(value,dict):return '{'+','.join(json.dumps(k)+':'+nbt(v) for k,v in value.items())+'}'
    if isinstance(value,list):return '['+','.join(nbt(v) for v in value)+']'
    return json.dumps(value)
commands=['time set day','weather clear','gamemode creative @s','fill 997 179 1005 1008 179 1018 minecraft:smooth_quartz','fill 998 180 1006 1007 187 1017 minecraft:air']
for x in range(5):
    for y in range(5):
        for z in range(5):
            bounds=(x in (0,4))+(y in (0,4))+(z in (0,4))
            block='overload_sim:simulation_frame' if bounds>=2 or y==0 else 'ae2:quartz_vibrant_glass' if bounds==1 else 'minecraft:air'
            commands.append(f'setblock {1000+x} {180+y} {1010+z} {block}')
for i,(x,z) in enumerate((x,z) for x in range(1,4) for z in range(1,4)):
    block='simulation_efficiency_t3' if i<7 else 'simulation_overload_module' if i==7 else 'simulation_smelting_module'
    commands.append(f'setblock {1000+x} 180 {1010+z} overload_sim:{block}')
commands+=['setblock 1001 180 1010 overload_sim:simulation_controller[facing=north]','setblock 1001 180 1009 ae2:creative_energy_cell','setblock 1001 180 1008 ae2:drive']
cells={'item'+str(i):{'id':'ae2lt:mysterious_cell','count':1,'components':{'minecraft:custom_data':{'CellType':i+1}}} for i in range(2)}
cells['item2']={'id':'ae2lt:infinite_storage_cell','count':1}
commands.append('data merge block 1001 180 1008 '+nbt({'inv':cells}).replace('"CellType":1','"CellType":1b').replace('"CellType":2','"CellType":2b'))
crystal={'id':'overload_sim:perfect_simulation_crystal','count':1,'components':{'overload_sim:crystal_data':{'profile':'overload_sim:iron'}}}
inventory={'Size':49,'Items':[{'Slot':i,**crystal} for i in range(25)]}
entries=[{'Slot':i,'Quantity':1024,'Item':{'id':'minecraft:'+item,'count':1}} for i,item in [(0,'iron_ingot'),(1,'gold_ingot'),(2,'copper_ingot'),(3,'diamond'),(32,'redstone'),(64,'emerald'),(96,'quartz')]]
commands.append('data merge block 1001 180 1010 '+nbt({'SimulationMachine':{'Crystals':inventory,'Energy':2000000,'Bulk':{'Entries':entries}}}))
commands+=['give @s overload_sim:simulation_controller','give @s overload_sim:simulation_frame 64','give @s overload_sim:simulation_efficiency_t3','give @s overload_sim:simulation_overload_module','give @s overload_sim:simulation_smelting_module','tp @s 1007 182 1006 36 8']
path.parent.mkdir(parents=True,exist_ok=True);path.write_text('\n'.join(commands)+'\n',encoding='utf8')
print('Prepared ignored native production scene:',path)
