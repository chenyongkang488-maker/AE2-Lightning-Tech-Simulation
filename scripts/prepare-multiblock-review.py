"""Create a native powered production scene only in the ignored developer world."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1]
N=int(sys.argv[1]) if len(sys.argv)>1 else 5
assert 3<=N<=7
variant=sys.argv[2] if len(sys.argv)>2 else 'combo'
suffix='' if len(sys.argv)==1 else '_'+str(N)+'_'+variant
path=ROOT/f'run-client/saves/ui-review/datapacks/visual-review/data/overload_sim_review/function/multiblock{suffix}.mcfunction'
def nbt(value):
    if isinstance(value,dict):return '{'+','.join(json.dumps(k)+':'+nbt(v) for k,v in value.items())+'}'
    if isinstance(value,list):return '['+','.join(nbt(v) for v in value)+']'
    return json.dumps(value)
commands=['time set day','weather clear','gamemode creative @s','fill 997 179 1005 1012 179 1020 minecraft:smooth_quartz','fill 998 180 1006 1011 189 1019 minecraft:air']
for x in range(N):
    for y in range(N):
        for z in range(N):
            bounds=(x in (0,N-1))+(y in (0,N-1))+(z in (0,N-1))
            block='overload_sim:simulation_frame' if bounds>=2 or y in (0,N-1) else 'ae2:quartz_vibrant_glass' if bounds==1 else 'minecraft:air'
            commands.append(f'setblock {1000+x} {180+y} {1010+z} {block}')
for i,(x,z) in enumerate((x,z) for x in range(1,N-1) for z in range(1,N-1)):
    last=(N-2)**2-1
    block='simulation_smelting_module' if i==last and variant in ('combo','orange') else 'simulation_overload_module' if i==last-1 and variant=='combo' else 'simulation_efficiency_t3'
    commands.append(f'setblock {1000+x} 180 {1010+z} overload_sim:{block}')
commands+=['setblock 1001 180 1010 overload_sim:simulation_controller[facing=north]','setblock 1001 179 1010 ae2:creative_energy_cell','setblock 1001 178 1010 ae2:drive']
if N>=4:commands.append(f'setblock {1000+N-2} 180 1010 ae2lt:overloaded_interface')
cells={'item'+str(i):{'id':'ae2lt:mysterious_cell','count':1,'components':{'minecraft:custom_data':{'CellType':i+1}}} for i in range(2)}
cells['item2']={'id':'ae2lt:infinite_storage_cell','count':1}
commands.append('data merge block 1001 178 1010 '+nbt({'inv':cells}).replace('"CellType":1','"CellType":1b').replace('"CellType":2','"CellType":2b'))
crystal={'id':'overload_sim:perfect_simulation_crystal','count':1,'components':{'overload_sim:crystal_data':{'profile':'overload_sim:iron'}}}
inventory={'Size':49,'Items':[{'Slot':i,**crystal} for i in range(49 if variant=='recovery' else N*N)]}
entries=[{'Slot':i,'Quantity':1024,'Item':{'id':'minecraft:'+item,'count':1}} for i,item in [(0,'iron_ingot'),(1,'gold_ingot'),(2,'copper_ingot'),(3,'diamond'),(32,'redstone'),(64,'emerald'),(96,'quartz')]]
commands.append('data merge block 1001 180 1010 '+nbt({'SimulationMachine':{'Crystals':inventory,'Energy':2000000,'Bulk':{'Entries':entries}}}))
if variant=='recovery':commands+=['setblock 1001 179 1010 minecraft:smooth_quartz','setblock 1001 178 1010 minecraft:air','data merge block 1001 180 1010 {SimulationMachine:{Energy:0}}']
commands+=['clear @s','give @s overload_sim:simulation_controller','give @s overload_sim:simulation_frame 64','give @s overload_sim:simulation_efficiency_t3','give @s overload_sim:simulation_overload_module','give @s overload_sim:simulation_smelting_module',f'tp @s 996 {180+N/2} 1005 -40 5']
path.parent.mkdir(parents=True,exist_ok=True);path.write_text('\n'.join(commands)+'\n',encoding='utf8')
print('Prepared ignored native production scene:',path)
if N==7 and variant=='pink':
    stress=[]
    for offset in (20,30,40,50):
        for line in commands:
            if line.startswith(('give ','clear ','tp ')):continue
            tokens=line.split(' ')
            pairs=[(1,3),(4,6)] if tokens[0]=='fill' else [(1,3)] if tokens[0]=='setblock' else [(3,5)] if line.startswith('data merge block ') else []
            for xi,zi in pairs:
                tokens[xi]=str(float(tokens[xi])+offset).removesuffix('.0')
                tokens[zi]=str(float(tokens[zi])+offset).removesuffix('.0')
            stress.append(' '.join(tokens))
    stress+=['gamemode spectator @s','tp @s 1038 185 1010 0 8']
    (path.parent/'stress.mcfunction').write_text('\n'.join(stress)+'\n',encoding='utf8')
