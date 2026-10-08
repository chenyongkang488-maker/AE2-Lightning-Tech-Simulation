"""Write JSON data and translations; GuideME pages are hand maintained."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NAMES={
 'simulation_controller':('模拟室控制器','Simulation Chamber Controller'),
 'simulation_frame':('模拟室框架','Simulation Chamber Frame'),
 'simulation_efficiency_t1':('模拟效率模块 T1','Simulation Efficiency Module T1'),
 'simulation_efficiency_t2':('模拟效率模块 T2','Simulation Efficiency Module T2'),
 'simulation_efficiency_t3':('模拟效率模块 T3','Simulation Efficiency Module T3'),
 'simulation_fortune_module':('模拟时运模块','Simulation Fortune Module'),
 'simulation_overload_module':('模拟过载模块','Simulation Overload Module'),
 'simulation_smelting_module':('模拟熔炼模块','Simulation Smelting Module')}
def write(path,value):
 path=RES/path;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
for block in NAMES:
 write(Path('data/overload_sim/loot_table/blocks')/(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'overload_sim:'+block}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/overload_sim/loot_table/blocks/formed_simulation_glass.json'),{'type':'minecraft:block','pools':[]})
write(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['overload_sim:overload_simulation_chamber']+['overload_sim:'+id for id in NAMES]})
write(Path('data/overload_sim/tags/block/simulation_frames.json'),{'replace':False,'values':['overload_sim:simulation_frame']})
write(Path('data/overload_sim/tags/block/simulation_glass.json'),{'replace':False,'values':['ae2:quartz_vibrant_glass']})
write(Path('data/overload_sim/multiblock_simulation/default.json'),{'policy':{'ticks':180,'reduction_cap':104,'fortune_cap':10,'t1':2,'t2':4,'t3':8,'fe':1000,'hv':1,'overload_ehv':1,'smelting_hv':2},'smelting':[{'input':'minecraft:raw_'+s,'result':'minecraft:'+s+'_ingot','count':2} for s in ('iron','gold','copper')],'modules':[]})
from manufacturing_recipes import write_recipes
write_recipes()
for locale,index in [('zh_cn',0),('en_us',1)]:
 path=RES/'assets/overload_sim/lang'/f'{locale}.json';lang=json.loads(path.read_text(encoding='utf8'))
 lang.update({'block.overload_sim.'+id:label[index] for id,label in NAMES.items()})
 lang.update({
 'gui.overload_sim.multiblock.crystals':['水晶：%s / %s','Crystals: %s / %s'][index],
 'gui.overload_sim.multiblock.outputs':['产物（第 %s / 4 页）','Outputs (page %s / 4)'][index],
 'gui.overload_sim.multiblock.take':['左键取一组，右键取一个，Shift取入背包','Left: one stack. Right: one item. Shift: fill inventory.'][index],
 'status.overload_sim.8':['结构所在区块未加载','Structure chunks unloaded'][index],
 'gui.overload_sim.multiblock.frequency':['过载频道：%s','Overload frequency: %s'][index],
 'gui.overload_sim.multiblock.error':['结构诊断：%s','Structure diagnostic: %s'][index]})
 path.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
# GuideME pages are hand maintained; resource generation must not overwrite them.
print('Multiblock JSON, recipes and languages written; GuideME pages left unchanged.')
